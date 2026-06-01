package com.nav.engine.dto;

import com.nav.engine.SearchEngine;

import java.util.UUID;

/**
 * 引擎对外响应。
 *
 * @param id          引擎 id
 * @param name        名称
 * @param urlTemplate 搜索 URL 模板(含 {query} 占位)
 * @param iconBuiltin 内置图标 key(预置引擎用,如 google;否则为空)
 * @param iconAssetId 自定义图标媒体 id(可空,M4 媒体功能后启用)
 * @param isDefault   是否默认引擎
 * @param sortOrder   排序值
 * @param isPreset    是否系统预置
 */
public record EngineResponse(
        UUID id,
        String name,
        String urlTemplate,
        String iconBuiltin,
        UUID iconAssetId,
        boolean isDefault,
        int sortOrder,
        boolean isPreset) {

    /**
     * 由实体转对外响应。
     *
     * @param e 引擎实体
     * @return 响应
     */
    public static EngineResponse from(SearchEngine e) {
        return new EngineResponse(e.getId(), e.getName(), e.getUrlTemplate(),
                e.getIconBuiltin(), e.getIconAssetId(), e.isDefault(), e.getSortOrder(), e.isPreset());
    }
}
