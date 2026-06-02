package com.nav.shortcut;

import com.nav.common.error.ApiException;
import com.nav.shortcut.dto.CreateGroupRequest;
import com.nav.shortcut.dto.GroupResponse;
import com.nav.shortcut.dto.UpdateGroupRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 快捷方式分组业务:按用户隔离的增删改查与排序;删分组时级联删除其下快捷方式。
 */
@Service
public class GroupService {

    private final ShortcutGroupRepository groupRepository;
    private final ShortcutRepository shortcutRepository;

    public GroupService(ShortcutGroupRepository groupRepository, ShortcutRepository shortcutRepository) {
        this.groupRepository = groupRepository;
        this.shortcutRepository = shortcutRepository;
    }

    /**
     * 列出当前用户的全部分组。
     *
     * @param userId 用户 id
     * @return 分组列表
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> list(UUID userId) {
        // 1. 按排序取出并转响应
        return groupRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                .stream().map(GroupResponse::from).toList();
    }

    /**
     * 新建分组,排到现有分组末尾。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新分组
     */
    @Transactional
    public GroupResponse create(UUID userId, CreateGroupRequest request) {
        // 1. 排到末尾(排序值取现有分组数)
        int sortOrder = (int) groupRepository.countByUserId(userId);
        // 2. 建分组
        ShortcutGroup g = new ShortcutGroup();
        g.setUserId(userId);
        g.setName(request.name());
        g.setSortOrder(sortOrder);
        // 3. 保存并返回
        return GroupResponse.from(groupRepository.save(g));
    }

    /**
     * 更新分组名称。
     *
     * @param userId  用户 id
     * @param id      分组 id
     * @param request 更新请求
     * @return 更新后的分组
     */
    @Transactional
    public GroupResponse update(UUID userId, UUID id, UpdateGroupRequest request) {
        // 1. 取本人分组,不存在或越权均 404
        ShortcutGroup g = requireOwned(userId, id);
        // 2. 更新名称
        g.setName(request.name());
        // 3. 保存并返回
        return GroupResponse.from(groupRepository.save(g));
    }

    /**
     * 删除分组,并级联删除其下全部快捷方式(同一事务)。
     *
     * @param userId 用户 id
     * @param id     分组 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人分组,不存在或越权均 404
        ShortcutGroup g = requireOwned(userId, id);
        // 2. 先删该分组下的全部快捷方式(外键 NO ACTION,不先删会被阻止)
        shortcutRepository.deleteByGroupId(g.getId());
        // 3. 再删分组
        groupRepository.delete(g);
    }

    /**
     * 重排分组:orderedIds 须为当前用户全部分组 id 的一个排列,按其顺序写排序值。
     *
     * @param userId     用户 id
     * @param orderedIds 有序分组 id 列表
     * @return 重排后的分组列表
     */
    @Transactional
    public List<GroupResponse> reorder(UUID userId, List<UUID> orderedIds) {
        // 1. 取本人全部分组
        List<ShortcutGroup> groups = groupRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);
        // 2. 校验传入 id 无重复且与库中集合完全一致(防漏传/多传/越权 id)
        Set<UUID> incoming = new HashSet<>(orderedIds);
        Set<UUID> owned = groups.stream().map(ShortcutGroup::getId).collect(Collectors.toSet());
        if (incoming.size() != orderedIds.size() || !incoming.equals(owned)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GROUP_ORDER_MISMATCH", "排序列表必须是当前全部分组的一个排列");
        }
        // 3. 建 id->实体 映射,按传入顺序赋排序值
        Map<UUID, ShortcutGroup> byId = groups.stream().collect(Collectors.toMap(ShortcutGroup::getId, g -> g));
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setSortOrder(i);
        }
        // 4. 批量保存,按新排序返回
        groupRepository.saveAll(groups);
        groups.sort(Comparator.comparingInt(ShortcutGroup::getSortOrder));
        return groups.stream().map(GroupResponse::from).toList();
    }

    /**
     * 取本人分组,不存在或不属于该用户均抛 404(不暴露资源存在性)。
     */
    private ShortcutGroup requireOwned(UUID userId, UUID id) {
        return groupRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "分组不存在"));
    }
}
