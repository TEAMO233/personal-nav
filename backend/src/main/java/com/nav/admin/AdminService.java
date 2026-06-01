package com.nav.admin;

import com.nav.admin.dto.CreateUserRequest;
import com.nav.admin.dto.InviteCodeResponse;
import com.nav.auth.dto.UserResponse;
import com.nav.common.error.ApiException;
import com.nav.engine.EngineService;
import com.nav.user.InviteCode;
import com.nav.user.InviteCodeRepository;
import com.nav.user.Role;
import com.nav.user.User;
import com.nav.user.UserRepository;
import com.nav.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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

    public AdminService(UserRepository userRepository, InviteCodeRepository inviteCodeRepository,
                        PasswordEncoder passwordEncoder, EngineService engineService) {
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.engineService = engineService;
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
        // 3. 为新用户初始化预置引擎(同一事务,与开户一起落库)
        engineService.initPresetEngines(saved.getId());
        // 4. 返回
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole().name());
    }

    /**
     * 重置指定用户密码。
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
}
