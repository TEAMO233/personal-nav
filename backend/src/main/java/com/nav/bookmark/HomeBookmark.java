package com.nav.bookmark;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 首页书签实体,由用户在设置页配置并展示在首页底部书签面板。
 */
@Entity
@Table(name = "home_bookmarks")
public class HomeBookmark extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 书签名称 */
    @Column(nullable = false, length = 96)
    private String name;

    /** 目标 URL */
    @Column(nullable = false, length = 2048)
    private String url;

    /** 简短说明,可空 */
    @Column(length = 160)
    private String description;

    /** 图标媒体资源 id,可空 */
    @Column(name = "icon_asset_id")
    private UUID iconAssetId;

    /** 是否在首页显示 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 首页显示排序 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getIconAssetId() {
        return iconAssetId;
    }

    public void setIconAssetId(UUID iconAssetId) {
        this.iconAssetId = iconAssetId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
