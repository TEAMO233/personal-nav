package com.nav.common.error;

/**
 * 统一错误响应结构。
 *
 * @param code    业务错误码
 * @param message 给前端展示的错误信息
 */
public record ApiError(String code, String message) {
}
