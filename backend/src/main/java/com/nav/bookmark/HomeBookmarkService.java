package com.nav.bookmark;

import com.nav.bookmark.dto.CreateHomeBookmarkRequest;
import com.nav.bookmark.dto.HomeBookmarkResponse;
import com.nav.bookmark.dto.UpdateHomeBookmarkRequest;
import com.nav.common.error.ApiException;
import com.nav.media.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 首页书签业务:按用户隔离的列表、新增、更新、删除与排序。
 */
@Service
public class HomeBookmarkService {

    private final HomeBookmarkRepository bookmarkRepository;
    private final MediaService mediaService;

    public HomeBookmarkService(HomeBookmarkRepository bookmarkRepository, MediaService mediaService) {
        this.bookmarkRepository = bookmarkRepository;
        this.mediaService = mediaService;
    }

    /**
     * 列出当前用户首页书签。
     *
     * @param userId      用户 id
     * @param enabledOnly 是否只返回启用书签
     * @return 书签列表
     */
    @Transactional(readOnly = true)
    public List<HomeBookmarkResponse> list(UUID userId, boolean enabledOnly) {
        // 1. 首页只取启用项,设置页取全部项
        List<HomeBookmark> bookmarks = enabledOnly
                ? bookmarkRepository.findByUserIdAndEnabledTrueOrderBySortOrderAscCreatedAtDesc(userId)
                : bookmarkRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(userId);
        // 2. 转响应
        return bookmarks.stream().map(HomeBookmarkResponse::from).toList();
    }

    /**
     * 新建首页书签。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新书签
     */
    @Transactional
    public HomeBookmarkResponse create(UUID userId, CreateHomeBookmarkRequest request) {
        // 1. 带图标时校验归属
        assertIconOwned(userId, request.iconAssetId());
        // 2. 建书签,默认排到末尾
        HomeBookmark bookmark = new HomeBookmark();
        bookmark.setUserId(userId);
        bookmark.setName(request.name().trim());
        bookmark.setUrl(request.url().trim());
        bookmark.setDescription(cleanNullable(request.description()));
        bookmark.setIconAssetId(request.iconAssetId());
        bookmark.setEnabled(request.enabled() == null || request.enabled());
        bookmark.setSortOrder((int) bookmarkRepository.countByUserId(userId));
        // 3. 保存并返回
        return HomeBookmarkResponse.from(bookmarkRepository.save(bookmark));
    }

    /**
     * 更新首页书签。
     *
     * @param userId  用户 id
     * @param id      书签 id
     * @param request 更新请求
     * @return 更新后书签
     */
    @Transactional
    public HomeBookmarkResponse update(UUID userId, UUID id, UpdateHomeBookmarkRequest request) {
        // 1. 取本人书签并校验图标
        HomeBookmark bookmark = requireOwned(userId, id);
        assertIconOwned(userId, request.iconAssetId());
        // 2. 更新字段
        bookmark.setName(request.name().trim());
        bookmark.setUrl(request.url().trim());
        bookmark.setDescription(cleanNullable(request.description()));
        bookmark.setIconAssetId(request.iconAssetId());
        bookmark.setEnabled(request.enabled() == null || request.enabled());
        // 3. 保存并返回
        return HomeBookmarkResponse.from(bookmarkRepository.save(bookmark));
    }

    /**
     * 删除首页书签。
     *
     * @param userId 用户 id
     * @param id     书签 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 删除当前用户书签
        bookmarkRepository.delete(requireOwned(userId, id));
    }

    /**
     * 重排首页书签。
     *
     * @param userId     用户 id
     * @param orderedIds 有序书签 id 列表
     * @return 重排后书签
     */
    @Transactional
    public List<HomeBookmarkResponse> reorder(UUID userId, List<UUID> orderedIds) {
        // 1. 取本人全部书签
        List<HomeBookmark> bookmarks = bookmarkRepository.findByUserId(userId);
        // 2. 校验传入 id 是当前用户全部书签的一份排列
        Set<UUID> incoming = new HashSet<>(orderedIds);
        Set<UUID> owned = bookmarks.stream().map(HomeBookmark::getId).collect(Collectors.toSet());
        if (incoming.size() != orderedIds.size() || !incoming.equals(owned)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HOME_BOOKMARK_ORDER_MISMATCH",
                    "排序列表必须是当前全部首页书签的一个排列");
        }
        // 3. 按传入顺序写排序值
        Map<UUID, HomeBookmark> byId = bookmarks.stream().collect(Collectors.toMap(HomeBookmark::getId, b -> b));
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setSortOrder(i);
        }
        // 4. 保存并按新顺序返回
        bookmarkRepository.saveAll(bookmarks);
        bookmarks.sort(Comparator.comparingInt(HomeBookmark::getSortOrder));
        return bookmarks.stream().map(HomeBookmarkResponse::from).toList();
    }

    /**
     * 取本人书签,不存在或越权均 404。
     */
    private HomeBookmark requireOwned(UUID userId, UUID id) {
        return bookmarkRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "HOME_BOOKMARK_NOT_FOUND", "首页书签不存在"));
    }

    /**
     * 校验图标归属。
     */
    private void assertIconOwned(UUID userId, UUID iconAssetId) {
        if (iconAssetId != null) {
            mediaService.assertOwned(userId, iconAssetId);
        }
    }

    /**
     * 清理可空文本。
     */
    private String cleanNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
