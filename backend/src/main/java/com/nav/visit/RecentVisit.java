package com.nav.visit;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 最近访问实体,记录用户点击导航资源的访问快照。
 */
@Entity
@Table(name = "recent_visits")
public class RecentVisit extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 关联快捷方式 id,可空 */
    @Column(name = "shortcut_id")
    private UUID shortcutId;

    /** 访问时的名称快照 */
    @Column(nullable = false, length = 96)
    private String name;

    /** 访问时的 URL 快照 */
    @Column(nullable = false, length = 2048)
    private String url;

    /** 域名快照 */
    @Column(nullable = false, length = 255)
    private String domain;

    /** 访问时间 */
    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getShortcutId() {
        return shortcutId;
    }

    public void setShortcutId(UUID shortcutId) {
        this.shortcutId = shortcutId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Instant visitedAt) {
        this.visitedAt = visitedAt;
    }
}
