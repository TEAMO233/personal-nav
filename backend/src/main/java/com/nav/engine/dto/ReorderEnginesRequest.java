package com.nav.engine.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * 引擎排序请求体:orderedIds 须为当前用户全部引擎 id 的一个排列,按其顺序重排。
 *
 * @param orderedIds 有序引擎 id 列表
 */
public record ReorderEnginesRequest(
        @NotEmpty(message = "排序列表不能为空") List<UUID> orderedIds) {
}
