package com.nav.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 集成测试用的轻量 HTTP 客户端:自动维护 Cookie(会话 + CSRF),写请求自动带 X-XSRF-TOKEN 头。
 * 一个实例代表一个浏览器会话。支持 JSON、multipart 上传、二进制读取三类请求。
 */
public class ApiClient {

    private final TestRestTemplate rest;
    private final Map<String, String> cookies = new HashMap<>();

    public ApiClient(TestRestTemplate rest) {
        this.rest = rest;
    }

    /**
     * 发起一次 JSON 请求:自动带 Cookie,非 GET 附加 CSRF 头,并收集响应 Cookie。
     *
     * @param method   HTTP 方法
     * @param path     路径
     * @param jsonBody 请求体(可为 null)
     * @return 响应
     */
    public ResponseEntity<String> exchange(HttpMethod method, String path, String jsonBody) {
        // 1. 组装认证头并指定 JSON 类型
        HttpHeaders headers = authHeaders(method);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 2. 发请求并收集 Cookie
        ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<>(jsonBody, headers), String.class);
        collectCookies(response);
        return response;
    }

    /**
     * 以二进制方式 GET(读媒体文件),返回字节响应。
     *
     * @param path 路径
     * @return 字节响应
     */
    public ResponseEntity<byte[]> getBytes(String path) {
        // 1. 组装认证头(GET 不需 CSRF)
        HttpHeaders headers = authHeaders(HttpMethod.GET);
        // 2. 发请求并收集 Cookie
        ResponseEntity<byte[]> response = rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        collectCookies(response);
        return response;
    }

    /**
     * 以 multipart 表单上传一个文件部分。
     *
     * @param path        路径
     * @param partName    表单字段名
     * @param filename    文件名
     * @param contentType 文件声明类型(可为 null)
     * @param content     文件字节
     * @return 响应
     */
    public ResponseEntity<String> postMultipart(String path, String partName, String filename,
                                                String contentType, byte[] content) {
        // 1. 组装认证头并指定 multipart 类型
        HttpHeaders headers = authHeaders(HttpMethod.POST);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // 2. 把字节包成带文件名的资源,作为一个表单部分
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        if (contentType != null) {
            partHeaders.setContentType(MediaType.parseMediaType(contentType));
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(partName, new HttpEntity<>(resource, partHeaders));
        // 3. 发请求并收集 Cookie
        ResponseEntity<String> response = rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        collectCookies(response);
        return response;
    }

    /**
     * 组装认证头:带上已有 Cookie,非 GET 请求附加 CSRF 头。
     */
    private HttpHeaders authHeaders(HttpMethod method) {
        // 1. 带上已收集的 Cookie
        HttpHeaders headers = new HttpHeaders();
        if (!cookies.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; ")));
        }
        // 2. 写请求附加 CSRF 头
        if (method != HttpMethod.GET && cookies.containsKey("XSRF-TOKEN")) {
            headers.add("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
        }
        return headers;
    }

    /**
     * 把响应里的 Set-Cookie 收进 Cookie 表(只取 name=value 部分)。
     */
    private void collectCookies(ResponseEntity<?> response) {
        // 1. 没有 Set-Cookie 直接返回
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return;
        }
        // 2. 逐条提取 name=value 存入 Cookie 表
        for (String setCookie : setCookies) {
            String pair = setCookie.split(";", 2)[0];
            int eq = pair.indexOf('=');
            if (eq > 0) {
                cookies.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
    }
}
