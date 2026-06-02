package com.nav.media.dto;

import com.nav.media.MediaAsset;

import java.util.UUID;

/**
 * 媒体资源对外响应。
 *
 * @param id          媒体 id
 * @param type        来源类型(UPLOAD / FAVICON / URL)
 * @param contentType 文件 MIME 类型
 * @param url         读取该媒体的接口地址(/api/media/{id})
 */
public record MediaResponse(UUID id, String type, String contentType, String url) {

    /**
     * 由实体转对外响应。
     *
     * @param asset 媒体实体
     * @return 响应
     */
    public static MediaResponse from(MediaAsset asset) {
        return new MediaResponse(asset.getId(), asset.getType().name(),
                asset.getContentType(), "/api/media/" + asset.getId());
    }
}
