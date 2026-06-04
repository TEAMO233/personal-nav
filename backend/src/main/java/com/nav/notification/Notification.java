package com.nav.notification;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 通知实体,用于首页顶栏铃铛。
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 通知类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    /** 来源资源 id,如逾期待办 id */
    @Column(name = "source_id")
    private UUID sourceId;

    /** 标题 */
    @Column(nullable = false, length = 160)
    private String title;

    /** 内容 */
    @Column(length = 512)
    private String content;

    /** 是否已读 */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /** 是否已解决;默认列表隐藏已解决通知 */
    @Column(nullable = false)
    private boolean resolved = false;

    /** 通知时间 */
    @Column(name = "notified_at", nullable = false)
    private Instant notifiedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}
