package com.nav.engine;

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

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 搜索引擎接口集成测试。
 * 经 Testcontainers 起真实 PG+Redis,验证预置引擎初始化、用户隔离、增删改、排序、设默认、越权拦截。
 * 初始管理员由 AdminInitializer 据 @TestPropertySource 的凭据创建。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=engadmin",
        "app.admin.password=engadminpass123"
})
class EngineIntegrationTest {

    /** 1x1 PNG(base64 解码),作引擎自定义图标的上传内容 */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 新开用户自动获得 4 个预置引擎,Google 为默认且带内置图标 key。
     */
    @Test
    void newUserHasFourPresetEngines() throws Exception {
        // 1. 管理员开户并以新用户登录
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "preset_user", "presetpass123");
        // 2. 取引擎列表
        JsonNode engines = listEngines(user);
        // 3. 恰好 4 个且全部为预置
        assertThat(engines.size()).isEqualTo(4);
        for (JsonNode e : engines) {
            assertThat(e.path("isPreset").asBoolean()).isTrue();
        }
        // 4. 默认引擎唯一且为 Google
        List<String> defaults = new ArrayList<>();
        for (JsonNode e : engines) {
            if (e.path("isDefault").asBoolean()) {
                defaults.add(e.path("name").asText());
            }
        }
        assertThat(defaults).containsExactly("Google");
        // 5. 排序首位为 Google,带内置图标 key
        assertThat(engines.get(0).path("name").asText()).isEqualTo("Google");
        assertThat(engines.get(0).path("iconBuiltin").asText()).isEqualTo("google");
    }

    /**
     * 引擎按用户隔离:A 新建的引擎 B 不可见。
     */
    @Test
    void enginesIsolatedBetweenUsers() throws Exception {
        // 1. 开两个用户
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient userA = createAndLoginUser(admin, "iso_a", "isoapass123");
        ApiClient userB = createAndLoginUser(admin, "iso_b", "isobpass123");
        // 2. A 新建一个自定义引擎
        ResponseEntity<String> created = userA.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"MyEngine\",\"urlTemplate\":\"https://e.com/s?q={query}\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. A 有 5 个引擎,B 仍只有 4 个预置且看不到 A 的引擎
        assertThat(listEngines(userA).size()).isEqualTo(5);
        JsonNode bEngines = listEngines(userB);
        assertThat(bEngines.size()).isEqualTo(4);
        for (JsonNode e : bEngines) {
            assertThat(e.path("name").asText()).isNotEqualTo("MyEngine");
        }
    }

    /**
     * 引擎增删改全流程:新建追加末尾、更新、删除。
     */
    @Test
    void createUpdateDeleteEngine() throws Exception {
        // 1. 新用户登录
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "crud_user", "crudpass123");
        // 2. 新建引擎,追加到末尾(sortOrder=4,非预置)
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"Custom\",\"urlTemplate\":\"https://c.com/s?q={query}\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createdNode = objectMapper.readTree(created.getBody());
        String id = createdNode.path("id").asText();
        assertThat(createdNode.path("sortOrder").asInt()).isEqualTo(4);
        assertThat(createdNode.path("isPreset").asBoolean()).isFalse();
        // 3. 更新名称与模板
        ResponseEntity<String> updated = user.exchange(HttpMethod.PUT, "/api/engines/" + id,
                "{\"name\":\"Custom2\",\"urlTemplate\":\"https://c2.com/s?q={query}\"}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(updated.getBody()).path("name").asText()).isEqualTo("Custom2");
        // 4. 删除
        ResponseEntity<String> deleted = user.exchange(HttpMethod.DELETE, "/api/engines/" + id, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // 5. 删除后回到 4 个预置
        assertThat(listEngines(user).size()).isEqualTo(4);
    }

    /**
     * 自定义引擎可设置上传图标:建引擎带 iconAssetId 时响应回显该 id;更新传 null 可清除图标。
     */
    @Test
    void createEngineWithCustomIcon() throws Exception {
        // 1. 新用户登录
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "icon_user", "iconpass123");
        // 2. 先上传一张图拿媒体 id(引擎图标受外键约束,须引用真实媒体)
        ResponseEntity<String> uploaded = user.postMultipart("/api/media/upload", "file", "icon.png", "image/png", PNG);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        String assetId = objectMapper.readTree(uploaded.getBody()).path("id").asText();
        // 3. 建引擎带该图标,响应回显 iconAssetId
        ResponseEntity<String> created = user.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"WithIcon\",\"urlTemplate\":\"https://i.com/s?q={query}\",\"iconAssetId\":\"" + assetId + "\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createdNode = objectMapper.readTree(created.getBody());
        String engineId = createdNode.path("id").asText();
        assertThat(createdNode.path("iconAssetId").asText()).isEqualTo(assetId);
        // 4. 更新时把图标清空(iconAssetId 传 null)
        ResponseEntity<String> updated = user.exchange(HttpMethod.PUT, "/api/engines/" + engineId,
                "{\"name\":\"WithIcon\",\"urlTemplate\":\"https://i.com/s?q={query}\",\"iconAssetId\":null}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(updated.getBody()).path("iconAssetId").isNull()).isTrue();
    }

    /**
     * 引擎自定义图标须属于本人:引用不存在或他人的 iconAssetId 一律 400。
     */
    @Test
    void createEngineWithInvalidIconReturns400() throws Exception {
        // 1. 开两个用户
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient userA = createAndLoginUser(admin, "icon_inv_a", "iconinvapass123");
        ApiClient userB = createAndLoginUser(admin, "icon_inv_b", "iconinvbpass123");
        // 2. A 引用不存在的图标 id 建引擎 -> 400
        ResponseEntity<String> nonexistent = userA.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"X\",\"urlTemplate\":\"https://x.com/s?q={query}\",\"iconAssetId\":\""
                        + java.util.UUID.randomUUID() + "\"}");
        assertThat(nonexistent.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(nonexistent.getBody()).path("code").asText()).isEqualTo("ICON_ASSET_INVALID");
        // 3. B 上传一张图,A 引用 B 的图标建引擎 -> 同样 400(归属校验)
        String bAsset = objectMapper.readTree(
                        userB.postMultipart("/api/media/upload", "file", "b.png", "image/png", PNG).getBody())
                .path("id").asText();
        ResponseEntity<String> foreign = userA.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"Y\",\"urlTemplate\":\"https://y.com/s?q={query}\",\"iconAssetId\":\"" + bAsset + "\"}");
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(foreign.getBody()).path("code").asText()).isEqualTo("ICON_ASSET_INVALID");
    }

    /**
     * 缺少 {query} 占位的 URL 模板被拒(400)。
     */
    @Test
    void createEngineWithoutPlaceholderRejected() throws Exception {
        // 1. 新用户登录后用无占位模板建引擎
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "tpl_user", "tplpass123");
        ResponseEntity<String> resp = user.exchange(HttpMethod.POST, "/api/engines",
                "{\"name\":\"Bad\",\"urlTemplate\":\"https://b.com/search\"}");
        // 2. 校验失败返回 400
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * 重排引擎:逆序提交后列表顺序随之改变。
     */
    @Test
    void reorderEngines() throws Exception {
        // 1. 新用户登录,取 4 预置引擎当前顺序的 id
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "order_user", "orderpass123");
        List<String> ids = new ArrayList<>();
        for (JsonNode e : listEngines(user)) {
            ids.add(e.path("id").asText());
        }
        // 2. 逆序提交排序
        List<String> reversed = new ArrayList<>(ids);
        Collections.reverse(reversed);
        String body = "{\"orderedIds\":[" + reversed.stream()
                .map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]}";
        ResponseEntity<String> resp = user.exchange(HttpMethod.PUT, "/api/engines/order", body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. 重新取列表,首尾应已对调
        JsonNode after = listEngines(user);
        assertThat(after.get(0).path("id").asText()).isEqualTo(ids.get(3));
        assertThat(after.get(3).path("id").asText()).isEqualTo(ids.get(0));
    }

    /**
     * 设默认引擎:切换后旧默认取消、新默认生效。
     */
    @Test
    void setDefaultEngine() throws Exception {
        // 1. 新用户登录,取引擎列表
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "default_user", "defaultpass123");
        JsonNode engines = listEngines(user);
        // 2. 把第二个(百度)设为默认
        String baiduId = engines.get(1).path("id").asText();
        ResponseEntity<String> resp = user.exchange(HttpMethod.PUT, "/api/engines/" + baiduId + "/default", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. 重新取列表,有且仅有百度为默认
        JsonNode after = listEngines(user);
        for (JsonNode e : after) {
            assertThat(e.path("isDefault").asBoolean()).isEqualTo(e.path("id").asText().equals(baiduId));
        }
    }

    /**
     * 越权:操作他人引擎返回 404(不暴露资源存在性)。
     */
    @Test
    void crossUserAccessReturns404() throws Exception {
        // 1. 开两个用户
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient userA = createAndLoginUser(admin, "x_a", "xapass123");
        ApiClient userB = createAndLoginUser(admin, "x_b", "xbpass123");
        // 2. 取 A 的一个引擎 id
        String aEngineId = listEngines(userA).get(0).path("id").asText();
        // 3. B 尝试把 A 的引擎设默认 -> 404
        ResponseEntity<String> setDefault = userB.exchange(HttpMethod.PUT, "/api/engines/" + aEngineId + "/default", null);
        assertThat(setDefault.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // 4. B 尝试删除 A 的引擎 -> 404
        ResponseEntity<String> delete = userB.exchange(HttpMethod.DELETE, "/api/engines/" + aEngineId, null);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 删除默认引擎后,剩余排序最前的引擎补设为默认。
     */
    @Test
    void deleteDefaultReassignsDefault() throws Exception {
        // 1. 新用户登录,取默认(Google)与次位(百度)的 id
        ApiClient admin = loginAs("engadmin", "engadminpass123");
        ApiClient user = createAndLoginUser(admin, "deldef_user", "deldefpass123");
        JsonNode engines = listEngines(user);
        String googleId = engines.get(0).path("id").asText();
        String baiduId = engines.get(1).path("id").asText();
        // 2. 删除默认引擎 Google
        ResponseEntity<String> deleted = user.exchange(HttpMethod.DELETE, "/api/engines/" + googleId, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // 3. 剩余排序最前(百度)成为新默认
        JsonNode after = listEngines(user);
        assertThat(after.get(0).path("id").asText()).isEqualTo(baiduId);
        assertThat(after.get(0).path("isDefault").asBoolean()).isTrue();
    }

    // ===== 测试辅助 =====

    /**
     * 取当前客户端用户的引擎列表(JSON 数组节点)。
     */
    private JsonNode listEngines(ApiClient client) throws Exception {
        ResponseEntity<String> resp = client.exchange(HttpMethod.GET, "/api/engines", null);
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
