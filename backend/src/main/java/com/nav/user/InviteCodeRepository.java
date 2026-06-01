package com.nav.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 邀请码数据访问。
 */
public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {
}
