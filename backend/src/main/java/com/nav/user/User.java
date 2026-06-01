package com.nav.user;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 用户实体,对应 users 表。
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** 登录用户名,全局唯一 */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 密码哈希,不存明文 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** 角色:USER 或 ADMIN */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    /** 账户状态:ACTIVE 或 DISABLED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
