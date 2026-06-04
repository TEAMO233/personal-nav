package com.nav.note.dto;

import com.nav.note.Note;

import java.time.Instant;
import java.util.UUID;

/**
 * 便签响应。
 *
 * @param id        便签 id
 * @param content   内容
 * @param pinned    是否置顶
 * @param sortOrder 排序值
 * @param createdAt 创建时间
 */
public record NoteResponse(UUID id, String content, boolean pinned, int sortOrder, Instant createdAt) {

    /**
     * 由实体转响应。
     *
     * @param note 便签实体
     * @return 响应
     */
    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getContent(), note.isPinned(),
                note.getSortOrder(), note.getCreatedAt());
    }
}
