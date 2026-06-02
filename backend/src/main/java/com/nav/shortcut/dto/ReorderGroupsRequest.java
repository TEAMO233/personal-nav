package com.nav.shortcut.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * 分组排序请求体:orderedIds 须为当前用户全部分组 id 的一个排列,按其顺序重排。
 *
 * @param orderedIds 有序分组 id 列表
 */
public record ReorderGroupsRequest(
        @NotEmpty(message = "排序列表不能为空") List<UUID> orderedIds) {
}
