package com.nav.shortcut.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 更新快捷方式请求体(改名称 / URL / 图标;跨组移动走排序接口)。
 *
 * @param name        名称
 * @param url         目标 URL
 * @param iconAssetId 图标媒体 id(可空)
 */
public record UpdateShortcutRequest(
        @NotBlank(message = "名称不能为空") @Size(max = 64, message = "名称过长") String name,
        @NotBlank(message = "URL 不能为空") @Size(max = 2048, message = "URL 过长") String url,
        UUID iconAssetId) {
}
