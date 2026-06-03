package com.nav.shortcut;

import com.nav.common.error.ApiException;
import com.nav.media.MediaService;
import com.nav.shortcut.dto.CreateShortcutRequest;
import com.nav.shortcut.dto.ReorderShortcutsRequest;
import com.nav.shortcut.dto.ShortcutResponse;
import com.nav.shortcut.dto.UpdateShortcutRequest;
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
 * 快捷方式业务:按用户隔离的增删改查、组内排序与跨组移动。
 */
@Service
public class ShortcutService {

    private final ShortcutRepository shortcutRepository;
    private final ShortcutGroupRepository groupRepository;
    private final MediaService mediaService;

    public ShortcutService(ShortcutRepository shortcutRepository, ShortcutGroupRepository groupRepository,
                           MediaService mediaService) {
        this.shortcutRepository = shortcutRepository;
        this.groupRepository = groupRepository;
        this.mediaService = mediaService;
    }

    /**
     * 列出当前用户的全部快捷方式,前端按 groupId 分桶展示。
     *
     * @param userId 用户 id
     * @return 快捷方式列表
     */
    @Transactional(readOnly = true)
    public List<ShortcutResponse> list(UUID userId) {
        // 1. 按排序取出并转响应
        return shortcutRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                .stream().map(ShortcutResponse::from).toList();
    }

    /**
     * 新建快捷方式,排到目标分组内末尾。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新快捷方式
     */
    @Transactional
    public ShortcutResponse create(UUID userId, CreateShortcutRequest request) {
        // 1. 校验目标分组属于本人,不存在或越权均 404
        requireOwnedGroup(userId, request.groupId());
        // 2. 带自定义图标时校验图标属于本人
        if (request.iconAssetId() != null) {
            mediaService.assertOwned(userId, request.iconAssetId());
        }
        // 3. 排到组内末尾(排序值取该组现有快捷方式数)
        int sortOrder = (int) shortcutRepository.countByGroupId(request.groupId());
        // 4. 建快捷方式
        Shortcut s = new Shortcut();
        s.setUserId(userId);
        s.setGroupId(request.groupId());
        s.setName(request.name());
        s.setUrl(request.url());
        s.setIconAssetId(request.iconAssetId());
        s.setSortOrder(sortOrder);
        // 5. 保存并返回
        return ShortcutResponse.from(shortcutRepository.save(s));
    }

    /**
     * 更新快捷方式的名称 / URL / 图标(不改所属分组)。
     *
     * @param userId  用户 id
     * @param id      快捷方式 id
     * @param request 更新请求
     * @return 更新后的快捷方式
     */
    @Transactional
    public ShortcutResponse update(UUID userId, UUID id, UpdateShortcutRequest request) {
        // 1. 取本人快捷方式,不存在或越权均 404
        Shortcut s = requireOwned(userId, id);
        // 2. 带自定义图标时校验图标属于本人(传 null 是清除,不校验)
        if (request.iconAssetId() != null) {
            mediaService.assertOwned(userId, request.iconAssetId());
        }
        // 3. 更新可改字段
        s.setName(request.name());
        s.setUrl(request.url());
        s.setIconAssetId(request.iconAssetId());
        // 4. 保存并返回
        return ShortcutResponse.from(shortcutRepository.save(s));
    }

    /**
     * 删除快捷方式。
     *
     * @param userId 用户 id
     * @param id     快捷方式 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人快捷方式,不存在或越权均 404
        Shortcut s = requireOwned(userId, id);
        // 2. 删除
        shortcutRepository.delete(s);
    }

    /**
     * 重排 + 跨组移动:items 须为当前用户全部快捷方式的一份完整新位置快照,
     * 每项给出目标分组与组内排序值,在同一事务里原子更新 group_id 与 sort_order。
     *
     * @param userId 用户 id
     * @param items  全部快捷方式的新位置
     * @return 重排后的快捷方式列表
     */
    @Transactional
    public List<ShortcutResponse> reorder(UUID userId, List<ReorderShortcutsRequest.Item> items) {
        // 1. 取本人全部快捷方式
        List<Shortcut> shortcuts = shortcutRepository.findByUserId(userId);
        // 2. 校验传入 id 无重复且与库中集合完全一致(防漏传 / 多传 / 越权 id)
        List<UUID> incomingIds = items.stream().map(ReorderShortcutsRequest.Item::id).toList();
        Set<UUID> incoming = new HashSet<>(incomingIds);
        Set<UUID> owned = shortcuts.stream().map(Shortcut::getId).collect(Collectors.toSet());
        if (incoming.size() != incomingIds.size() || !incoming.equals(owned)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SHORTCUT_ORDER_MISMATCH",
                    "排序列表必须是当前全部快捷方式的一份完整快照");
        }
        // 3. 校验每个目标分组都属于本人(防移动到他人分组)
        Set<UUID> ownedGroups = groupRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                .stream().map(ShortcutGroup::getId).collect(Collectors.toSet());
        for (ReorderShortcutsRequest.Item item : items) {
            if (!ownedGroups.contains(item.groupId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GROUP_NOT_OWNED", "目标分组不存在或不属于当前用户");
            }
        }
        // 4. 按传入项更新每个快捷方式的所属分组与组内排序值
        Map<UUID, Shortcut> byId = shortcuts.stream().collect(Collectors.toMap(Shortcut::getId, s -> s));
        for (ReorderShortcutsRequest.Item item : items) {
            Shortcut s = byId.get(item.id());
            s.setGroupId(item.groupId());
            s.setSortOrder(item.sortOrder());
        }
        // 5. 批量保存,按分组聚合、组内排序返回
        shortcutRepository.saveAll(shortcuts);
        shortcuts.sort(Comparator.comparing(Shortcut::getGroupId).thenComparingInt(Shortcut::getSortOrder));
        return shortcuts.stream().map(ShortcutResponse::from).toList();
    }

    /**
     * 取本人快捷方式,不存在或不属于该用户均抛 404(不暴露资源存在性)。
     */
    private Shortcut requireOwned(UUID userId, UUID id) {
        return shortcutRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHORTCUT_NOT_FOUND", "快捷方式不存在"));
    }

    /**
     * 校验目标分组属于本人,不存在或越权均抛 404。
     */
    private void requireOwnedGroup(UUID userId, UUID groupId) {
        groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "分组不存在"));
    }
}
