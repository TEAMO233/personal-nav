package com.nav.auth;

import com.nav.auth.dto.RegisterRequest;
import com.nav.auth.dto.UserResponse;
import com.nav.common.error.ApiException;
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

import java.time.Instant;

/**
 * 认证业务:邀请码自助注册。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, InviteCodeRepository inviteCodeRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用邀请码自助注册:校验邀请码后建用户并消费该邀请码,单事务保证原子。
     *
     * @param request 注册请求
     * @return 新建用户信息
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 1. 查邀请码,不存在视为无效
        InviteCode invite = inviteCodeRepository.findByCode(request.code())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID", "邀请码无效"));
        // 2. 已被使用则拒绝
        if (invite.getUsedBy() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVITE_CODE_USED", "邀请码已被使用");
        }
        // 3. 已过期则拒绝
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVITE_CODE_EXPIRED", "邀请码已过期");
        }
        // 4. 用户名占用校验
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "用户名已存在");
        }
        // 5. 建普通用户
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        // 6. 消费邀请码(标记使用者与使用时间)
        invite.setUsedBy(saved.getId());
        invite.setUsedAt(Instant.now());
        inviteCodeRepository.save(invite);
        // 7. 返回
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole().name());
    }
}
