package com.nav.visit.dto;

import com.nav.visit.RecentVisit;

import java.time.Instant;
import java.util.UUID;

/**
 * 最近访问响应。
 *
 * @param id         记录 id
 * @param shortcutId 快捷方式 id
 * @param name       名称
 * @param url        URL
 * @param domain     域名
 * @param visitedAt  访问时间
 */
public record RecentVisitResponse(UUID id, UUID shortcutId, String name, String url, String domain, Instant visitedAt) {

    /**
     * 由实体转响应。
     *
     * @param visit 最近访问实体
     * @return 响应
     */
    public static RecentVisitResponse from(RecentVisit visit) {
        return new RecentVisitResponse(visit.getId(), visit.getShortcutId(), visit.getName(),
                visit.getUrl(), visit.getDomain(), visit.getVisitedAt());
    }
}
