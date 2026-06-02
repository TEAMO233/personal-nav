package com.nav.media;

import com.nav.common.error.ApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 网站图标(favicon)抓取:先抓站点 HTML 解析 link 标签取最优图标,
 * 解析不到再回退到站点根目录的 /favicon.ico。
 */
@Service
public class FaviconService {

    private final RemoteContentFetcher fetcher;

    public FaviconService(RemoteContentFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * 抓取站点 favicon 的图片内容。
     *
     * @param siteUrl 站点地址
     * @return 图标抓取结果(图片字节 + 内容类型)
     */
    public RemoteContentFetcher.FetchResult fetch(String siteUrl) {
        // 1. 抓站点 HTML(SSRF 校验在 fetcher 内做)
        RemoteContentFetcher.FetchResult page = fetcher.fetch(siteUrl);
        URI baseUri = page.finalUri();
        // 2. 解析 HTML 找最优图标地址
        String iconUrl = parseIconUrl(new String(page.bytes(), StandardCharsets.UTF_8), baseUri);
        // 3. 解析到就抓它,失败则继续走回退
        if (iconUrl != null) {
            try {
                return fetcher.fetch(iconUrl);
            } catch (ApiException ignored) {
                // 图标地址抓取失败,落到下面的回退
            }
        }
        // 4. 回退到站点根的 /favicon.ico
        String fallback = baseUri.resolve("/favicon.ico").toString();
        try {
            return fetcher.fetch(fallback);
        } catch (ApiException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MEDIA_FAVICON_NOT_FOUND", "未能获取站点图标");
        }
    }

    /**
     * 从 HTML 里解析出最优图标地址(按 link 标签的 sizes 挑最大的),没有则返回 null。
     */
    private String parseIconUrl(String html, URI baseUri) {
        // 1. jsoup 解析,baseUri 用于把相对地址补成绝对地址
        Document doc = Jsoup.parse(html, baseUri.toString());
        // 2. 取所有图标类 link 标签(rel 含 icon:覆盖 icon / shortcut icon / apple-touch-icon)
        Elements links = doc.select("link[rel~=(?i)icon]");
        // 3. 选 sizes 最大的一个(无 sizes 视为 0)
        String bestHref = null;
        int bestSize = -1;
        for (Element link : links) {
            String href = link.attr("abs:href");
            if (href.isBlank()) {
                continue;
            }
            int size = parseSize(link.attr("sizes"));
            if (size > bestSize) {
                bestSize = size;
                bestHref = href;
            }
        }
        // 4. 返回最优图标的绝对地址
        return bestHref;
    }

    /**
     * 从 sizes 属性(如 "32x32")解析出边长,解析不到返回 0。
     */
    private int parseSize(String sizes) {
        // 1. 空属性按 0 处理
        if (sizes == null || sizes.isBlank()) {
            return 0;
        }
        // 2. 取 "32x32" 里 x 前的数字
        String first = sizes.trim().toLowerCase().split("[ x]")[0];
        try {
            return Integer.parseInt(first);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
