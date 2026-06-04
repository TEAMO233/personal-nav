package com.nav.visit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 最近访问数据访问。
 */
public interface RecentVisitRepository extends JpaRepository<RecentVisit, UUID> {

    /**
     * 取当前用户最近访问。
     *
     * @param userId   用户 id
     * @param pageable 分页
     * @return 最近访问列表
     */
    List<RecentVisit> findByUserIdOrderByVisitedAtDesc(UUID userId, Pageable pageable);

    /**
     * 按 id 取最近访问并校验归属。
     *
     * @param id     访问记录 id
     * @param userId 用户 id
     * @return 访问记录,可能为空
     */
    Optional<RecentVisit> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 清空当前用户最近访问。
     *
     * @param userId 用户 id
     */
    void deleteAllByUserId(UUID userId);
}
