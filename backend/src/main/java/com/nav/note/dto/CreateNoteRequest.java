package com.nav.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新建便签请求。
 *
 * @param content 内容
 * @param pinned  是否置顶
 */
public record CreateNoteRequest(
        @NotBlank(message = "便签内容不能为空") @Size(max = 1000, message = "便签内容过长") String content,
        Boolean pinned) {
}
