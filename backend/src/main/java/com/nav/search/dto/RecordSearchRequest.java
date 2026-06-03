package com.nav.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 记录一次搜索的请求。
 *
 * @param keyword 搜索关键词
 */
public record RecordSearchRequest(@NotBlank @Size(max = 256) String keyword) {
}
