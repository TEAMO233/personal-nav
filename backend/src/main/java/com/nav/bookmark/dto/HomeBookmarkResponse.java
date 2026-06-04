package com.nav.bookmark.dto;

import com.nav.bookmark.HomeBookmark;

import java.util.UUID;

/**
 * 首页书签响应。
 *
 * @param id          书签 id
 * @param name        名称
 * @param url         目标 URL
 * @param description 简短说明
 * @param iconAssetId 图标媒体 id
 * @param enabled     是否启用
 * @param sortOrder   排序值
 */
public record HomeBookmarkResponse(UUID id, String name, String url, String description,
                                   UUID iconAssetId, boolean enabled, int sortOrder) {

    /**
     * 由实体转响应。
     *
     * @param bookmark 首页书签实体
     * @return 响应
     */
    public static HomeBookmarkResponse from(HomeBookmark bookmark) {
        return new HomeBookmarkResponse(bookmark.getId(), bookmark.getName(), bookmark.getUrl(),
                bookmark.getDescription(), bookmark.getIconAssetId(), bookmark.isEnabled(), bookmark.getSortOrder());
    }
}
