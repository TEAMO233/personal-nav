package com.nav.shortcut.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 新建快捷方式请求体。
 *
 * @param groupId     所属分组 id
 * @param name        名称
 * @param url         目标 URL
 * @param iconAssetId 图标媒体 id(可空)
 */
public record CreateShortcutRequest(
        @NotNull(message = "分组 id 不能为空") UUID groupId,
        @NotBlank(message = "名称不能为空") @Size(max = 64, message = "名称过长") String name,
        @NotBlank(message = "URL 不能为空") @Size(max = 2048, message = "URL 过长") String url,
        UUID iconAssetId) {
}
