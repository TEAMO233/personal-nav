package com.nav.bookmark.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 新建首页书签请求。
 *
 * @param name        名称
 * @param url         目标 URL
 * @param description 简短说明
 * @param iconAssetId 图标媒体 id
 * @param enabled     是否启用
 */
public record CreateHomeBookmarkRequest(
        @NotBlank(message = "书签名称不能为空") @Size(max = 96, message = "书签名称过长") String name,
        @NotBlank(message = "书签链接不能为空") @Size(max = 2048, message = "书签链接过长") String url,
        @Size(max = 160, message = "书签说明过长") String description,
        UUID iconAssetId,
        Boolean enabled) {
}
