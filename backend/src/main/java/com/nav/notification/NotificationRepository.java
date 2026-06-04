package com.nav.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 通知数据访问。
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 按用户列出未解决通知。
     *
     * @param userId   用户 id
     * @param pageable 分页
     * @return 通知列表
     */
    List<Notification> findByUserIdAndResolvedFalseOrderByNotifiedAtDesc(UUID userId, Pageable pageable);

    /**
     * 统计未读且未解决通知数量。
     *
     * @param userId 用户 id
     * @return 未读数量
     */
    long countByUserIdAndResolvedFalseAndReadFalse(UUID userId);

    /**
     * 查找某来源通知。
     *
     * @param userId   用户 id
     * @param type     通知类型
     * @param sourceId 来源 id
     * @return 通知,可能为空
     */
    Optional<Notification> findByUserIdAndTypeAndSourceId(UUID userId, NotificationType type, UUID sourceId);

    /**
     * 按 id 取通知并校验归属。
     *
     * @param id     通知 id
     * @param userId 用户 id
     * @return 通知,可能为空
     */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 取当前用户全部未解决通知。
     *
     * @param userId 用户 id
     * @return 通知列表
     */
    List<Notification> findByUserIdAndResolvedFalse(UUID userId);
}
