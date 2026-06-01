package com.nav.admin.dto;

import com.nav.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员开户请求体。
 *
 * @param username 用户名
 * @param password 初始密码
 * @param role     角色,留空默认普通用户
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 8, message = "密码至少 8 位") String password,
        Role role) {
}
