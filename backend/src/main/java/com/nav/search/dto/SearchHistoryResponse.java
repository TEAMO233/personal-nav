package com.nav.search.dto;

import com.nav.search.SearchHistory;

import java.time.Instant;
import java.util.UUID;

/**
 * 搜索历史项响应。
 *
 * @param id         记录 id
 * @param keyword    搜索关键词
 * @param searchedAt 最近搜索时间
 */
public record SearchHistoryResponse(UUID id, String keyword, Instant searchedAt) {

    /**
     * 由实体转响应。
     *
     * @param h 搜索历史实体
     * @return 响应
     */
    public static SearchHistoryResponse from(SearchHistory h) {
        return new SearchHistoryResponse(h.getId(), h.getKeyword(), h.getSearchedAt());
    }
}
