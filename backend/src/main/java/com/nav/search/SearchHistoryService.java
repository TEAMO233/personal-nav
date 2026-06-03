package com.nav.search;

import com.nav.common.error.ApiException;
import com.nav.search.dto.SearchHistoryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 搜索历史业务:按用户隔离的记录(去重置顶)、列出、删除与清空。
 */
@Service
public class SearchHistoryService {

    private final SearchHistoryRepository repository;

    public SearchHistoryService(SearchHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 列出当前用户最近的搜索历史。
     *
     * @param userId 用户 id
     * @return 最近搜索历史
     */
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> list(UUID userId) {
        // 1. 取最近 20 条并转响应
        return repository.findTop20ByUserIdOrderBySearchedAtDesc(userId)
                .stream().map(SearchHistoryResponse::from).toList();
    }

    /**
     * 记录一次搜索:同词只存一条,命中则更新搜索时间置顶,否则新建。
     *
     * @param userId  用户 id
     * @param keyword 搜索关键词
     * @return 更新后的最近搜索历史
     */
    @Transactional
    public List<SearchHistoryResponse> record(UUID userId, String keyword) {
        // 1. 去首尾空白;空词不记录,直接返回当前列表
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return list(userId);
        }
        // 2. 命中同词则复用并更新时间(置顶),否则新建一条
        SearchHistory h = repository.findByUserIdAndKeyword(userId, kw).orElseGet(() -> {
            SearchHistory created = new SearchHistory();
            created.setUserId(userId);
            created.setKeyword(kw);
            return created;
        });
        h.setSearchedAt(Instant.now());
        repository.save(h);
        // 3. 返回更新后的最近列表
        return list(userId);
    }

    /**
     * 删除当前用户的一条搜索历史,不存在或越权均 404。
     *
     * @param userId 用户 id
     * @param id     记录 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人记录,不存在或越权当作不存在(不暴露存在性)
        SearchHistory h = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SEARCH_HISTORY_NOT_FOUND", "搜索历史不存在"));
        // 2. 删除
        repository.delete(h);
    }

    /**
     * 清空当前用户的全部搜索历史。
     *
     * @param userId 用户 id
     */
    @Transactional
    public void clear(UUID userId) {
        // 1. 删除该用户全部历史
        repository.deleteAllByUserId(userId);
    }
}
