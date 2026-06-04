package com.nav.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 首页书签数据访问。
 */
public interface HomeBookmarkRepository extends JpaRepository<HomeBookmark, UUID> {

    /**
     * 按用户取全部首页书签。
     *
     * @param userId 用户 id
     * @return 书签列表
     */
    List<HomeBookmark> findByUserIdOrderBySortOrderAscCreatedAtDesc(UUID userId);

    /**
     * 按用户取启用中的首页书签。
     *
     * @param userId 用户 id
     * @return 启用书签列表
     */
    List<HomeBookmark> findByUserIdAndEnabledTrueOrderBySortOrderAscCreatedAtDesc(UUID userId);

    /**
     * 按用户取全部首页书签,用于排序校验。
     *
     * @param userId 用户 id
     * @return 书签列表
     */
    List<HomeBookmark> findByUserId(UUID userId);

    /**
     * 按 id 取书签并校验归属。
     *
     * @param id     书签 id
     * @param userId 用户 id
     * @return 书签,可能为空
     */
    Optional<HomeBookmark> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计当前用户书签数。
     *
     * @param userId 用户 id
     * @return 书签数
     */
    long countByUserId(UUID userId);
}
