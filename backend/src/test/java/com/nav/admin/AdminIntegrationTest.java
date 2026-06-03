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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
     * 并发用同一邀请码注册:只有一个成功,另一个被拒(防 TOCTOU 重复消费)。
     */
    @Test
    void concurrentRegisterSameCodeOnlyOneSucceeds() throws Exception {
        // 1. 管理员签发一个邀请码
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> invite = admin.exchange(HttpMethod.POST, "/api/admin/invite-codes", "{}");
        String code = objectMapper.readTree(invite.getBody()).path("code").asText();

        // 2. 两个访客各自先拿 CSRF
        ApiClient g1 = new ApiClient(rest);
        g1.exchange(HttpMethod.GET, "/api/auth/me", null);
        ApiClient g2 = new ApiClient(rest);
        g2.exchange(HttpMethod.GET, "/api/auth/me", null);

        // 3. 两线程等统一信号后同时用同一码注册不同用户名
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger badCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(registerTask(g1, code, "race_a", start, okCount, badCount));
        pool.submit(registerTask(g2, code, "race_b", start, okCount, badCount));
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 4. 恰好一个成功、一个被拒
        assertThat(okCount.get()).isEqualTo(1);
        assertThat(badCount.get()).isEqualTo(1);
    }

    /**
     * 禁用用户:其已登录会话立即失效(被踢下线),且无法再登录。
     */
    @Test
    void disableUserKicksOutSessionAndBlocksLogin() throws Exception {
        // 1. 管理员开户
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"disableme\",\"password\":\"disablepass123\"}");
        String userId = objectMapper.readTree(created.getBody()).path("id").asText();

        // 2. 该用户登录,带会话访问 me 正常
        ApiClient victim = loginAs("disableme", "disablepass123");
        assertThat(victim.exchange(HttpMethod.GET, "/api/auth/me", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. 管理员禁用该用户
        ResponseEntity<String> disabled = admin.exchange(HttpMethod.POST,
                "/api/admin/users/" + userId + "/status", "{\"status\":\"DISABLED\"}");
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. 原会话立即失效:再访问 me 返回 401
        assertThat(victim.exchange(HttpMethod.GET, "/api/auth/me", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // 5. 被禁用账户无法重新登录(DISABLED 拒登)
        ApiClient retry = new ApiClient(rest);
        retry.exchange(HttpMethod.GET, "/api/auth/me", null);
        ResponseEntity<String> relogin = retry.exchange(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"disableme\",\"password\":\"disablepass123\"}");
        assertThat(relogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 启用此前被禁用的用户后,其可重新登录。
     */
    @Test
    void enableUserRestoresLogin() throws Exception {
        // 1. 管理员开户后禁用
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"toggleme\",\"password\":\"togglepass123\"}");
        String userId = objectMapper.readTree(created.getBody()).path("id").asText();
        admin.exchange(HttpMethod.POST, "/api/admin/users/" + userId + "/status", "{\"status\":\"DISABLED\"}");

        // 2. 禁用态登录失败
        ApiClient t1 = new ApiClient(rest);
        t1.exchange(HttpMethod.GET, "/api/auth/me", null);
        assertThat(t1.exchange(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"toggleme\",\"password\":\"togglepass123\"}").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // 3. 管理员重新启用
        ResponseEntity<String> enabled = admin.exchange(HttpMethod.POST,
                "/api/admin/users/" + userId + "/status", "{\"status\":\"ACTIVE\"}");
        assertThat(enabled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. 启用后可正常登录
        ApiClient t2 = loginAs("toggleme", "togglepass123");
        assertThat(t2.exchange(HttpMethod.GET, "/api/auth/me", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 重置密码后,该用户此前已登录的会话立即失效。
     */
    @Test
    void resetPasswordInvalidatesActiveSessions() throws Exception {
        // 1. 管理员开户,该用户登录拿会话
        ApiClient admin = loginAs("admin", "adminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"resetkick\",\"password\":\"oldpass123\"}");
        String userId = objectMapper.readTree(created.getBody()).path("id").asText();
        ApiClient victim = loginAs("resetkick", "oldpass123");
        assertThat(victim.exchange(HttpMethod.GET, "/api/auth/me", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. 管理员重置其密码
        admin.exchange(HttpMethod.POST, "/api/admin/users/" + userId + "/reset-password",
                "{\"newPassword\":\"newpass123\"}");

        // 3. 旧会话立即失效:再访问 me 返回 401
        assertThat(victim.exchange(HttpMethod.GET, "/api/auth/me", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 管理员不能禁用自己(避免把自己锁在系统外)。
     */
    @Test
    void adminCannotDisableSelf() throws Exception {
        // 1. 管理员登录并取自己的 id
        ApiClient admin = loginAs("admin", "adminpass123");
        String selfId = objectMapper.readTree(
                admin.exchange(HttpMethod.GET, "/api/auth/me", null).getBody()).path("id").asText();

        // 2. 禁用自己被拒 400
        ResponseEntity<String> resp = admin.exchange(HttpMethod.POST,
                "/api/admin/users/" + selfId + "/status", "{\"status\":\"DISABLED\"}");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(resp.getBody()).path("code").asText()).isEqualTo("CANNOT_DISABLE_SELF");
    }

    /**
     * 造一个「等统一信号后用指定邀请码注册」的任务,按响应状态分类计数。
     */
    private Runnable registerTask(ApiClient client, String code, String username,
                                  CountDownLatch start, AtomicInteger okCount, AtomicInteger badCount) {
        return () -> {
            try {
                // 1. 等主线程统一放行,尽量让两次注册同时打到后端
                start.await();
                ResponseEntity<String> resp = client.exchange(HttpMethod.POST, "/api/auth/register",
                        "{\"code\":\"" + code + "\",\"username\":\"" + username + "\",\"password\":\"racepass123\"}");
                // 2. 按状态分类:200 成功,400 被拒
                if (resp.getStatusCode() == HttpStatus.OK) {
                    okCount.incrementAndGet();
                } else if (resp.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    badCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
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
