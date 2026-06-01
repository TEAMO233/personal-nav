package com.nav.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新建引擎请求体。
 *
 * @param name        引擎名称
 * @param urlTemplate 搜索 URL 模板,须含查询占位 {query}
 */
public record CreateEngineRequest(
        @NotBlank(message = "引擎名称不能为空") @Size(max = 64, message = "引擎名称过长") String name,
        @NotBlank(message = "URL 模板不能为空") @Size(max = 1024, message = "URL 模板过长") String urlTemplate) {
}
