package com.nav.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * 所有实体的公共基类。
 * 统一主键(UUID,保存时生成)和创建时间,子类只声明自己的业务字段。
 */
@MappedSuperclass
public abstract class BaseEntity {

    /** 主键 UUID,保存时由 Hibernate 生成,数据库侧另有 gen_random_uuid() 兜底 */
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** 创建时间,保存时自动填充 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
