package com.nav.security;

import com.nav.user.User;
import com.nav.user.UserRepository;
import com.nav.user.UserStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库按用户名加载用户,供 Spring Security 认证使用。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 按用户名查用户,查不到抛异常
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        // 2. 封装成 Security 主体,DISABLED 账户标记为不可用(认证时会被拒)
        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getRole(), user.getStatus() == UserStatus.ACTIVE);
    }
}
