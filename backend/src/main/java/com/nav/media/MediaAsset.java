package com.nav.media;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 媒体资源实体,对应 media_assets 表,记录图标等文件的存储信息。
 */
@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 来源类型:UPLOAD / FAVICON / URL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaType type;

    /** 存储键,定位到具体文件 */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /** 原始来源 URL,仅 favicon/url 类型有 */
    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    /** 文件 MIME 类型 */
    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
