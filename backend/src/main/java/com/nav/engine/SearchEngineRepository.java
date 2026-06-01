package com.nav.engine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 搜索引擎数据访问。
 */
public interface SearchEngineRepository extends JpaRepository<SearchEngine, UUID> {
}
