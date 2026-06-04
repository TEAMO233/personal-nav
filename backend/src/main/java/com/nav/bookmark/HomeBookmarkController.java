package com.nav.bookmark;

import com.nav.bookmark.dto.CreateHomeBookmarkRequest;
import com.nav.bookmark.dto.HomeBookmarkResponse;
import com.nav.bookmark.dto.ReorderHomeBookmarksRequest;
import com.nav.bookmark.dto.UpdateHomeBookmarkRequest;
import com.nav.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 首页书签接口:设置页管理,首页读取启用书签。
 */
@RestController
@RequestMapping("/api/home-bookmarks")
public class HomeBookmarkController {

    private final HomeBookmarkService bookmarkService;

    public HomeBookmarkController(HomeBookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /**
     * 列出当前用户首页书签。
     *
     * @param enabledOnly 是否只返回启用书签
     * @return 书签列表
     */
    @GetMapping
    public List<HomeBookmarkResponse> list(@RequestParam(defaultValue = "true") boolean enabledOnly) {
        // 1. 首页默认只取启用项,设置页可传 false 取全部
        return bookmarkService.list(SecurityUtils.currentUserId(), enabledOnly);
    }

    /**
     * 新建首页书签。
     *
     * @param request 新建请求
     * @return 新书签
     */
    @PostMapping
    public HomeBookmarkResponse create(@Valid @RequestBody CreateHomeBookmarkRequest request) {
        // 1. 为当前用户创建书签
        return bookmarkService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新首页书签。
     *
     * @param id      书签 id
     * @param request 更新请求
     * @return 更新后书签
     */
    @PutMapping("/{id}")
    public HomeBookmarkResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateHomeBookmarkRequest request) {
        // 1. 更新当前用户书签
        return bookmarkService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除首页书签。
     *
     * @param id 书签 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户书签
        bookmarkService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 重排首页书签。
     *
     * @param request 排序请求
     * @return 重排后书签
     */
    @PutMapping("/order")
    public List<HomeBookmarkResponse> reorder(@Valid @RequestBody ReorderHomeBookmarksRequest request) {
        // 1. 按传入顺序重排当前用户书签
        return bookmarkService.reorder(SecurityUtils.currentUserId(), request.orderedIds());
    }
}
