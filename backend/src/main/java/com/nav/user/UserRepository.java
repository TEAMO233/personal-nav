package com.nav.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问。
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 按用户名查用户。
     *
     * @param username 用户名
     * @return 用户(可能为空)
     */
    Optional<User> findByUsername(String username);

    /**
     * 用户名是否已存在。
     *
     * @param username 用户名
     * @return 存在返回 true
     */
    boolean existsByUsername(String username);

    /**
     * 是否存在指定角色的用户。
     *
     * @param role 角色
     * @return 存在返回 true
     */
    boolean existsByRole(Role role);
}
