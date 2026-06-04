package com.nav.visit.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 记录访问请求。
 *
 * @param shortcutId 快捷方式 id(优先使用,会校验归属)
 * @param name       访问名称(无 shortcutId 时必填)
 * @param url        访问 URL(无 shortcutId 时必填)
 */
public record RecordVisitRequest(
        UUID shortcutId,
        @Size(max = 96, message = "访问名称过长") String name,
        @Size(max = 2048, message = "访问 URL 过长") String url) {
}
