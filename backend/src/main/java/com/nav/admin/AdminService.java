package com.nav.admin;

import com.nav.admin.dto.AdminUserResponse;
import com.nav.admin.dto.CreateUserRequest;
import com.nav.admin.dto.InviteCodeResponse;
import com.nav.auth.dto.UserResponse;
import com.nav.common.error.ApiException;
import com.nav.engine.EngineService;
import com.nav.shortcut.ShortcutService;
import com.nav.user.InviteCode;
import com.nav.user.InviteCodeRepository;
import com.nav.user.Role;
import com.nav.user.User;
import com.nav.user.UserRepository;
import com.nav.user.UserStatus;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 管理后台业务:开户、重置密码、签发邀请码。
 */
@Service
public class AdminService {

    /** 默认邀请码有效天数 */
    private static final int DEFAULT_INVITE_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EngineService engineService;
    private final ShortcutService shortcutService;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public AdminService(UserRepository userRepository, InviteCodeRepository inviteCodeRepository,
                        PasswordEncoder passwordEncoder, EngineService engineService,
                        ShortcutService shortcutService,
                        FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.engineService = engineService;
        this.shortcutService = shortcutService;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 管理员开户。
     *
     * @param request 开户请求
     * @return 新建用户信息
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 1. 用户名占用校验
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "用户名已存在");
        }
        // 2. 建用户(密码哈希存储,角色留空默认普通用户)
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? Role.USER : request.role());
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        // 3. 为新用户初始化预置引擎与首页精选入口(同一事务,与开户一起落库)
        engineService.initPresetEngines(saved.getId());
        shortcutService.initPresetShortcuts(saved.getId());
        // 4. 返回
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole().name());
    }

    /**
     * 列出全部用户(按创建时间升序),供管理后台展示并定位重置密码目标。
     *
     * @return 用户列表
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        // 1. 按创建时间升序查全部用户,逐个转成响应
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getRole().name(),
                        u.getStatus().name(), u.getCreatedAt()))
                .toList();
    }

    /**
     * 重置指定用户密码,并立即失效其全部会话,强制用新密码重新登录。
     *
     * @param userId      用户 id
     * @param newPassword 新密码
     */
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        // 1. 查用户,不存在抛 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在"));
        // 2. 更新密码哈希
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // 3. 失效该用户全部会话(即时注销,旧会话立即作废)
        invalidateUserSessions(user.getUsername());
    }

    /**
     * 启用或禁用用户;禁用后立即失效其全部会话,把已登录的踢下线。
     *
     * @param adminId 当前操作的管理员 id
     * @param userId  目标用户 id
     * @param status  目标状态(ACTIVE 启用 / DISABLED 禁用)
     */
    @Transactional
    public void updateStatus(UUID adminId, UUID userId, UserStatus status) {
        // 1. 不允许管理员禁用自己,避免把自己锁在系统外
        if (status == UserStatus.DISABLED && userId.equals(adminId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DISABLE_SELF", "不能禁用当前登录的管理员自己");
        }
        // 2. 查用户,不存在抛 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在"));
        // 3. 更新状态
        user.setStatus(status);
        userRepository.save(user);
        // 4. 禁用后立即失效该用户全部会话(已登录的被踢下线)
        if (status == UserStatus.DISABLED) {
            invalidateUserSessions(user.getUsername());
        }
    }

    /**
     * 签发邀请码。
     *
     * @param adminId       签发的管理员 id
     * @param expiresInDays 有效天数,留空用默认
     * @return 邀请码信息
     */
    @Transactional
    public InviteCodeResponse createInviteCode(UUID adminId, Integer expiresInDays) {
        // 1. 生成随机邀请码
        String code = generateCode();
        // 2. 计算过期时间
        int days = expiresInDays == null ? DEFAULT_INVITE_DAYS : expiresInDays;
        Instant expiresAt = Instant.now().plus(days, ChronoUnit.DAYS);
        // 3. 落库
        InviteCode invite = new InviteCode();
        invite.setCode(code);
        invite.setCreatedBy(adminId);
        invite.setExpiresAt(expiresAt);
        InviteCode saved = inviteCodeRepository.save(invite);
        // 4. 返回
        return new InviteCodeResponse(saved.getCode(), saved.getExpiresAt());
    }

    /**
     * 生成 URL 安全的随机邀请码。
     */
    private String generateCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 删除某用户在 Redis 里的全部会话(即时注销)。
     *
     * @param username 用户名(Spring Session 按它索引会话)
     */
    private void invalidateUserSessions(String username) {
        // 1. 按用户名找出其全部会话 id 并逐个删除
        sessionRepository.findByPrincipalName(username).keySet()
                .forEach(sessionRepository::deleteById);
    }
}
