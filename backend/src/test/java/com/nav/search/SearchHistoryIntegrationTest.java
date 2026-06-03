package com.nav.search;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 搜索历史接口集成测试。
 * 经 Testcontainers 起真实 PG+Redis,验证记录去重置顶、列出、删除、清空、用户隔离、越权与未登录拦截。
 * 初始管理员由 AdminInitializer 据 @TestPropertySource 的凭据创建。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=shadmin",
        "app.admin.password=shadminpass123"
})
class SearchHistoryIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录关键词后能在列表里查到,最近搜索的排在最前。
     */
    @Test
    void recordThenList() throws Exception {
        // 1. 新用户依次搜两个词
        ApiClient user = newUser("sh_list", "shlistpass123");
        record(user, "vue");
        record(user, "spring boot");
        // 2. 列表两条,最近的(spring boot)在最前
        JsonNode list = listHistory(user);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).path("keyword").asText()).isEqualTo("spring boot");
        assertThat(list.get(1).path("keyword").asText()).isEqualTo("vue");
    }

    /**
     * 同一关键词重复搜索去重:只存一条,且被置顶到最前。
     */
    @Test
    void duplicateKeywordDedup() throws Exception {
        // 1. 搜 alpha、beta,再搜一次 alpha
        ApiClient user = newUser("sh_dup", "shduppass123");
        record(user, "alpha");
        record(user, "beta");
        record(user, "alpha");
        // 2. 仍只两条,且 alpha 被置顶
        JsonNode list = listHistory(user);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).path("keyword").asText()).isEqualTo("alpha");
    }

    /**
     * 搜索历史按用户隔离:A 的历史 B 看不到。
     */
    @Test
    void isolatedBetweenUsers() throws Exception {
        // 1. 两个用户,A 搜一次
        ApiClient a = newUser("sh_iso_a", "shisoapass123");
        ApiClient b = newUser("sh_iso_b", "shisobpass123");
        record(a, "secret query");
        // 2. A 有 1 条,B 看不到
        assertThat(listHistory(a).size()).isEqualTo(1);
        assertThat(listHistory(b).size()).isEqualTo(0);
    }

    /**
     * 删除一条搜索历史后列表减少。
     */
    @Test
    void deleteOne() throws Exception {
        // 1. 搜两个词
        ApiClient user = newUser("sh_del", "shdelpass123");
        record(user, "k1");
        record(user, "k2");
        // 2. 删较早的 k1(在列表末尾)
        JsonNode list = listHistory(user);
        String id = list.get(1).path("id").asText();
        ResponseEntity<String> del = user.exchange(HttpMethod.DELETE, "/api/search-history/" + id, null);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // 3. 只剩 k2
        JsonNode after = listHistory(user);
        assertThat(after.size()).isEqualTo(1);
        assertThat(after.get(0).path("keyword").asText()).isEqualTo("k2");
    }

    /**
     * 清空全部搜索历史。
     */
    @Test
    void clearAll() throws Exception {
        // 1. 搜两个词后清空
        ApiClient user = newUser("sh_clear", "shclearpass123");
        record(user, "x");
        record(user, "y");
        ResponseEntity<String> clear = user.exchange(HttpMethod.DELETE, "/api/search-history", null);
        assertThat(clear.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // 2. 列表为空
        assertThat(listHistory(user).size()).isEqualTo(0);
    }

    /**
     * 越权:删他人搜索历史返回 404(不暴露存在性),原记录不受影响。
     */
    @Test
    void crossUserDeleteReturns404() throws Exception {
        // 1. A 搜一次拿到记录 id
        ApiClient a = newUser("sh_x_a", "shxapass123");
        ApiClient b = newUser("sh_x_b", "shxbpass123");
        record(a, "owned by a");
        String id = listHistory(a).get(0).path("id").asText();
        // 2. B 删 A 的记录 -> 404
        ResponseEntity<String> del = b.exchange(HttpMethod.DELETE, "/api/search-history/" + id, null);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // 3. A 的记录仍在
        assertThat(listHistory(a).size()).isEqualTo(1);
    }

    /**
     * 未登录访问搜索历史接口返回 401。
     */
    @Test
    void unauthenticatedReturns401() {
        // 1. 未登录直接取列表
        ApiClient anon = new ApiClient(rest);
        ResponseEntity<String> resp = anon.exchange(HttpMethod.GET, "/api/search-history", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ===== 测试辅助 =====

    /**
     * 记录一次搜索。
     */
    private void record(ApiClient client, String keyword) {
        ResponseEntity<String> resp = client.exchange(HttpMethod.POST, "/api/search-history",
                "{\"keyword\":\"" + keyword + "\"}");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 取当前用户的搜索历史列表(JSON 数组节点)。
     */
    private JsonNode listHistory(ApiClient client) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.GET, "/api/search-history", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody());
    }

    /**
     * 管理员开户并以新用户登录。
     */
    private ApiClient newUser(String username, String password) {
        ApiClient admin = loginAs("shadmin", "shadminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginAs(username, password);
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
