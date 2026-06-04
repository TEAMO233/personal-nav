package com.nav.note;

import com.nav.common.error.ApiException;
import com.nav.note.dto.CreateNoteRequest;
import com.nav.note.dto.NoteResponse;
import com.nav.note.dto.UpdateNoteRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 灵感便签业务:按用户隔离的首页内联列表、新增、更新与删除。
 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    /**
     * 列出当前用户便签。
     *
     * @param userId 用户 id
     * @param limit  最大返回数量,可空
     * @return 便签列表
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID userId, Integer limit) {
        // 1. 置顶优先,再按排序与创建时间
        Sort sort = homeSort();
        List<Note> notes = limit == null
                ? noteRepository.findByUserId(userId, sort)
                : noteRepository.findByUserId(userId, PageRequest.of(0, Math.max(1, Math.min(limit, 50)), sort));
        // 2. 转响应
        return notes.stream().map(NoteResponse::from).toList();
    }

    /**
     * 新建便签。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新便签
     */
    @Transactional
    public NoteResponse create(UUID userId, CreateNoteRequest request) {
        // 1. 建便签
        Note note = new Note();
        note.setUserId(userId);
        note.setContent(request.content());
        note.setPinned(Boolean.TRUE.equals(request.pinned()));
        note.setSortOrder((int) noteRepository.countByUserId(userId));
        // 2. 立即 flush,确保创建时间在响应体中可见
        return NoteResponse.from(noteRepository.saveAndFlush(note));
    }

    /**
     * 更新便签。
     *
     * @param userId  用户 id
     * @param id      便签 id
     * @param request 更新请求
     * @return 更新后便签
     */
    @Transactional
    public NoteResponse update(UUID userId, UUID id, UpdateNoteRequest request) {
        // 1. 取本人便签
        Note note = requireOwned(userId, id);
        // 2. 更新字段
        note.setContent(request.content());
        note.setPinned(Boolean.TRUE.equals(request.pinned()));
        // 3. 保存并返回
        return NoteResponse.from(noteRepository.save(note));
    }

    /**
     * 删除便签。
     *
     * @param userId 用户 id
     * @param id     便签 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人便签并删除
        noteRepository.delete(requireOwned(userId, id));
    }

    /**
     * 取本人便签,不存在或越权均 404。
     */
    private Note requireOwned(UUID userId, UUID id) {
        return noteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTE_NOT_FOUND", "便签不存在"));
    }

    /**
     * 首页便签排序。
     */
    private Sort homeSort() {
        return Sort.by(
                Sort.Order.desc("pinned"),
                Sort.Order.desc("createdAt"),
                Sort.Order.asc("sortOrder"));
    }
}
