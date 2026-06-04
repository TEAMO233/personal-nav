package com.nav.todo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 待办 / 日程数据访问。
 */
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    /**
     * 按用户查询待办,支持排序。
     *
     * @param userId 用户 id
     * @param sort   排序
     * @return 待办列表
     */
    List<Todo> findByUserId(UUID userId, Sort sort);

    /**
     * 按用户查询待办,支持分页。
     *
     * @param userId   用户 id
     * @param pageable 分页与排序
     * @return 待办列表
     */
    List<Todo> findByUserId(UUID userId, Pageable pageable);

    /**
     * 按 id 取待办并校验归属。
     *
     * @param id     待办 id
     * @param userId 用户 id
     * @return 待办,可能为空
     */
    Optional<Todo> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计当前用户待办数量,用于新建时排到末尾。
     *
     * @param userId 用户 id
     * @return 待办数量
     */
    long countByUserId(UUID userId);

    /**
     * 查找当前用户已逾期且未完成的待办。
     *
     * @param userId 用户 id
     * @param now    当前时间
     * @return 逾期待办
     */
    List<Todo> findByUserIdAndDoneFalseAndScheduledAtBefore(UUID userId, Instant now);
}
