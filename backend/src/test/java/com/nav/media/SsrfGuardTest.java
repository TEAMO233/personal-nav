package com.nav.media;

import com.nav.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SSRF 防护单元测试:验证内网地址被拒、公网放行、环回开关生效。
 * 用 IP 字面量避免依赖真实 DNS,不发任何网络请求。
 */
class SsrfGuardTest {

    /**
     * 默认(不放行环回)时,环回、私网、链路本地、通配地址,以及非 http(s) 协议都被拒。
     */
    @Test
    void blocksInternalAndNonHttp() {
        SsrfGuard guard = new SsrfGuard(false);
        // 1. 环回
        assertBlocked(guard, "http://127.0.0.1/a");
        // 2. 私网三段
        assertBlocked(guard, "http://10.0.0.1/");
        assertBlocked(guard, "http://192.168.1.1/");
        assertBlocked(guard, "http://172.16.0.1/");
        // 3. 链路本地(含云元数据地址 169.254.169.254)
        assertBlocked(guard, "http://169.254.169.254/latest/meta-data/");
        // 4. 通配地址
        assertBlocked(guard, "http://0.0.0.0/");
        // 5. 非 http/https 协议
        assertBlocked(guard, "file:///etc/passwd");
        assertBlocked(guard, "ftp://example.com/");
    }

    /**
     * 公网地址放行(用公共 DNS 的 IP,不查 DNS)。
     */
    @Test
    void allowsPublicAddress() {
        SsrfGuard guard = new SsrfGuard(false);
        // 1. 公网 IP 放行
        assertThatCode(() -> guard.check(URI.create("https://1.1.1.1/"))).doesNotThrowAnyException();
        assertThatCode(() -> guard.check(URI.create("http://8.8.8.8/"))).doesNotThrowAnyException();
    }

    /**
     * 放行环回开关打开后,环回地址允许,但私网仍被拒。
     */
    @Test
    void loopbackToggle() {
        SsrfGuard guard = new SsrfGuard(true);
        // 1. 环回放行
        assertThatCode(() -> guard.check(URI.create("http://127.0.0.1/"))).doesNotThrowAnyException();
        // 2. 私网仍拒
        assertBlocked(guard, "http://10.0.0.1/");
    }

    /**
     * 断言某地址被 SsrfGuard 拦截(抛 ApiException)。
     */
    private void assertBlocked(SsrfGuard guard, String url) {
        assertThatThrownBy(() -> guard.check(URI.create(url))).isInstanceOf(ApiException.class);
    }
}
