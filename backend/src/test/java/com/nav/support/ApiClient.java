package com.nav.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 集成测试用的轻量 HTTP 客户端:自动维护 Cookie(会话 + CSRF),写请求自动带 X-XSRF-TOKEN 头。
 * 一个实例代表一个浏览器会话。
 */
public class ApiClient {

    private final TestRestTemplate rest;
    private final Map<String, String> cookies = new HashMap<>();

    public ApiClient(TestRestTemplate rest) {
        this.rest = rest;
    }

    /**
     * 发起一次请求:自动带上已有 Cookie,非 GET 请求附加 CSRF 头,并收集响应里的 Set-Cookie。
     *
     * @param method   HTTP 方法
     * @param path     路径
     * @param jsonBody 请求体(可为 null)
     * @return 响应
     */
    public ResponseEntity<String> exchange(HttpMethod method, String path, String jsonBody) {
        // 1. 组装请求头:Cookie + (写请求)CSRF 头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!cookies.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; ")));
        }
        if (method != HttpMethod.GET && cookies.containsKey("XSRF-TOKEN")) {
            headers.add("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
        }
        // 2. 发请求
        ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<>(jsonBody, headers), String.class);
        // 3. 收集响应 Cookie 供后续请求使用
        collectCookies(response);
        return response;
    }

    /**
     * 把响应里的 Set-Cookie 收进 Cookie 表(只取 name=value 部分)。
     */
    private void collectCookies(ResponseEntity<?> response) {
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return;
        }
        for (String setCookie : setCookies) {
            String pair = setCookie.split(";", 2)[0];
            int eq = pair.indexOf('=');
            if (eq > 0) {
                cookies.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
    }
}
