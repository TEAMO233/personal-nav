package com.nav.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.TestcontainersConfiguration;
import com.nav.support.ApiClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 媒体接口集成测试。
 * 经 Testcontainers 起真实 PG+Redis,并用本机临时 HTTP 桩站点验证三种图标来源(上传 / 外链 / favicon)
 * 全链路存取,以及越权 404、SSRF 私网拦截、非图片拒收、未登录拦截等安全约束。
 * 放行环回(allow-loopback=true)以便抓取本机桩站点;私网与链路本地地址仍被拒。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.admin.username=mediaadmin",
        "app.admin.password=mediaadminpass123",
        "app.media.fetch.allow-loopback=true",
        "app.storage.local.base-dir=target/it-media"
})
class MediaIntegrationTest {

    /** 1x1 PNG(base64 解码),作上传与桩图标内容 */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    /** 最小 ICO 字节(仅含魔数,够探测识别) */
    private static final byte[] ICO = {0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    /** 测试期文件落盘目录,测试结束清理 */
    private static final Path STORAGE_DIR = Path.of("target/it-media");

    private static HttpServer stub;
    private static String base;

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 启动本机 HTTP 桩站点,提供 HTML 页面、图标与根 favicon.ico。
     */
    @BeforeAll
    static void startStub() throws IOException {
        // 1. 绑定本机随机端口
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // 2. 声明了图标的页面(link 指向 /icon.png)
        serve("/page.html", "text/html",
                "<html><head><link rel=\"icon\" href=\"/icon.png\"></head><body>hi</body></html>"
                        .getBytes(StandardCharsets.UTF_8));
        // 3. 无图标声明的页面(用于验证回退到 /favicon.ico)
        serve("/no-icon.html", "text/html",
                "<html><head><title>x</title></head><body>hi</body></html>".getBytes(StandardCharsets.UTF_8));
        // 4. 图标与根 favicon
        serve("/icon.png", "image/png", PNG);
        serve("/favicon.ico", "image/x-icon", ICO);
        stub.start();
        base = "http://127.0.0.1:" + stub.getAddress().getPort();
    }

    /**
     * 关停桩站点并递归清理测试落盘文件。
     */
    @AfterAll
    static void stopStub() throws IOException {
        // 1. 停桩
        if (stub != null) {
            stub.stop(0);
        }
        // 2. 删测试期写入的文件
        if (Files.exists(STORAGE_DIR)) {
            try (var paths = Files.walk(STORAGE_DIR)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 清理失败忽略,不影响测试结论
                    }
                });
            }
        }
    }

    /**
     * 上传图片后,经 /api/media/{id} 原样读回,内容类型为 image/png。
     */
    @Test
    void uploadThenReadBack() throws Exception {
        // 1. 新用户登录并上传 PNG
        ApiClient user = newUser("up_user", "uppass123456");
        ResponseEntity<String> uploaded = user.postMultipart("/api/media/upload", "file", "a.png", "image/png", PNG);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 读回并比对字节与类型
        ResponseEntity<byte[]> got = user.getBytes("/api/media/" + idOf(uploaded));
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(got.getBody()).isEqualTo(PNG);
        assertThat(got.getHeaders().getContentType()).isNotNull();
        assertThat(got.getHeaders().getContentType().toString()).isEqualTo("image/png");
    }

    /**
     * 从图片外链下载保存后能原样读回。
     */
    @Test
    void saveFromUrlThenReadBack() throws Exception {
        // 1. 从桩站点的图片外链保存
        ApiClient user = newUser("url_user", "urlpass123456");
        ResponseEntity<String> saved = user.exchange(HttpMethod.POST, "/api/media/from-url",
                "{\"url\":\"" + base + "/icon.png\"}");
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 读回比对
        ResponseEntity<byte[]> got = user.getBytes("/api/media/" + idOf(saved));
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(got.getBody()).isEqualTo(PNG);
    }

    /**
     * 抓取站点 favicon:解析 HTML 的 link 标签拿到图标并保存。
     */
    @Test
    void fetchFaviconFromHtmlLink() throws Exception {
        // 1. 抓取声明了 icon 的页面
        ApiClient user = newUser("fav_user", "favpass123456");
        ResponseEntity<String> fav = user.exchange(HttpMethod.POST, "/api/media/fetch-favicon",
                "{\"url\":\"" + base + "/page.html\"}");
        assertThat(fav.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 读回应为 link 指向的 png
        ResponseEntity<byte[]> got = user.getBytes("/api/media/" + idOf(fav));
        assertThat(got.getBody()).isEqualTo(PNG);
    }

    /**
     * 站点无 icon 声明时,回退抓取根目录 /favicon.ico。
     */
    @Test
    void fetchFaviconFallsBackToFaviconIco() throws Exception {
        // 1. 抓取无 icon 声明的页面
        ApiClient user = newUser("fb_user", "fbpass123456");
        ResponseEntity<String> fav = user.exchange(HttpMethod.POST, "/api/media/fetch-favicon",
                "{\"url\":\"" + base + "/no-icon.html\"}");
        assertThat(fav.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 读回应为根 favicon.ico 内容
        ResponseEntity<byte[]> got = user.getBytes("/api/media/" + idOf(fav));
        assertThat(got.getBody()).isEqualTo(ICO);
    }

    /**
     * 越权:读取他人媒体返回 404,不暴露资源存在性。
     */
    @Test
    void crossUserReadReturns404() throws Exception {
        // 1. A 上传得到 id
        ApiClient userA = newUser("own_a", "ownapass12345");
        String id = idOf(userA.postMultipart("/api/media/upload", "file", "a.png", "image/png", PNG));
        // 2. B 读 A 的媒体 -> 404
        ApiClient userB = newUser("own_b", "ownbpass12345");
        ResponseEntity<byte[]> got = userB.getBytes("/api/media/" + id);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * SSRF:抓取私网 / 链路本地地址被拒(即便放行了环回)。
     */
    @Test
    void blocksPrivateNetworkUrl() throws Exception {
        // 1. 云元数据地址(链路本地 169.254.0.0/16)应被拦
        ApiClient user = newUser("ssrf_user", "ssrfpass12345");
        ResponseEntity<String> resp = user.exchange(HttpMethod.POST, "/api/media/from-url",
                "{\"url\":\"http://169.254.169.254/latest/meta-data/\"}");
        // 2. 返回 400 且错误码为地址被拦
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(resp.getBody()).path("code").asText()).isEqualTo("MEDIA_URL_BLOCKED");
    }

    /**
     * 上传非图片内容被拒(415),按文件头探测挡住伪装。
     */
    @Test
    void rejectsNonImageUpload() throws Exception {
        // 1. 上传纯文本冒充图片
        ApiClient user = newUser("type_user", "typepass12345");
        ResponseEntity<String> resp = user.postMultipart("/api/media/upload", "file", "a.txt", "text/plain",
                "this is plain text, not an image".getBytes(StandardCharsets.UTF_8));
        // 2. 返回 415
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 未登录访问媒体接口返回 401。
     */
    @Test
    void unauthenticatedReturns401() {
        // 1. 未登录直接读取
        ApiClient anon = new ApiClient(rest);
        ResponseEntity<byte[]> got = anon.getBytes("/api/media/" + UUID.randomUUID());
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ===== 测试辅助 =====

    /**
     * 注册一个桩路由,固定返回给定内容与类型。
     */
    private static void serve(String path, String contentType, byte[] body) {
        stub.createContext(path, exchange -> {
            // 1. 写内容类型并发送响应体
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    /**
     * 管理员开户并以新用户登录。
     */
    private ApiClient newUser(String username, String password) {
        // 1. 管理员开户
        ApiClient admin = loginAs("mediaadmin", "mediaadminpass123");
        ResponseEntity<String> created = admin.exchange(HttpMethod.POST, "/api/admin/users",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 2. 以新用户登录
        return loginAs(username, password);
    }

    /**
     * 先取 CSRF 再登录,返回带会话的客户端。
     */
    private ApiClient loginAs(String username, String password) {
        // 1. GET 一次拿到 CSRF cookie
        ApiClient client = new ApiClient(rest);
        client.exchange(HttpMethod.GET, "/api/auth/me", null);
        // 2. 登录
        ResponseEntity<String> login = client.exchange(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return client;
    }

    /**
     * 从媒体响应体里取出 id 字段。
     */
    private String idOf(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).path("id").asText();
    }
}
