package com.nav.shortcut;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 快捷方式数据访问。
 */
public interface ShortcutRepository extends JpaRepository<Shortcut, UUID> {

    /**
     * 按用户取全部快捷方式,排序值升序、创建时间升序(同一分组内即有序)。
     *
     * @param userId 用户 id
     * @return 快捷方式列表
     */
    List<Shortcut> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    /**
     * 按用户取全部快捷方式(顺序无关),用于重排时校验 id 归属。
     *
     * @param userId 用户 id
     * @return 快捷方式列表
     */
    List<Shortcut> findByUserId(UUID userId);

    /**
     * 按 id 取快捷方式并校验归属,不属于该用户则返回空。
     *
     * @param id     快捷方式 id
     * @param userId 用户 id
     * @return 快捷方式,可能为空
     */
    Optional<Shortcut> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计某分组下的快捷方式数,用于新建时排到组内末尾。
     *
     * @param groupId 分组 id
     * @return 快捷方式数
     */
    long countByGroupId(UUID groupId);

    /**
     * 删除某分组下的全部快捷方式,供删除分组时级联清理。
     *
     * @param groupId 分组 id
     */
    void deleteByGroupId(UUID groupId);
}
