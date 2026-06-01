package com.nav.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求相关工具。
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * 取客户端真实 IP。
     * 真实 IP 由可信反向代理经 server.forward-headers-strategy 统一解析后写入连接地址,
     * 应用层不裸读 X-Forwarded-For,避免该头被伪造绕过限流;生产须部署在可信代理之后。
     *
     * @param request 请求
     * @return 客户端 IP
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
