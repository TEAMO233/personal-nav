package com.nav.admin;

import com.nav.admin.dto.AdminUserResponse;
import com.nav.admin.dto.CreateInviteCodeRequest;
import com.nav.admin.dto.CreateUserRequest;
import com.nav.admin.dto.InviteCodeResponse;
import com.nav.admin.dto.ResetPasswordRequest;
import com.nav.auth.dto.UserResponse;
import com.nav.common.RequestUtils;
import com.nav.security.RedisRateLimiter;
import com.nav.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 管理后台接口(仅 ADMIN 可访问,鉴权由 SecurityConfig 的 /api/admin/** 规则保证)。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 开户限流:同一 IP 1 小时内最多 20 次
    private static final int CREATE_USER_MAX = 20;
    private static final Duration CREATE_USER_WINDOW = Duration.ofHours(1);

    private final AdminService adminService;
    private final RedisRateLimiter rateLimiter;

    public AdminController(AdminService adminService, RedisRateLimiter rateLimiter) {
        this.adminService = adminService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 开户。
     *
     * @param request     开户请求
     * @param httpRequest 原始请求
     * @return 新建用户信息
     */
    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        // 1. 按 IP 限流
        rateLimiter.checkLimit("rl:admin-user:" + RequestUtils.clientIp(httpRequest), CREATE_USER_MAX, CREATE_USER_WINDOW);
        // 2. 开户
        return adminService.createUser(request);
    }

    /**
     * 列出全部用户,供管理后台展示并定位重置密码目标。
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        // 1. 委托服务查全部用户
        return adminService.listUsers();
    }

    /**
     * 重置指定用户密码。
     *
     * @param id      用户 id
     * @param request 新密码
     */
    @PostMapping("/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request.newPassword());
    }

    /**
     * 签发邀请码。
     *
     * @param request 有效期请求,可不传
     * @return 邀请码信息
     */
    @PostMapping("/invite-codes")
    public InviteCodeResponse createInviteCode(@RequestBody(required = false) CreateInviteCodeRequest request) {
        // 1. 取当前管理员 id 作为签发者
        UUID adminId = SecurityUtils.currentUserId();
        // 2. 解析有效期并签发
        Integer days = request == null ? null : request.expiresInDays();
        return adminService.createInviteCode(adminId, days);
    }
}
