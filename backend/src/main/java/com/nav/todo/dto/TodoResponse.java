package com.nav.todo.dto;

import com.nav.todo.Todo;

import java.time.Instant;
import java.util.UUID;

/**
 * 待办对外响应。
 *
 * @param id          待办 id
 * @param title       标题
 * @param tag         标签
 * @param scheduledAt 计划时间
 * @param done        是否完成
 * @param sortOrder   排序值
 * @param createdAt   创建时间
 */
public record TodoResponse(UUID id, String title, String tag, Instant scheduledAt,
                           boolean done, int sortOrder, Instant createdAt) {

    /**
     * 由实体转响应。
     *
     * @param todo 待办实体
     * @return 响应
     */
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getTitle(), todo.getTag(), todo.getScheduledAt(),
                todo.isDone(), todo.getSortOrder(), todo.getCreatedAt());
    }
}
