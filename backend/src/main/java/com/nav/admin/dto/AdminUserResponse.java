package com.nav.admin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 管理后台用户列表项响应。
 * 比 UserResponse 多了状态与创建时间,供后台表格展示并定位重置密码目标。
 *
 * @param id        用户 id
 * @param username  用户名
 * @param role      角色(USER / ADMIN)
 * @param status    账户状态(ACTIVE / DISABLED)
 * @param createdAt 创建时间
 */
public record AdminUserResponse(UUID id, String username, String role, String status, Instant createdAt) {
}
