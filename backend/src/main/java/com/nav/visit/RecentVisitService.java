package com.nav.visit;

import com.nav.common.error.ApiException;
import com.nav.shortcut.Shortcut;
import com.nav.shortcut.ShortcutRepository;
import com.nav.visit.dto.RecentVisitResponse;
import com.nav.visit.dto.RecordVisitRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 最近访问业务:记录用户点击导航资源,并按用户隔离展示最近访问列表。
 */
@Service
public class RecentVisitService {

    private final RecentVisitRepository visitRepository;
    private final ShortcutRepository shortcutRepository;

    public RecentVisitService(RecentVisitRepository visitRepository, ShortcutRepository shortcutRepository) {
        this.visitRepository = visitRepository;
        this.shortcutRepository = shortcutRepository;
    }

    /**
     * 列出最近访问。
     *
     * @param userId 用户 id
     * @param limit  最大返回数量
     * @return 最近访问
     */
    @Transactional(readOnly = true)
    public List<RecentVisitResponse> list(UUID userId, Integer limit) {
        // 1. 限制返回数量,默认 6 条
        int size = limit == null ? 6 : Math.max(1, Math.min(limit, 50));
        // 2. 查询并转响应
        return visitRepository.findByUserIdOrderByVisitedAtDesc(userId, PageRequest.of(0, size))
                .stream().map(RecentVisitResponse::from).toList();
    }

    /**
     * 记录一次访问。
     *
     * @param userId  用户 id
     * @param request 记录请求
     * @return 新访问记录
     */
    @Transactional
    public RecentVisitResponse record(UUID userId, RecordVisitRequest request) {
        // 1. 优先用 shortcutId,后端校验归属并取数据库快照
        VisitSnapshot snapshot = request.shortcutId() == null
                ? fromRawRequest(request)
                : fromShortcut(userId, request.shortcutId());
        // 2. 建访问记录
        RecentVisit visit = new RecentVisit();
        visit.setUserId(userId);
        visit.setShortcutId(request.shortcutId());
        visit.setName(snapshot.name());
        visit.setUrl(snapshot.url());
        visit.setDomain(extractDomain(snapshot.url()));
        visit.setVisitedAt(Instant.now());
        // 3. 保存并返回
        return RecentVisitResponse.from(visitRepository.save(visit));
    }

    /**
     * 删除一条访问记录。
     *
     * @param userId 用户 id
     * @param id     记录 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人记录并删除
        RecentVisit visit = visitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECENT_VISIT_NOT_FOUND", "访问记录不存在"));
        visitRepository.delete(visit);
    }

    /**
     * 清空当前用户最近访问。
     *
     * @param userId 用户 id
     */
    @Transactional
    public void clear(UUID userId) {
        // 1. 删除本人全部最近访问
        visitRepository.deleteAllByUserId(userId);
    }

    /**
     * 从快捷方式取访问快照。
     */
    private VisitSnapshot fromShortcut(UUID userId, UUID shortcutId) {
        Shortcut shortcut = shortcutRepository.findByIdAndUserId(shortcutId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHORTCUT_NOT_FOUND", "快捷方式不存在"));
        return new VisitSnapshot(shortcut.getName(), shortcut.getUrl());
    }

    /**
     * 从请求取访问快照。
     */
    private VisitSnapshot fromRawRequest(RecordVisitRequest request) {
        if (!StringUtils.hasText(request.name()) || !StringUtils.hasText(request.url())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VISIT_TARGET_REQUIRED", "访问名称和 URL 不能为空");
        }
        return new VisitSnapshot(request.name().trim(), request.url().trim());
    }

    /**
     * 从 URL 提取域名,解析失败则回退原串。
     */
    private String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (!StringUtils.hasText(host)) {
                return url;
            }
            return host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException ex) {
            return url;
        }
    }

    /**
     * 访问快照。
     */
    private record VisitSnapshot(String name, String url) {
    }
}
