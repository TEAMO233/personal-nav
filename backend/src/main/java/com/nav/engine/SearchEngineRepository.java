package com.nav.engine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 搜索引擎数据访问。
 */
public interface SearchEngineRepository extends JpaRepository<SearchEngine, UUID> {

    /** 按用户取全部引擎,先按排序值再按创建时间升序 */
    List<SearchEngine> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    /** 按 id + 用户取单个引擎,用于归属校验 */
    Optional<SearchEngine> findByIdAndUserId(UUID id, UUID userId);

    /** 统计某用户的引擎数,用于把新引擎追加到末尾 */
    long countByUserId(UUID userId);
}
