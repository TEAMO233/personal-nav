package com.nav.auth;

import com.nav.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证接口集成测试。
 * 经 Testcontainers 起真实 PG+Redis,验证 CSRF 下发、登录建会话、me 鉴权、错误密码、缺失 CSRF、会话固定防护。
 * 初始管理员由 AdminInitializer 据 @TestPropertySource 的凭据创建。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=root",
        "app.admin.password=rootpass123"
})
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    /**
     * 未登录访问 me 返回 401,且响应下发 XSRF-TOKEN Cookie。
     */
    @Test
    void unauthenticatedMeReturns401AndSetsCsrfCookie() {
        // 1. 未带任何凭据访问 me
        ResponseEntity<String> resp = rest.getForEntity("/api/auth/me", String.class);
        // 2. 返回 401
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // 3. 响应下发了 XSRF-TOKEN Cookie
        assertThat(extractCookie(resp, "XSRF-TOKEN")).isNotNull();
    }

    /**
     * 完整登录流程:拿 CSRF token -> 登录拿会话 -> 带会话访问 me 返回管理员信息。
     */
    @Test
    void loginThenAccessMe() {
        // 1. 先 GET 拿 XSRF-TOKEN
        ResponseEntity<String> bootstrap = rest.getForEntity("/api/auth/me", String.class);
        String xsrf = extractCookie(bootstrap, "XSRF-TOKEN");

        // 2. 带 CSRF 登录
        ResponseEntity<String> loginResp = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"root\",\"password\":\"rootpass123\"}",
                        jsonHeaders(xsrf, null)), String.class);
        // 3. 登录成功并下发会话 Cookie
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).contains("\"role\":\"ADMIN\"").contains("\"username\":\"root\"");
        String session = extractCookie(loginResp, "NAV_SESSION");
        assertThat(session).isNotNull();

        // 4. 带会话 Cookie 访问 me
        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.add(HttpHeaders.COOKIE, "NAV_SESSION=" + session);
        ResponseEntity<String> meResp = rest.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(meHeaders), String.class);
        // 5. 返回当前用户为 root/ADMIN
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody()).contains("\"username\":\"root\"").contains("\"role\":\"ADMIN\"");
    }

    /**
     * 错误密码登录返回 401。
     */
    @Test
    void wrongPasswordReturns401() {
        // 1. 拿 CSRF token
        String xsrf = extractCookie(rest.getForEntity("/api/auth/me", String.class), "XSRF-TOKEN");
        // 2. 用错误密码登录
        ResponseEntity<String> resp = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"root\",\"password\":\"wrong\"}",
                        jsonHeaders(xsrf, null)), String.class);
        // 3. 返回 401
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 缺少 CSRF token 的写请求被拒(403)。
     */
    @Test
    void loginWithoutCsrfReturns403() {
        // 1. 不带 X-XSRF-TOKEN 的登录请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"root\",\"password\":\"rootpass123\"}", headers), String.class);
        // 2. CSRF 校验失败返回 403
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 会话固定防护:带着已有会话再次登录,sessionId 应被换发(前后不同)。
     */
    @Test
    void sessionIdChangesAfterLogin() {
        // 1. 拿 CSRF token
        String xsrf = extractCookie(rest.getForEntity("/api/auth/me", String.class), "XSRF-TOKEN");
        // 2. 第一次登录拿到会话 A
        ResponseEntity<String> login1 = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"root\",\"password\":\"rootpass123\"}",
                        jsonHeaders(xsrf, null)), String.class);
        String sessionA = extractCookie(login1, "NAV_SESSION");
        assertThat(sessionA).isNotNull();
        // 3. 带会话 A 再次登录,应换发新会话 B
        ResponseEntity<String> login2 = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"root\",\"password\":\"rootpass123\"}",
                        jsonHeaders(xsrf, sessionA)), String.class);
        String sessionB = extractCookie(login2, "NAV_SESSION");
        // 4. 两次会话 id 不同,证明登录后换发了 session id(防会话固定)
        assertThat(sessionB).isNotNull().isNotEqualTo(sessionA);
    }

    /**
     * 组装带 CSRF token 的 JSON 请求头,可选附带会话 Cookie。
     */
    private HttpHeaders jsonHeaders(String xsrf, String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        StringBuilder cookie = new StringBuilder("XSRF-TOKEN=").append(xsrf);
        if (sessionCookie != null) {
            cookie.append("; NAV_SESSION=").append(sessionCookie);
        }
        headers.add(HttpHeaders.COOKIE, cookie.toString());
        headers.add("X-XSRF-TOKEN", xsrf);
        return headers;
    }

    /**
     * 从响应的 Set-Cookie 中取指定名称的 Cookie 值。
     */
    private String extractCookie(ResponseEntity<?> response, String name) {
        // 1. 取 Set-Cookie 头
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return null;
        }
        // 2. 匹配指定名称,截取到分号前的值
        String prefix = name + "=";
        for (String cookie : setCookies) {
            if (cookie.startsWith(prefix)) {
                String value = cookie.substring(prefix.length());
                int semicolon = value.indexOf(';');
                return semicolon >= 0 ? value.substring(0, semicolon) : value;
            }
        }
        return null;
    }
}
