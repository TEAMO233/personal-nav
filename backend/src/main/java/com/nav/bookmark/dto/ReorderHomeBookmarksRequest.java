package com.nav.bookmark.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * 首页书签排序请求。
 *
 * @param orderedIds 有序书签 id 列表
 */
public record ReorderHomeBookmarksRequest(
        @NotEmpty(message = "排序列表不能为空") List<UUID> orderedIds) {
}
