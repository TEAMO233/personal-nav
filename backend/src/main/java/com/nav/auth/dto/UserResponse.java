package com.nav.auth.dto;

import java.util.UUID;

/**
 * 用户信息响应。
 *
 * @param id       用户 id
 * @param username 用户名
 * @param role     角色
 */
public record UserResponse(UUID id, String username, String role) {
}
