package com.nav.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 邀请码数据访问。
 */
public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {

    /**
     * 按邀请码字符串查记录。
     *
     * @param code 邀请码
     * @return 邀请码记录(可能为空)
     */
    Optional<InviteCode> findByCode(String code);
}
