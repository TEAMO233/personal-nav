package com.nav.todo;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 待办 / 日程实体,用于首页内联管理和今日完成率计算。
 */
@Entity
@Table(name = "todos")
public class Todo extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 待办标题 */
    @Column(nullable = false, length = 160)
    private String title;

    /** 标签,如 工作 / 学习 */
    @Column(length = 32)
    private String tag;

    /** 计划时间,可空;有值时同时可作为日程展示 */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    /** 是否已完成 */
    @Column(nullable = false)
    private boolean done = false;

    /** 排序值 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
