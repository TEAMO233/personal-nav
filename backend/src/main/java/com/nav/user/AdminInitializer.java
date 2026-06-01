package com.nav.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时初始化管理员:库中无 ADMIN 且配置了凭据时,创建一个初始管理员。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username:}") String adminUsername,
                            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 1. 已存在管理员则跳过
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        // 2. 未配置初始管理员凭据则跳过并提示
        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
            log.warn("库中无管理员且未配置 APP_ADMIN_USERNAME/APP_ADMIN_PASSWORD,跳过初始管理员创建");
            return;
        }
        // 3. 用户名已被占用则跳过(避免和已有普通用户冲突)
        if (userRepository.existsByUsername(adminUsername)) {
            log.warn("初始管理员用户名 {} 已被占用,跳过创建", adminUsername);
            return;
        }
        // 4. 创建初始管理员
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);
        log.info("已创建初始管理员:{}", adminUsername);
    }
}
