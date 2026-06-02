package com.nav.shortcut.dto;

import com.nav.shortcut.Shortcut;

import java.util.UUID;

/**
 * 快捷方式对外响应。
 *
 * @param id          快捷方式 id
 * @param groupId     所属分组 id
 * @param name        名称
 * @param url         目标 URL
 * @param iconAssetId 图标媒体 id(可空)
 * @param sortOrder   组内排序值
 */
public record ShortcutResponse(UUID id, UUID groupId, String name, String url, UUID iconAssetId, int sortOrder) {

    /**
     * 由实体转对外响应。
     *
     * @param s 快捷方式实体
     * @return 响应
     */
    public static ShortcutResponse from(Shortcut s) {
        return new ShortcutResponse(s.getId(), s.getGroupId(), s.getName(), s.getUrl(), s.getIconAssetId(), s.getSortOrder());
    }
}
