package com.nav.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 从图片外链保存的请求体。
 *
 * @param url 图片地址
 */
public record SaveFromUrlRequest(
        @NotBlank(message = "图片地址不能为空") @Size(max = 2048, message = "地址过长") String url) {
}
