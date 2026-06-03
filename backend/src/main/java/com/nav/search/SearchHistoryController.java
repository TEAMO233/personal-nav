package com.nav.search;

import com.nav.search.dto.RecordSearchRequest;
import com.nav.search.dto.SearchHistoryResponse;
import com.nav.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 搜索历史接口:按当前登录用户隔离的列出、记录、删除与清空。
 */
@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    public SearchHistoryController(SearchHistoryService searchHistoryService) {
        this.searchHistoryService = searchHistoryService;
    }

    /**
     * 列出当前用户最近的搜索历史。
     *
     * @return 最近搜索历史
     */
    @GetMapping
    public List<SearchHistoryResponse> list() {
        // 1. 取当前用户搜索历史
        return searchHistoryService.list(SecurityUtils.currentUserId());
    }

    /**
     * 记录一次搜索(同词去重置顶)。
     *
     * @param request 关键词
     * @return 更新后的最近搜索历史
     */
    @PostMapping
    public List<SearchHistoryResponse> record(@Valid @RequestBody RecordSearchRequest request) {
        // 1. 为当前用户记录搜索
        return searchHistoryService.record(SecurityUtils.currentUserId(), request.keyword());
    }

    /**
     * 删除一条搜索历史。
     *
     * @param id 记录 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户的该条历史
        searchHistoryService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 清空当前用户的全部搜索历史。
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        // 1. 清空当前用户全部历史
        searchHistoryService.clear(SecurityUtils.currentUserId());
    }
}
