package com.nav.security;

import com.nav.common.error.ApiException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的固定窗口限流:同一个 key 在时间窗口内累计请求数超过上限就拒绝。
 */
@Component
public class RedisRateLimiter {

    // 原子地累加计数,并在首次计数时设置窗口过期时间,避免「加完计数还没设过期就崩溃」导致 key 永不过期
    private static final RedisScript<Long> INCR_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查并累加一次计数,超过上限抛 429。
     *
     * @param key         限流键(通常含动作 + IP + 用户名)
     * @param maxRequests 窗口内允许的最大次数
     * @param window      时间窗口
     */
    public void checkLimit(String key, int maxRequests, Duration window) {
        // 1. 原子累加并在首次设置过期
        Long count = redisTemplate.execute(INCR_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
        // 2. 超过上限则拒绝
        if (count != null && count > maxRequests) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "请求过于频繁,请稍后再试");
        }
    }
}
