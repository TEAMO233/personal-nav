package com.nav.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 用户数据访问。
 */
public interface UserRepository extends JpaRepository<User, UUID> {
}
