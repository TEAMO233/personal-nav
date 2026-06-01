package com.nav.admin.dto;

/**
 * 签发邀请码请求体。
 *
 * @param expiresInDays 有效天数,留空用默认值
 */
public record CreateInviteCodeRequest(Integer expiresInDays) {
}
