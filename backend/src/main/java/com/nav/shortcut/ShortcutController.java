package com.nav.shortcut;

import com.nav.security.SecurityUtils;
import com.nav.shortcut.dto.CreateShortcutRequest;
import com.nav.shortcut.dto.ReorderShortcutsRequest;
import com.nav.shortcut.dto.ShortcutResponse;
import com.nav.shortcut.dto.UpdateShortcutRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 快捷方式接口:按当前登录用户隔离的增删改查、组内排序与跨组移动。
 */
@RestController
@RequestMapping("/api/shortcuts")
public class ShortcutController {

    private final ShortcutService shortcutService;

    public ShortcutController(ShortcutService shortcutService) {
        this.shortcutService = shortcutService;
    }

    /**
     * 列出当前用户的全部快捷方式。
     *
     * @return 快捷方式列表
     */
    @GetMapping
    public List<ShortcutResponse> list() {
        // 1. 取当前用户快捷方式
        return shortcutService.list(SecurityUtils.currentUserId());
    }

    /**
     * 列出当前用户首页精选快捷方式。
     *
     * @return 首页精选快捷方式列表
     */
    @GetMapping("/featured")
    public List<ShortcutResponse> featured() {
        // 1. 取当前用户首页精选快捷方式
        return shortcutService.featured(SecurityUtils.currentUserId());
    }

    /**
     * 新建快捷方式。
     *
     * @param request 新建请求
     * @return 新快捷方式
     */
    @PostMapping
    public ShortcutResponse create(@Valid @RequestBody CreateShortcutRequest request) {
        // 1. 为当前用户建快捷方式
        return shortcutService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新快捷方式。
     *
     * @param id      快捷方式 id
     * @param request 更新请求
     * @return 更新后的快捷方式
     */
    @PutMapping("/{id}")
    public ShortcutResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateShortcutRequest request) {
        // 1. 更新当前用户的快捷方式
        return shortcutService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除快捷方式。
     *
     * @param id 快捷方式 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户的快捷方式
        shortcutService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 批量重排 + 跨组移动快捷方式。
     *
     * @param request 排序请求
     * @return 重排后的快捷方式列表
     */
    @PutMapping("/order")
    public List<ShortcutResponse> reorder(@Valid @RequestBody ReorderShortcutsRequest request) {
        // 1. 按传入位置重排当前用户快捷方式
        return shortcutService.reorder(SecurityUtils.currentUserId(), request.items());
    }
}
