package com.nav.shortcut.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 快捷方式排序请求体:items 须覆盖当前用户的全部快捷方式,每项给出所属分组与组内排序值;
 * 同一请求里把某项的 groupId 改成另一分组即实现跨组移动。
 *
 * @param items 全部快捷方式的新位置列表
 */
public record ReorderShortcutsRequest(
        @NotEmpty(message = "排序列表不能为空") @Valid List<Item> items) {

    /**
     * 单个快捷方式的新位置。
     *
     * @param id        快捷方式 id
     * @param groupId   目标分组 id
     * @param sortOrder 组内排序值
     */
    public record Item(
            @NotNull(message = "快捷方式 id 不能为空") UUID id,
            @NotNull(message = "分组 id 不能为空") UUID groupId,
            int sortOrder) {
    }
}
