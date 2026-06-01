package com.nav.security;

import com.nav.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * 安全上下文工具:取当前登录用户信息,供 service 层做多租户隔离。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 取当前登录用户 id,未登录则抛 401。
     *
     * @return 当前用户 id
     */
    public static UUID currentUserId() {
        // 1. 从安全上下文取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 2. 是已登录用户主体则返回其 id
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            return principal.getUserId();
        }
        // 3. 否则视为未登录
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "未登录");
    }
}
