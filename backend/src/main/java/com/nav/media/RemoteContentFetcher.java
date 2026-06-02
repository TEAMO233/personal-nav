package com.nav.media;

import com.nav.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 远程内容抓取:用统一的超时、大小上限和 SSRF 校验下载网页或图片。
 * 自己处理重定向,每跳都重新校验地址,杜绝跳转到内网。
 */
@Component
public class RemoteContentFetcher {

    private final SsrfGuard ssrfGuard;
    /** 单次下载字节上限 */
    private final long maxBytes;
    /** 最多跟随的重定向次数 */
    private final int maxRedirects;
    /** 读取响应超时 */
    private final Duration readTimeout;
    private final HttpClient httpClient;

    public RemoteContentFetcher(SsrfGuard ssrfGuard,
                                @Value("${app.media.fetch.max-bytes:2097152}") long maxBytes,
                                @Value("${app.media.fetch.max-redirects:3}") int maxRedirects,
                                @Value("${app.media.fetch.connect-timeout-ms:3000}") long connectTimeoutMs,
                                @Value("${app.media.fetch.read-timeout-ms:5000}") long readTimeoutMs) {
        this.ssrfGuard = ssrfGuard;
        this.maxBytes = maxBytes;
        this.maxRedirects = maxRedirects;
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        // 1. 关掉自动重定向,改由本类逐跳校验后手动跟随
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * 抓取目标地址内容,自动跟随并逐跳校验重定向。
     *
     * @param rawUrl 目标地址
     * @return 抓取结果(字节、内容类型、最终地址)
     */
    public FetchResult fetch(String rawUrl) {
        // 1. 解析地址
        URI uri = parse(rawUrl);
        // 2. 最多跟随 maxRedirects 次重定向
        for (int hop = 0; hop <= maxRedirects; hop++) {
            // 2.1 每跳都先做 SSRF 校验
            ssrfGuard.check(uri);
            HttpResponse<InputStream> response = send(uri);
            int status = response.statusCode();
            // 2.2 命中重定向:取 Location 拼成新地址,继续下一跳
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("location").orElse(null);
                closeQuietly(response);
                if (location == null) {
                    throw fetchFailed("重定向缺少目标地址");
                }
                uri = uri.resolve(location);
                continue;
            }
            // 2.3 非 2xx 视为失败
            if (status < 200 || status >= 300) {
                closeQuietly(response);
                throw fetchFailed("远程返回异常状态 " + status);
            }
            // 2.4 读取响应体(带大小上限)
            byte[] body = readLimited(response.body());
            String contentType = response.headers().firstValue("content-type").orElse(null);
            return new FetchResult(body, contentType, uri);
        }
        // 3. 超过重定向上限
        throw fetchFailed("重定向次数过多");
    }

    /**
     * 发一次 GET 请求,带读取超时。
     */
    private HttpResponse<InputStream> send(URI uri) {
        try {
            // 1. 组装请求并发送,响应体以流形式拿,便于边读边限大小
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(readTimeout)
                    .header("User-Agent", "PersonalNav/1.0")
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw fetchFailed("无法连接目标地址");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw fetchFailed("抓取被中断");
        }
    }

    /**
     * 读取输入流,累计字节超过上限就中断并报错。
     */
    private byte[] readLimited(InputStream in) {
        try (in) {
            // 1. 分块读,边读边累计,超限即拒
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            long total = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > maxBytes) {
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "MEDIA_TOO_LARGE", "文件超过大小上限");
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw fetchFailed("读取远程内容失败");
        }
    }

    /**
     * 解析地址字符串为 URI。
     */
    private URI parse(String rawUrl) {
        try {
            return URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIA_URL_INVALID", "地址格式不正确");
        }
    }

    /**
     * 安静关闭重定向响应的流,忽略关闭异常。
     */
    private void closeQuietly(HttpResponse<InputStream> response) {
        try {
            response.body().close();
        } catch (IOException ignored) {
            // 关闭失败无需处理
        }
    }

    /**
     * 构造一个「抓取失败」的 502 异常。
     */
    private ApiException fetchFailed(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "MEDIA_FETCH_FAILED", message);
    }

    /**
     * 抓取结果。
     *
     * @param bytes       内容字节
     * @param contentType 响应声明的内容类型(可空)
     * @param finalUri    跟随重定向后的最终地址
     */
    public record FetchResult(byte[] bytes, String contentType, URI finalUri) {
    }
}
