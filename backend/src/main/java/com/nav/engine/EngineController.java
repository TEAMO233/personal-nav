package com.nav.engine;

import com.nav.engine.dto.CreateEngineRequest;
import com.nav.engine.dto.EngineResponse;
import com.nav.engine.dto.ReorderEnginesRequest;
import com.nav.engine.dto.UpdateEngineRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 搜索引擎接口:按当前登录用户隔离的增删改查、排序、设默认。
 */
@RestController
@RequestMapping("/api/engines")
public class EngineController {

    private final EngineService engineService;

    public EngineController(EngineService engineService) {
        this.engineService = engineService;
    }

    /**
     * 列出当前用户的全部引擎。
     *
     * @return 引擎列表
     */
    @GetMapping
    public List<EngineResponse> list() {
        // 1. 取当前用户引擎
        return engineService.list(SecurityUtils.currentUserId());
    }

    /**
     * 新建引擎。
     *
     * @param request 新建请求
     * @return 新引擎
     */
    @PostMapping
    public EngineResponse create(@Valid @RequestBody CreateEngineRequest request) {
        // 1. 为当前用户建引擎
        return engineService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新引擎。
     *
     * @param id      引擎 id
     * @param request 更新请求
     * @return 更新后的引擎
     */
    @PutMapping("/{id}")
    public EngineResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEngineRequest request) {
        // 1. 更新当前用户的引擎
        return engineService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除引擎。
     *
     * @param id 引擎 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户的引擎
        engineService.delete(SecurityUtils.currentUserId(), id);
    }

    /**
     * 批量重排引擎。
     *
     * @param request 排序请求
     * @return 重排后的引擎列表
     */
    @PutMapping("/order")
    public List<EngineResponse> reorder(@Valid @RequestBody ReorderEnginesRequest request) {
        // 1. 按传入顺序重排当前用户引擎
        return engineService.reorder(SecurityUtils.currentUserId(), request.orderedIds());
    }

    /**
     * 设默认引擎。
     *
     * @param id 引擎 id
     * @return 设为默认后的引擎
     */
    @PutMapping("/{id}/default")
    public EngineResponse setDefault(@PathVariable UUID id) {
        // 1. 把当前用户的该引擎设为默认
        return engineService.setDefault(SecurityUtils.currentUserId(), id);
    }
}
