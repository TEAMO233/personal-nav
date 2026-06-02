package com.nav.shortcut;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 快捷方式分组数据访问。
 */
public interface ShortcutGroupRepository extends JpaRepository<ShortcutGroup, UUID> {

    /**
     * 按用户取全部分组,排序值升序、创建时间升序。
     *
     * @param userId 用户 id
     * @return 分组列表
     */
    List<ShortcutGroup> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    /**
     * 按 id 取分组并校验归属,不属于该用户则返回空。
     *
     * @param id     分组 id
     * @param userId 用户 id
     * @return 分组,可能为空
     */
    Optional<ShortcutGroup> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计某用户的分组数,用于新建分组时排到末尾。
     *
     * @param userId 用户 id
     * @return 分组数
     */
    long countByUserId(UUID userId);
}
