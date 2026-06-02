package com.nav.shortcut;

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
 * 快捷方式接口集成测试。
 * 经 Testcontainers 起真实 PG+Redis,验证增删改、组内排序、跨组移动、用户隔离、越权拦截与删分组级联。
 * 初始管理员由 AdminInitializer 据 @TestPropertySource 的凭据创建。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=scadmin",
        "app.admin.password=scadminpass123"
})
class ShortcutIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 增删改全流程:同组内新建依次排到末尾、更新名称与 URL、删除后数量减少。
     */
    @Test
    void createUpdateDeleteShortcut() throws Exception {
        // 1. 新用户登录并建一个分组
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient user = createAndLoginUser(admin, "sc_crud", "sccrudpass123");
        String groupId = createGroup(user, "G1");
        // 2. 建第一个快捷方式,排到组内首位(sortOrder=0)
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/shortcuts",
                "{\"groupId\":\"" + groupId + "\",\"name\":\"Gmail\",\"url\":\"https://mail.google.com\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createdNode = objectMapper.readTree(created.getBody());
        String id = createdNode.path("id").asText();
        assertThat(createdNode.path("sortOrder").asInt()).isEqualTo(0);
        assertThat(createdNode.path("groupId").asText()).isEqualTo(groupId);
        // 3. 再建一个,排到组内末尾
        createShortcut(user, groupId, "Drive", "https://drive.google.com");
        assertThat(listShortcuts(user).size()).isEqualTo(2);
        // 4. 更新第一个的名称与 URL
        ResponseEntity<String> updated = user.exchange(HttpMethod.PUT, "/api/shortcuts/" + id,
                "{\"name\":\"Gmail2\",\"url\":\"https://mail.google.com/mail\"}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(updated.getBody()).path("name").asText()).isEqualTo("Gmail2");
        // 5. 删除第一个后仅剩一个
        ResponseEntity<String> deleted = user.exchange(HttpMethod.DELETE, "/api/shortcuts/" + id, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listShortcuts(user).size()).isEqualTo(1);
    }

    /**
     * 快捷方式按用户隔离:A 建的快捷方式 B 不可见。
     */
    @Test
    void shortcutsIsolatedBetweenUsers() throws Exception {
        // 1. 开两个用户
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient userA = createAndLoginUser(admin, "sc_iso_a", "scisoapass123");
        ApiClient userB = createAndLoginUser(admin, "sc_iso_b", "scisobpass123");
        // 2. A 建组与快捷方式
        String groupA = createGroup(userA, "GA");
        createShortcut(userA, groupA, "A_BM", "https://a.com");
        // 3. A 有 1 个,B 有 0 个
        assertThat(listShortcuts(userA).size()).isEqualTo(1);
        assertThat(listShortcuts(userB).size()).isEqualTo(0);
    }

    /**
     * 越权:操作他人快捷方式返回 404(不暴露资源存在性)。
     */
    @Test
    void crossUserAccessReturns404() throws Exception {
        // 1. 开两个用户,A 建一个快捷方式
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient userA = createAndLoginUser(admin, "sc_x_a", "scxapass123");
        ApiClient userB = createAndLoginUser(admin, "sc_x_b", "scxbpass123");
        String groupA = createGroup(userA, "GA");
        String scA = createShortcut(userA, groupA, "A_BM", "https://a.com");
        // 2. B 改 A 的快捷方式 -> 404
        ResponseEntity<String> upd = userB.exchange(HttpMethod.PUT, "/api/shortcuts/" + scA,
                "{\"name\":\"hack\",\"url\":\"https://h.com\"}");
        assertThat(upd.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // 3. B 删 A 的快捷方式 -> 404
        ResponseEntity<String> del = userB.exchange(HttpMethod.DELETE, "/api/shortcuts/" + scA, null);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 组内重排:互换排序值后列表顺序随之改变。
     */
    @Test
    void reorderWithinGroup() throws Exception {
        // 1. 新用户在一个组内建两个快捷方式
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient user = createAndLoginUser(admin, "sc_order", "scorderpass123");
        String groupId = createGroup(user, "G1");
        String s1 = createShortcut(user, groupId, "S1", "https://1.com");
        String s2 = createShortcut(user, groupId, "S2", "https://2.com");
        // 2. 组内互换:s2 排前(0)、s1 排后(1)
        String body = "{\"items\":["
                + "{\"id\":\"" + s2 + "\",\"groupId\":\"" + groupId + "\",\"sortOrder\":0},"
                + "{\"id\":\"" + s1 + "\",\"groupId\":\"" + groupId + "\",\"sortOrder\":1}]}";
        ResponseEntity<String> resp = user.exchange(HttpMethod.PUT, "/api/shortcuts/order", body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. 列表按排序值,s2 在前、s1 在后
        JsonNode after = listShortcuts(user);
        assertThat(after.get(0).path("id").asText()).isEqualTo(s2);
        assertThat(after.get(1).path("id").asText()).isEqualTo(s1);
    }

    /**
     * 跨组移动:把快捷方式的 groupId 改成另一分组后持久化生效。
     */
    @Test
    void moveShortcutAcrossGroups() throws Exception {
        // 1. 新用户建两个分组,组1 里建两个快捷方式
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient user = createAndLoginUser(admin, "sc_move", "scmovepass123");
        String g1 = createGroup(user, "G1");
        String g2 = createGroup(user, "G2");
        String s1 = createShortcut(user, g1, "S1", "https://1.com");
        String s2 = createShortcut(user, g1, "S2", "https://2.com");
        // 2. 把 s1 移到 g2,s2 留在 g1(全集快照提交)
        String body = "{\"items\":["
                + "{\"id\":\"" + s1 + "\",\"groupId\":\"" + g2 + "\",\"sortOrder\":0},"
                + "{\"id\":\"" + s2 + "\",\"groupId\":\"" + g1 + "\",\"sortOrder\":0}]}";
        ResponseEntity<String> resp = user.exchange(HttpMethod.PUT, "/api/shortcuts/order", body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. 验证持久化:s1 现属 g2,s2 仍属 g1
        JsonNode after = listShortcuts(user);
        String s1Group = null;
        String s2Group = null;
        for (JsonNode n : after) {
            if (n.path("id").asText().equals(s1)) {
                s1Group = n.path("groupId").asText();
            }
            if (n.path("id").asText().equals(s2)) {
                s2Group = n.path("groupId").asText();
            }
        }
        assertThat(s1Group).isEqualTo(g2);
        assertThat(s2Group).isEqualTo(g1);
    }

    /**
     * 排序快照不完整(漏传)被拒:返回 400。
     */
    @Test
    void reorderRejectsIncompleteSnapshot() throws Exception {
        // 1. 新用户在一个组内建两个快捷方式
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient user = createAndLoginUser(admin, "sc_incomplete", "scincpass123");
        String groupId = createGroup(user, "G1");
        String s1 = createShortcut(user, groupId, "S1", "https://1.com");
        createShortcut(user, groupId, "S2", "https://2.com");
        // 2. 只传 s1(漏传 s2)-> 400
        String body = "{\"items\":[{\"id\":\"" + s1 + "\",\"groupId\":\"" + groupId + "\",\"sortOrder\":0}]}";
        ResponseEntity<String> resp = user.exchange(HttpMethod.PUT, "/api/shortcuts/order", body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * 跨组移动到他人分组被拒:返回 400。
     */
    @Test
    void reorderRejectsForeignGroup() throws Exception {
        // 1. 开两个用户,A 建快捷方式、B 建一个分组
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient userA = createAndLoginUser(admin, "sc_fg_a", "scfgapass123");
        ApiClient userB = createAndLoginUser(admin, "sc_fg_b", "scfgbpass123");
        String groupA = createGroup(userA, "GA");
        String scA = createShortcut(userA, groupA, "S", "https://a.com");
        String groupB = createGroup(userB, "GB");
        // 2. A 试图把自己的快捷方式移到 B 的分组 -> 400
        String body = "{\"items\":[{\"id\":\"" + scA + "\",\"groupId\":\"" + groupB + "\",\"sortOrder\":0}]}";
        ResponseEntity<String> resp = userA.exchange(HttpMethod.PUT, "/api/shortcuts/order", body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * 在他人分组里新建快捷方式被拒:返回 404(不暴露分组存在性)。
     */
    @Test
    void createInForeignGroupReturns404() throws Exception {
        // 1. 开两个用户,A 建一个分组
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient userA = createAndLoginUser(admin, "sc_cf_a", "sccfapass123");
        ApiClient userB = createAndLoginUser(admin, "sc_cf_b", "sccfbpass123");
        String groupA = createGroup(userA, "GA");
        // 2. B 试图在 A 的分组里建快捷方式 -> 404
        ResponseEntity<String> resp = userB.exchange(HttpMethod.POST, "/api/shortcuts",
                "{\"groupId\":\"" + groupA + "\",\"name\":\"X\",\"url\":\"https://x.com\"}");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 删除分组级联删除其下全部快捷方式。
     */
    @Test
    void deleteGroupCascadesShortcuts() throws Exception {
        // 1. 新用户在一个组内建两个快捷方式
        ApiClient admin = loginAs("scadmin", "scadminpass123");
        ApiClient user = createAndLoginUser(admin, "sc_cascade", "sccascpass123");
        String groupId = createGroup(user, "G1");
        createShortcut(user, groupId, "S1", "https://1.com");
        createShortcut(user, groupId, "S2", "https://2.com");
        assertThat(listShortcuts(user).size()).isEqualTo(2);
        // 2. 删分组,其下快捷方式一并消失
        ResponseEntity<String> del = user.exchange(HttpMethod.DELETE, "/api/groups/" + groupId, null);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listShortcuts(user).size()).isEqualTo(0);
    }

    // ===== 测试辅助 =====

    /**
     * 取当前客户端用户的快捷方式列表(JSON 数组节点)。
     */
    private JsonNode listShortcuts(ApiClient client) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.GET, "/api/shortcuts", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody());
    }

    /**
     * 建一个分组并返回其 id。
     */
    private String createGroup(ApiClient client, String name) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.POST, "/api/groups",
                "{\"name\":\"" + name + "\"}");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody()).path("id").asText();
    }

    /**
     * 在指定分组下建一个快捷方式并返回其 id。
     */
    private String createShortcut(ApiClient client, String groupId, String name, String url) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.POST, "/api/shortcuts",
                "{\"groupId\":\"" + groupId + "\",\"name\":\"" + name + "\",\"url\":\"" + url + "\"}");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody()).path("id").asText();
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

    /**
     * 管理员开户后以该用户登录。
     */
    private ApiClient createAndLoginUser(ApiClient admin, String username, String password) {
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginAs(username, password);
    }
}
