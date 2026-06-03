package com.nav.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /**
     * 原子消费邀请码:仅当尚未被使用时,标记使用者与使用时间。
     * 用受影响行数判定是否抢到(1=消费成功,0=已被他人抢先),杜绝并发同码重复注册。
     *
     * @param id     邀请码 id
     * @param userId 使用者 id
     * @param usedAt 使用时间
     * @return 受影响行数
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE InviteCode i SET i.usedBy = :userId, i.usedAt = :usedAt WHERE i.id = :id AND i.usedBy IS NULL")
    int consumeIfUnused(@Param("id") UUID id, @Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
