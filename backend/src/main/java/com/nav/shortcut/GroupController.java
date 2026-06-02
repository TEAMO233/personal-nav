package com.nav.shortcut;

import com.nav.security.SecurityUtils;
import com.nav.shortcut.dto.CreateGroupRequest;
import com.nav.shortcut.dto.GroupResponse;
import com.nav.shortcut.dto.ReorderGroupsRequest;
import com.nav.shortcut.dto.UpdateGroupRequest;
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
 * 快捷方式分组接口:按当前登录用户隔离的增删改查与排序。
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * 列出当前用户的全部分组。
     *
     * @return 分组列表
     */
    @GetMapping
    public List<GroupResponse> list() {
        // 1. 取当前用户分组
        return groupService.list(SecurityUtils.currentUserId());
    }

    /**
     * 新建分组。
     *
     * @param request 新建请求
     * @return 新分组
     */
    @PostMapping
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request) {
        // 1. 为当前用户建分组
        return groupService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新分组。
     *
     * @param id      分组 id
     * @param request 更新请求
     * @return 更新后的分组
     */
    @PutMapping("/{id}")
    public GroupResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGroupRequest request) {
        // 1. 更新当前用户的分组
        return groupService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除分组(级联删除其下快捷方式)。
     *
     * @param id 分组 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户的分组及其下快捷方式
        groupService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 批量重排分组。
     *
     * @param request 排序请求
     * @return 重排后的分组列表
     */
    @PutMapping("/order")
    public List<GroupResponse> reorder(@Valid @RequestBody ReorderGroupsRequest request) {
        // 1. 按传入顺序重排当前用户分组
        return groupService.reorder(SecurityUtils.currentUserId(), request.orderedIds());
    }
}
