package com.nav.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 媒体资源数据访问。
 */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}
