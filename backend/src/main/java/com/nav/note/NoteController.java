package com.nav.note;

import com.nav.note.dto.CreateNoteRequest;
import com.nav.note.dto.NoteResponse;
import com.nav.note.dto.UpdateNoteRequest;
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
 * 灵感便签接口:首页内联管理。
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 列出当前用户便签。
     *
     * @param limit 最大返回数量,可空
     * @return 便签列表
     */
    @GetMapping
    public List<NoteResponse> list(@RequestParam(required = false) Integer limit) {
        // 1. 取当前用户便签
        return noteService.list(SecurityUtils.currentUserId(), limit);
    }

    /**
     * 新建便签。
     *
     * @param request 新建请求
     * @return 新便签
     */
    @PostMapping
    public NoteResponse create(@Valid @RequestBody CreateNoteRequest request) {
        // 1. 为当前用户新建便签
        return noteService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新便签。
     *
     * @param id      便签 id
     * @param request 更新请求
     * @return 更新后便签
     */
    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
        // 1. 更新当前用户便签
        return noteService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除便签。
     *
     * @param id 便签 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户便签
        noteService.delete(SecurityUtils.currentUserId(), id);
    }
}
