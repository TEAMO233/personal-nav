package com.nav.auth;

import com.nav.TestcontainersConfiguration;
import com.nav.support.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录限流集成测试:同一 IP+用户名 连续登录超过阈值后返回 429。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RateLimitTest {

    @Autowired
    private TestRestTemplate rest;

    /**
     * 同一用户名连续登录:前若干次因密码错返回 401,超过阈值后被限流 429。
     */
    @Test
    void loginRateLimitReturns429AfterThreshold() {
        // 1. 先拿 CSRF token
        ApiClient client = new ApiClient(rest);
        client.exchange(HttpMethod.GET, "/api/auth/me", null);

        // 2. 连续 12 次错误登录(阈值为 10 次/5 分钟)
        boolean got429 = false;
        int lastStatus = 0;
        for (int i = 0; i < 12; i++) {
            ResponseEntity<String> resp = client.exchange(HttpMethod.POST, "/api/auth/login",
                    "{\"username\":\"ghost\",\"password\":\"wrongpass\"}");
            lastStatus = resp.getStatusCode().value();
            if (lastStatus == 429) {
                got429 = true;
            }
        }
        // 3. 最终应被限流
        assertThat(got429).isTrue();
        assertThat(lastStatus).isEqualTo(429);
    }
}
