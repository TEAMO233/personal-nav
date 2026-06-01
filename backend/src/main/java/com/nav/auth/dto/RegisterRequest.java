package com.nav.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 邀请码自助注册请求体。
 *
 * @param code     邀请码
 * @param username 用户名
 * @param password 密码
 */
public record RegisterRequest(
        @NotBlank(message = "邀请码不能为空") String code,
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 8, message = "密码至少 8 位") String password) {
}
