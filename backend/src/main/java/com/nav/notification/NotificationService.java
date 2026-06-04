package com.nav.notification;

import com.nav.common.error.ApiException;
import com.nav.notification.dto.NotificationResponse;
import com.nav.notification.dto.UnreadCountResponse;
import com.nav.todo.Todo;
import com.nav.todo.TodoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 通知业务:同步逾期待办通知、列出、未读数与已读状态。
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TodoRepository todoRepository;

    public NotificationService(NotificationRepository notificationRepository, TodoRepository todoRepository) {
        this.notificationRepository = notificationRepository;
        this.todoRepository = todoRepository;
    }

    /**
     * 列出当前用户未解决通知。
     *
     * @param userId 用户 id
     * @param limit  最大数量
     * @return 通知列表
     */
    @Transactional
    public List<NotificationResponse> list(UUID userId, Integer limit) {
        // 1. 先同步逾期待办通知
        syncTodoOverdue(userId);
        // 2. 查询未解决通知
        int size = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
        return notificationRepository.findByUserIdAndResolvedFalseOrderByNotifiedAtDesc(userId, PageRequest.of(0, size))
                .stream().map(NotificationResponse::from).toList();
    }

    /**
     * 获取未读通知数。
     *
     * @param userId 用户 id
     * @return 未读数量
     */
    @Transactional
    public UnreadCountResponse unreadCount(UUID userId) {
        // 1. 先同步逾期待办通知
        syncTodoOverdue(userId);
        // 2. 统计未读且未解决数量
        return new UnreadCountResponse(notificationRepository.countByUserIdAndResolvedFalseAndReadFalse(userId));
    }

    /**
     * 标记单条已读。
     *
     * @param userId 用户 id
     * @param id     通知 id
     * @return 更新后通知
     */
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        // 1. 取本人通知
        Notification notification = requireOwned(userId, id);
        // 2. 标记已读
        notification.setRead(true);
        // 3. 保存并返回
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    /**
     * 标记当前用户全部未解决通知为已读。
     *
     * @param userId 用户 id
     */
    @Transactional
    public void markAllRead(UUID userId) {
        // 1. 同步后取未解决通知
        syncTodoOverdue(userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndResolvedFalse(userId);
        // 2. 批量标记已读
        for (Notification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * 待办完成 / 删除后解决对应逾期通知。
     *
     * @param userId 用户 id
     * @param todoId 待办 id
     */
    @Transactional
    public void resolveTodoOverdue(UUID userId, UUID todoId) {
        // 1. 找到对应通知,存在则设为已读且已解决
        notificationRepository.findByUserIdAndTypeAndSourceId(userId, NotificationType.TODO_OVERDUE, todoId)
                .ifPresent(n -> {
                    n.setRead(true);
                    n.setResolved(true);
                    notificationRepository.save(n);
                });
    }

    /**
     * 同步当前用户逾期待办为通知。
     */
    private void syncTodoOverdue(UUID userId) {
        // 1. 查找当前逾期且未完成待办
        List<Todo> overdue = todoRepository.findByUserIdAndDoneFalseAndScheduledAtBefore(userId, Instant.now());
        // 2. 为每个逾期待办生成或重新激活通知
        for (Todo todo : overdue) {
            Notification notification = notificationRepository
                    .findByUserIdAndTypeAndSourceId(userId, NotificationType.TODO_OVERDUE, todo.getId())
                    .orElseGet(() -> {
                        Notification created = new Notification();
                        created.setUserId(userId);
                        created.setType(NotificationType.TODO_OVERDUE);
                        created.setSourceId(todo.getId());
                        created.setRead(false);
                        created.setNotifiedAt(Instant.now());
                        return created;
                    });
            if (!notification.isResolved()) {
                notification.setTitle("待办已超时");
                notification.setContent(todo.getTitle());
            } else {
                notification.setResolved(false);
                notification.setRead(false);
                notification.setNotifiedAt(Instant.now());
                notification.setTitle("待办已超时");
                notification.setContent(todo.getTitle());
            }
            notificationRepository.save(notification);
        }
    }

    /**
     * 取本人通知,不存在或越权均 404。
     */
    private Notification requireOwned(UUID userId, UUID id) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "通知不存在"));
    }
}
