package com.nav.notification.dto;

import com.nav.notification.Notification;

import java.time.Instant;
import java.util.UUID;

/**
 * 通知响应。
 *
 * @param id         通知 id
 * @param type       类型
 * @param sourceId   来源 id
 * @param title      标题
 * @param content    内容
 * @param read       是否已读
 * @param resolved   是否已解决
 * @param notifiedAt 通知时间
 */
public record NotificationResponse(UUID id, String type, UUID sourceId, String title, String content,
                                   boolean read, boolean resolved, Instant notifiedAt) {

    /**
     * 由实体转响应。
     *
     * @param notification 通知实体
     * @return 响应
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType().name(), notification.getSourceId(),
                notification.getTitle(), notification.getContent(), notification.isRead(),
                notification.isResolved(), notification.getNotifiedAt());
    }
}
