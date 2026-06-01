package com.nav.admin.dto;

import java.time.Instant;

/**
 * 签发邀请码响应。
 *
 * @param code      邀请码
 * @param expiresAt 过期时间
 */
public record InviteCodeResponse(String code, Instant expiresAt) {
}
