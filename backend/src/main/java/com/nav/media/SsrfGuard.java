package com.nav.media;

import com.nav.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF 防护:抓取远程地址前先校验,只放行公网 http/https,拒绝环回、私网、链路本地等内网地址。
 * 重定向时每一跳都要再校验,防止跳转到内网。
 */
@Component
public class SsrfGuard {

    /** 是否放行环回地址,仅本地测试用,生产保持关闭 */
    private final boolean allowLoopback;

    public SsrfGuard(@Value("${app.media.fetch.allow-loopback:false}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    /**
     * 校验目标地址是否允许抓取,不允许则抛 400。
     *
     * @param uri 目标地址
     */
    public void check(URI uri) {
        // 1. 只允许 http / https
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw blocked("仅支持 http/https 地址");
        }
        // 2. 必须带主机名
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw blocked("地址缺少主机名");
        }
        // 3. 解析主机的所有 IP,任一是内网地址就拒(防域名被指向内网)
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw blocked("无法解析的主机名");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw blocked("禁止访问内网地址");
            }
        }
    }

    /**
     * 判断单个 IP 是否属于需要拦截的内网 / 保留地址。
     */
    private boolean isBlockedAddress(InetAddress address) {
        // 1. 环回(127.x / ::1):按开关决定放不放行(测试时放行)
        if (address.isLoopbackAddress()) {
            return !allowLoopback;
        }
        // 2. 通配、链路本地、站点本地(10/172.16/192.168)、多播一律拒
        if (address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        // 3. 补 IPv4 运营商级 NAT 段 100.64.0.0/10
        byte[] ip = address.getAddress();
        if (ip.length == 4) {
            int b0 = ip[0] & 0xFF;
            int b1 = ip[1] & 0xFF;
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
        }
        // 4. 补 IPv6 唯一本地地址 fc00::/7
        if (ip.length == 16 && (ip[0] & 0xFE) == 0xFC) {
            return true;
        }
        // 5. 其余视为公网,放行
        return false;
    }

    /**
     * 构造一个「地址被拦截」的 400 异常。
     */
    private ApiException blocked(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "MEDIA_URL_BLOCKED", message);
    }
}
