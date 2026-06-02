package com.nav.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.TestcontainersConfiguration;
import com.nav.support.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理后台集成测试。
 * 经 Testcontainers 起真实 PG+Redis,验证越权拦截、开户、重置密码、邀请码注册全流程。
 * 初始管理员由 AdminInitializer 据 @TestPropertySource 的凭据创建。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=admin",
        "app.admin.password=adminpass123"
})
class AdminIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 普通用户访问管理接口返回 403。
     */
    @Test
    void normalUserCannotAccessAdmin() {
        // 1. 管理员登录并开一个普通用户
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"normaluser\",\"password\":\"userpass123\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. 普通用户登录
        ApiClient user = loginAs("normaluser", "userpass123");

        // 3. 普通用户访问管理接口被拒 403
        ResponseEntity<String> denied = user.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"someone\",\"password\":\"password123\"}");
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 邀请码签发 -> 注册 -> 登录,且同一邀请码不可重复使用。
     */
    @Test
    void inviteCodeRegisterFlow() throws Exception {
        // 1. 管理员签发邀请码
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> invite = admin.exchange(HttpMethod.POST, "/api/admin/invite-codes", "{}");
        assertThat(invite.getStatusCode()).isEqualTo(HttpStatus.OK);
        String code = objectMapper.readTree(invite.getBody()).path("code").asText();
        assertThat(code).isNotBlank();

        // 2. 用邀请码注册
        ApiClient guest = new ApiClient(rest);
        guest.exchange(HttpMethod.GET, "/api/auth/me", null);
        ResponseEntity<String> register = guest.exchange(HttpMethod.POST, "/api/auth/register",
                "{\"code\":\"" + code + "\",\"username\":\"invited\",\"password\":\"invitepass123\"}");
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. 注册用户能登录
        ApiClient invited = loginAs("invited", "invitepass123");
        ResponseEntity<String> me = invited.exchange(HttpMethod.GET, "/api/auth/me", null);
        assertThat(me.getBody()).contains("\"username\":\"invited\"");

        // 4. 同一邀请码再次注册被拒(400)
        ApiClient guest2 = new ApiClient(rest);
        guest2.exchange(HttpMethod.GET, "/api/auth/me", null);
        ResponseEntity<String> reuse = guest2.exchange(HttpMethod.POST, "/api/auth/register",
                "{\"code\":\"" + code + "\",\"username\":\"invited2\",\"password\":\"invitepass123\"}");
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * 管理员重置密码后,旧密码失效、新密码可登录。
     */
    @Test
    void resetPasswordThenLoginWithNewPassword() throws Exception {
        // 1. 管理员开户
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"resetme\",\"password\":\"oldpass123\"}");
        String userId = objectMapper.readTree(created.getBody()).path("id").asText();

        // 2. 管理员重置该用户密码
        ResponseEntity<String> reset = admin.exchange(HttpMethod.POST,
                "/api/admin/users/" + userId + "/reset-password", "{\"newPassword\":\"newpass123\"}");
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 3. 旧密码登录失败 401
        ApiClient oldTry = new ApiClient(rest);
        oldTry.exchange(HttpMethod.GET, "/api/auth/me", null);
        ResponseEntity<String> oldLogin = oldTry.exchange(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"resetme\",\"password\":\"oldpass123\"}");
        assertThat(oldLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. 新密码登录成功
        ApiClient newTry = loginAs("resetme", "newpass123");
        ResponseEntity<String> me = newTry.exchange(HttpMethod.GET, "/api/auth/me", null);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 管理员能列出全部用户(字段含 status / createdAt);普通用户访问被拒 403。
     */
    @Test
    void listUsersForAdminOnly() {
        // 1. 管理员开户
        ApiClient admin = loginAs("admin", "adminpass123");
        admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"listed\",\"password\":\"listed123\"}");

        // 2. 管理员列用户:含初始 admin 与新建用户,且字段齐全
        ResponseEntity<String> list = admin.exchange(HttpMethod.GET, "/api/admin/users", null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody())
                .contains("\"username\":\"admin\"")
                .contains("\"username\":\"listed\"")
                .contains("\"role\":")
                .contains("\"status\":")
                .contains("\"createdAt\":");

        // 3. 普通用户访问被拒 403
        ApiClient user = loginAs("listed", "listed123");
        ResponseEntity<String> denied = user.exchange(HttpMethod.GET, "/api/admin/users", null);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 拿 CSRF 后登录,返回带会话的客户端。
     */
    private ApiClient loginAs(String username, String password) {
        ApiClient client = new ApiClient(rest);
        client.exchange(HttpMethod.GET, "/api/auth/me", null);
        ResponseEntity<String> login = client.exchange(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return client;
    }
}
