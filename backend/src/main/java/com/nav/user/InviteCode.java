package com.nav.user;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 邀请码实体,对应 invite_codes 表。
 */
@Entity
@Table(name = "invite_codes")
public class InviteCode extends BaseEntity {

    /** 邀请码字符串,全局唯一 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 签发者(管理员)用户 id */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /** 消费者用户 id,未使用时为空 */
    @Column(name = "used_by")
    private UUID usedBy;

    /** 过期时间,为空表示永不过期 */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** 被使用的时间,未使用时为空 */
    @Column(name = "used_at")
    private Instant usedAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(UUID usedBy) {
        this.usedBy = usedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
