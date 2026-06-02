package com.nav.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 抓取站点 favicon 的请求体。
 *
 * @param url 站点地址
 */
public record FetchFaviconRequest(
        @NotBlank(message = "站点地址不能为空") @Size(max = 2048, message = "地址过长") String url) {
}
