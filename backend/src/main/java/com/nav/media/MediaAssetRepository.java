package com.nav.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 媒体资源数据访问。
 */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    /**
     * 按 id 和归属用户查媒体,用于读取时校验归属。
     *
     * @param id     媒体 id
     * @param userId 归属用户 id
     * @return 命中的媒体(可空)
     */
    Optional<MediaAsset> findByIdAndUserId(UUID id, UUID userId);
}
