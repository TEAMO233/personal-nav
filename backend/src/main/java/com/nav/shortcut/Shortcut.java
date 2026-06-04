package com.nav.shortcut;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 快捷方式实体,对应 shortcuts 表,归属某个分组,按用户隔离。
 */
@Entity
@Table(name = "shortcuts")
public class Shortcut extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 所属分组 id */
    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    /** 名称 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 目标 URL */
    @Column(nullable = false, length = 2048)
    private String url;

    /** 图标媒体资源 id,可空 */
    @Column(name = "icon_asset_id")
    private UUID iconAssetId;

    /** 首页卡片描述,可空 */
    @Column(length = 160)
    private String description;

    /** 内置线性图标 key,用于首页精选卡片,可空 */
    @Column(name = "icon_key", length = 32)
    private String iconKey;

    /** 强调色 key,用于首页精选卡片,可空 */
    @Column(length = 32)
    private String accent;

    /** 是否在首页顶部精选入口展示 */
    @Column(nullable = false)
    private boolean featured = false;

    /** 首页精选入口排序值 */
    @Column(name = "featured_order", nullable = false)
    private int featuredOrder = 0;

    /** 排序值 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
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

    public UUID getIconAssetId() {
        return iconAssetId;
    }

    public void setIconAssetId(UUID iconAssetId) {
        this.iconAssetId = iconAssetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public int getFeaturedOrder() {
        return featuredOrder;
    }

    public void setFeaturedOrder(int featuredOrder) {
        this.featuredOrder = featuredOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
