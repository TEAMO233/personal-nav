package com.nav.auth;

import com.nav.auth.dto.LoginRequest;
import com.nav.auth.dto.RegisterRequest;
import com.nav.auth.dto.UserResponse;
import com.nav.common.RequestUtils;
import com.nav.security.CustomUserDetails;
import com.nav.security.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 认证接口:登录、邀请码注册、获取当前用户。登出由 Spring Security 过滤器处理(见 SecurityConfig)。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 登录限流:同一 IP+用户名 5 分钟内最多 10 次(针对单账户暴力破解)
    private static final int LOGIN_MAX = 10;
    // 登录限流:同一 IP 5 分钟内最多 30 次(防单 IP 用海量用户名喷洒 / Redis key 膨胀)
    private static final int LOGIN_IP_MAX = 30;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
    // 注册限流:同一 IP 1 小时内最多 20 次
    private static final int REGISTER_MAX = 20;
    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final RedisRateLimiter rateLimiter;
    // 把登录后的认证信息存进会话(经 Spring Session 落到 Redis)
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    // 登录成功后换发 session id,防会话固定攻击
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy = new ChangeSessionIdAuthenticationStrategy();

    public AuthController(AuthenticationManager authenticationManager, AuthService authService,
                          RedisRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 用户名密码登录,成功后换发会话并下发 Cookie。
     *
     * @param request      登录请求
     * @param httpRequest  原始请求
     * @param httpResponse 原始响应
     * @return 当前用户信息
     */
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. 双维度限流:先按 IP 总量,再按 IP+用户名,防暴力破解与单 IP 喷洒
        String ip = RequestUtils.clientIp(httpRequest);
        rateLimiter.checkLimit("rl:login-ip:" + ip, LOGIN_IP_MAX, LOGIN_WINDOW);
        rateLimiter.checkLimit("rl:login:" + ip + ":" + request.username(), LOGIN_MAX, LOGIN_WINDOW);
        // 2. 校验用户名密码(失败抛 AuthenticationException)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        // 3. 换发 session id,防会话固定
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        // 4. 建立 SecurityContext 并存入会话,持久化到 Redis
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        // 5. 返回当前用户信息
        return toResponse((CustomUserDetails) authentication.getPrincipal());
    }

    /**
     * 用邀请码自助注册。
     *
     * @param request     注册请求
     * @param httpRequest 原始请求
     * @return 新建用户信息
     */
    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // 1. 按 IP 限流,防刷注册
        rateLimiter.checkLimit("rl:register:" + RequestUtils.clientIp(httpRequest), REGISTER_MAX, REGISTER_WINDOW);
        // 2. 校验邀请码并建用户
        return authService.register(request);
    }

    /**
     * 获取当前登录用户信息。
     *
     * @param principal 当前登录主体(未登录已被过滤链拦截为 401)
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        // 1. 直接转换返回
        return toResponse(principal);
    }

    /**
     * 把用户详情转成对外响应。
     */
    private UserResponse toResponse(CustomUserDetails principal) {
        return new UserResponse(principal.getUserId(), principal.getUsername(), principal.getRole().name());
    }
}
