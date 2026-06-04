package com.nav.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 更新待办请求体。
 *
 * @param title       标题
 * @param tag         标签(可空)
 * @param scheduledAt 计划时间(可空)
 * @param done        是否完成
 */
public record UpdateTodoRequest(
        @NotBlank(message = "待办标题不能为空") @Size(max = 160, message = "待办标题过长") String title,
        @Size(max = 32, message = "标签过长") String tag,
        Instant scheduledAt,
        Boolean done) {
}
