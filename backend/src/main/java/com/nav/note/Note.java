package com.nav.note;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 灵感便签实体,用于首页便签小组件。
 */
@Entity
@Table(name = "notes")
public class Note extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 便签内容 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 是否置顶 / 星标 */
    @Column(nullable = false)
    private boolean pinned = false;

    /** 排序值 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
