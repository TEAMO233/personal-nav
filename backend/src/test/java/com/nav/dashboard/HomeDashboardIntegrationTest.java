package com.nav.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.TestcontainersConfiguration;
import com.nav.support.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 首页工作台资源集成测试。
 * 验证待办、便签、最近访问、通知和首页精选快捷方式的真实数据链路与用户隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=hdadmin",
        "app.admin.password=hdadminpass123"
})
class HomeDashboardIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 新用户会初始化 8 个首页精选快捷方式。
     */
    @Test
    void newUserHasFeaturedShortcuts() throws Exception {
        // 1. 开户并登录
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient user = createAndLoginUser(admin, "hd_featured", "hdfeaturedpass123");
        // 2. 首页精选返回 8 条,且包含描述与 featured 标记
        ResponseEntity<String> resp = user.exchange(HttpMethod.GET, "/api/shortcuts/featured", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = objectMapper.readTree(resp.getBody());
        assertThat(list.size()).isEqualTo(8);
        assertThat(list.get(0).path("featured").asBoolean()).isTrue();
        assertThat(list.get(0).path("description").asText()).isNotBlank();
    }

    /**
     * 待办 CRUD:新增、更新完成、删除。
     */
    @Test
    void todoCrudWorks() throws Exception {
        // 1. 新建待办
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient user = createAndLoginUser(admin, "hd_todo", "hdtodopass123");
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/todos",
                "{\"title\":\"完成首页\",\"tag\":\"工作\",\"scheduledAt\":\"2030-01-01T10:00:00Z\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode node = objectMapper.readTree(created.getBody());
        String id = node.path("id").asText();
        assertThat(node.path("done").asBoolean()).isFalse();
        // 2. 更新为完成
        ResponseEntity<String> updated = user.exchange(HttpMethod.PUT, "/api/todos/" + id,
                "{\"title\":\"完成首页\",\"tag\":\"工作\",\"scheduledAt\":\"2030-01-01T10:00:00Z\",\"done\":true}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(updated.getBody()).path("done").asBoolean()).isTrue();
        // 3. 删除后列表为空
        ResponseEntity<String> deleted = user.exchange(HttpMethod.DELETE, "/api/todos/" + id, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list(user, "/api/todos").size()).isEqualTo(0);
    }

    /**
     * 便签 CRUD:新增、更新置顶、删除。
     */
    @Test
    void noteCrudWorks() throws Exception {
        // 1. 新建便签
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient user = createAndLoginUser(admin, "hd_note", "hdnotepass123");
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/notes",
                "{\"content\":\"好的设计是少即是多。\",\"pinned\":true}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String id = objectMapper.readTree(created.getBody()).path("id").asText();
        // 2. 更新内容
        ResponseEntity<String> updated = user.exchange(HttpMethod.PUT, "/api/notes/" + id,
                "{\"content\":\"持续优化产品体验。\",\"pinned\":true}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(updated.getBody()).path("content").asText()).contains("持续优化");
        // 3. 删除
        ResponseEntity<String> deleted = user.exchange(HttpMethod.DELETE, "/api/notes/" + id, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list(user, "/api/notes").size()).isEqualTo(0);
    }

    /**
     * 连续新增便签后,首页列表保留多条并让最新便签排在第一位。
     */
    @Test
    void noteListKeepsMultipleCreatesAndNewestAppearsFirst() throws Exception {
        // 1. 连续新建三条置顶便签
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient user = createAndLoginUser(admin, "hd_note_multi", "hdnotemultipass123");
        ResponseEntity<String> first = user.exchange(HttpMethod.POST, "/api/notes",
                "{\"content\":\"第一条灵感\",\"pinned\":true}");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        Thread.sleep(5);
        ResponseEntity<String> second = user.exchange(HttpMethod.POST, "/api/notes",
                "{\"content\":\"第二条灵感\",\"pinned\":true}");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        Thread.sleep(5);
        ResponseEntity<String> third = user.exchange(HttpMethod.POST, "/api/notes",
                "{\"content\":\"第三条灵感\",\"pinned\":true}");
        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 首页列表应返回多条,不是只剩最新一条
        JsonNode latest = list(user, "/api/notes?limit=3");
        assertThat(latest.size()).isEqualTo(3);
        assertThat(latest.get(0).path("content").asText()).isEqualTo("第三条灵感");
        assertThat(latest.get(1).path("content").asText()).isEqualTo("第二条灵感");
        assertThat(latest.get(2).path("content").asText()).isEqualTo("第一条灵感");
        // 3. 全量列表保留三条,不是覆盖前一条
        assertThat(list(user, "/api/notes").size()).isEqualTo(3);
    }

    /**
     * 最近访问可由 shortcutId 记录,并校验快捷方式归属。
     */
    @Test
    void recentVisitRecordsShortcutAndRejectsForeignShortcut() throws Exception {
        // 1. 开两个用户
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient userA = createAndLoginUser(admin, "hd_visit_a", "hdvisitapass123");
        ApiClient userB = createAndLoginUser(admin, "hd_visit_b", "hdvisitbpass123");
        String shortcutA = list(userA, "/api/shortcuts/featured").get(0).path("id").asText();
        // 2. A 记录自己的快捷方式访问
        ResponseEntity<String> recorded = userA.exchange(HttpMethod.POST, "/api/recent-visits",
                "{\"shortcutId\":\"" + shortcutA + "\"}");
        assertThat(recorded.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode visit = objectMapper.readTree(recorded.getBody());
        assertThat(visit.path("shortcutId").asText()).isEqualTo(shortcutA);
        assertThat(list(userA, "/api/recent-visits").size()).isEqualTo(1);
        // 3. B 用 A 的快捷方式记录访问 -> 404
        ResponseEntity<String> rejected = userB.exchange(HttpMethod.POST, "/api/recent-visits",
                "{\"shortcutId\":\"" + shortcutA + "\"}");
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 首页书签支持设置页管理、首页只展示启用项,并按用户隔离。
     */
    @Test
    void homeBookmarkCrudOrderingAndIsolationWork() throws Exception {
        // 1. 开两个用户并创建两条书签
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient userA = createAndLoginUser(admin, "hd_bookmark_a", "hdbookmarkapass123");
        ApiClient userB = createAndLoginUser(admin, "hd_bookmark_b", "hdbookmarkbpass123");
        ResponseEntity<String> first = userA.exchange(HttpMethod.POST, "/api/home-bookmarks",
                "{\"name\":\"设计系统\",\"url\":\"https://developer.apple.com/design/\",\"description\":\"设计参考\",\"enabled\":true}");
        ResponseEntity<String> second = userA.exchange(HttpMethod.POST, "/api/home-bookmarks",
                "{\"name\":\"隐藏书签\",\"url\":\"https://example.com/hidden\",\"description\":\"暂不展示\",\"enabled\":false}");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        String firstId = objectMapper.readTree(first.getBody()).path("id").asText();
        String secondId = objectMapper.readTree(second.getBody()).path("id").asText();
        // 2. 默认列表只返回启用项,设置页列表返回全部
        ResponseEntity<String> enabledOnly = userA.exchange(HttpMethod.GET, "/api/home-bookmarks", null);
        assertThat(enabledOnly.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enabledOnly.getHeaders().getCacheControl()).isEqualTo(CacheControl.noStore().getHeaderValue());
        assertThat(objectMapper.readTree(enabledOnly.getBody()).size()).isEqualTo(1);
        JsonNode all = list(userA, "/api/home-bookmarks?enabledOnly=false");
        assertThat(all.size()).isEqualTo(2);
        // 3. 重排全部书签
        ResponseEntity<String> reordered = userA.exchange(HttpMethod.PUT, "/api/home-bookmarks/order",
                "{\"orderedIds\":[\"" + secondId + "\",\"" + firstId + "\"]}");
        assertThat(reordered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(reordered.getBody()).get(0).path("id").asText()).isEqualTo(secondId);
        // 4. 他人不可修改当前用户书签
        ResponseEntity<String> rejected = userB.exchange(HttpMethod.PUT, "/api/home-bookmarks/" + firstId,
                "{\"name\":\"越权\",\"url\":\"https://example.com\",\"enabled\":true}");
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // 5. 删除后设置页列表只剩 1 条
        ResponseEntity<String> deleted = userA.exchange(HttpMethod.DELETE, "/api/home-bookmarks/" + secondId, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list(userA, "/api/home-bookmarks?enabledOnly=false").size()).isEqualTo(1);
    }

    /**
     * 逾期待办会生成通知;待办完成后通知自动解决并从默认列表消失。
     */
    @Test
    void overdueTodoCreatesNotificationAndDoneResolvesIt() throws Exception {
        // 1. 新建一个已逾期待办
        ApiClient admin = loginAs("hdadmin", "hdadminpass123");
        ApiClient user = createAndLoginUser(admin, "hd_notify", "hdnotifypass123");
        String past = Instant.now().minusSeconds(3600).toString();
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/todos",
                "{\"title\":\"过期任务\",\"tag\":\"工作\",\"scheduledAt\":\"" + past + "\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String todoId = objectMapper.readTree(created.getBody()).path("id").asText();
        // 2. 拉通知时自动生成逾期通知
        JsonNode notifications = list(user, "/api/notifications");
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notifications.get(0).path("type").asText()).isEqualTo("TODO_OVERDUE");
        assertThat(list(user, "/api/notifications/unread-count").path("count").asLong()).isEqualTo(1);
        // 3. 完成待办后,通知解决并从默认列表消失
        ResponseEntity<String> done = user.exchange(HttpMethod.PUT, "/api/todos/" + todoId,
                "{\"title\":\"过期任务\",\"tag\":\"工作\",\"scheduledAt\":\"" + past + "\",\"done\":true}");
        assertThat(done.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list(user, "/api/notifications").size()).isEqualTo(0);
        assertThat(list(user, "/api/notifications/unread-count").path("count").asLong()).isEqualTo(0);
    }

    /**
     * 读取 JSON 节点。
     */
    private JsonNode list(ApiClient client, String path) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.GET, path, null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody());
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
