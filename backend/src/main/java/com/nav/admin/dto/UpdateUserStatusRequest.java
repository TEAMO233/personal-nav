package com.nav.admin.dto;

import com.nav.user.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 启用 / 禁用用户请求。
 *
 * @param status 目标状态(ACTIVE 启用 / DISABLED 禁用)
 */
public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
