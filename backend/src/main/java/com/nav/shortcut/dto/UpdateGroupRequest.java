package com.nav.shortcut.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新分组请求体。
 *
 * @param name 分组名称
 */
public record UpdateGroupRequest(
        @NotBlank(message = "分组名称不能为空") @Size(max = 64, message = "分组名称过长") String name) {
}
