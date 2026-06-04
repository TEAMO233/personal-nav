package com.nav.note;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 灵感便签数据访问。
 */
public interface NoteRepository extends JpaRepository<Note, UUID> {

    /**
     * 按用户查询便签,支持排序。
     *
     * @param userId 用户 id
     * @param sort   排序
     * @return 便签列表
     */
    List<Note> findByUserId(UUID userId, Sort sort);

    /**
     * 按用户查询便签,支持分页。
     *
     * @param userId   用户 id
     * @param pageable 分页与排序
     * @return 便签列表
     */
    List<Note> findByUserId(UUID userId, Pageable pageable);

    /**
     * 按 id 取便签并校验归属。
     *
     * @param id     便签 id
     * @param userId 用户 id
     * @return 便签,可能为空
     */
    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计当前用户便签数量,用于新建排序。
     *
     * @param userId 用户 id
     * @return 便签数量
     */
    long countByUserId(UUID userId);
}
