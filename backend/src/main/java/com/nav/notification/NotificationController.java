package com.nav.notification;

import com.nav.notification.dto.NotificationResponse;
import com.nav.notification.dto.UnreadCountResponse;
import com.nav.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 通知接口:顶栏铃铛未读数与下拉列表。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 列出当前用户未解决通知。
     *
     * @param limit 最大数量
     * @return 通知列表
     */
    @GetMapping
    public List<NotificationResponse> list(@RequestParam(required = false) Integer limit) {
        // 1. 取当前用户通知
        return notificationService.list(SecurityUtils.currentUserId(), limit);
    }

    /**
     * 获取未读通知数。
     *
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        // 1. 统计当前用户未读通知数
        return notificationService.unreadCount(SecurityUtils.currentUserId());
    }

    /**
     * 标记单条通知已读。
     *
     * @param id 通知 id
     * @return 更新后通知
     */
    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        // 1. 标记当前用户该通知已读
        return notificationService.markRead(SecurityUtils.currentUserId(), id);
    }

    /**
     * 全部标记已读。
     */
    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        // 1. 标记当前用户全部通知已读
        notificationService.markAllRead(SecurityUtils.currentUserId());
    }
}
