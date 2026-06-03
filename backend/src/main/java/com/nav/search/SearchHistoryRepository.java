package com.nav.search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 搜索历史数据访问。
 */
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    /**
     * 取某用户最近 20 条搜索历史(按搜索时间倒序)。
     *
     * @param userId 用户 id
     * @return 最近的搜索历史
     */
    List<SearchHistory> findTop20ByUserIdOrderBySearchedAtDesc(UUID userId);

    /**
     * 按用户与关键词查记录,用于去重(同词只存一条)。
     *
     * @param userId  用户 id
     * @param keyword 关键词
     * @return 命中的记录(可空)
     */
    Optional<SearchHistory> findByUserIdAndKeyword(UUID userId, String keyword);

    /**
     * 按 id 与归属用户查记录,用于删除时校验归属。
     *
     * @param id     记录 id
     * @param userId 归属用户 id
     * @return 命中的记录(可空)
     */
    Optional<SearchHistory> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 清空某用户的全部搜索历史。
     *
     * @param userId 用户 id
     */
    @Modifying
    @Query("delete from SearchHistory h where h.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
