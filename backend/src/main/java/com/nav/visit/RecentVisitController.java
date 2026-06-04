package com.nav.visit;

import com.nav.security.SecurityUtils;
import com.nav.visit.dto.RecentVisitResponse;
import com.nav.visit.dto.RecordVisitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 最近访问接口:记录与展示用户点击过的导航资源。
 */
@RestController
@RequestMapping("/api/recent-visits")
public class RecentVisitController {

    private final RecentVisitService visitService;

    public RecentVisitController(RecentVisitService visitService) {
        this.visitService = visitService;
    }

    /**
     * 列出最近访问。
     *
     * @param limit 最大返回数量
     * @return 最近访问列表
     */
    @GetMapping
    public List<RecentVisitResponse> list(@RequestParam(required = false) Integer limit) {
        // 1. 取当前用户最近访问
        return visitService.list(SecurityUtils.currentUserId(), limit);
    }

    /**
     * 记录一次访问。
     *
     * @param request 记录请求
     * @return 新访问记录
     */
    @PostMapping
    public RecentVisitResponse record(@Valid @RequestBody RecordVisitRequest request) {
        // 1. 为当前用户记录访问
        return visitService.record(SecurityUtils.currentUserId(), request);
    }

    /**
     * 删除一条最近访问。
     *
     * @param id 访问记录 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户的访问记录
        visitService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 清空当前用户最近访问。
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        // 1. 清空当前用户全部访问记录
        visitService.clear(SecurityUtils.currentUserId());
    }
}
