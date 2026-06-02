package com.nav.shortcut.dto;

import com.nav.shortcut.ShortcutGroup;

import java.util.UUID;

/**
 * 分组对外响应。
 *
 * @param id        分组 id
 * @param name      分组名称
 * @param sortOrder 排序值
 */
public record GroupResponse(UUID id, String name, int sortOrder) {

    /**
     * 由实体转对外响应。
     *
     * @param g 分组实体
     * @return 响应
     */
    public static GroupResponse from(ShortcutGroup g) {
        return new GroupResponse(g.getId(), g.getName(), g.getSortOrder());
    }
}
