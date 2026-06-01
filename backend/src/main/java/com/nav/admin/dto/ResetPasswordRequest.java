package com.nav.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置密码请求体。
 *
 * @param newPassword 新密码
 */
public record ResetPasswordRequest(
        @NotBlank(message = "密码不能为空") @Size(min = 8, message = "密码至少 8 位") String newPassword) {
}
