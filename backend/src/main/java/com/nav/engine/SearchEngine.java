package com.nav.engine;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 搜索引擎实体,对应 search_engines 表,按用户隔离。
 */
@Entity
@Table(name = "search_engines")
public class SearchEngine extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 引擎名称 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 搜索 URL 模板,含查询占位 {query} */
    @Column(name = "url_template", nullable = false, length = 1024)
    private String urlTemplate;

    /** 图标媒体资源 id,可空(预置引擎用内置图标) */
    @Column(name = "icon_asset_id")
    private UUID iconAssetId;

    /** 内置图标 key,预置引擎用(如 google);自定义引擎为空,改用 iconAssetId */
    @Column(name = "icon_builtin", length = 64)
    private String iconBuiltin;

    /** 是否为默认引擎 */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** 排序值 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /** 是否为系统预置引擎 */
    @Column(name = "is_preset", nullable = false)
    private boolean isPreset = false;

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

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public UUID getIconAssetId() {
        return iconAssetId;
    }

    public void setIconAssetId(UUID iconAssetId) {
        this.iconAssetId = iconAssetId;
    }

    public String getIconBuiltin() {
        return iconBuiltin;
    }

    public void setIconBuiltin(String iconBuiltin) {
        this.iconBuiltin = iconBuiltin;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isPreset() {
        return isPreset;
    }

    public void setPreset(boolean isPreset) {
        this.isPreset = isPreset;
    }
}
