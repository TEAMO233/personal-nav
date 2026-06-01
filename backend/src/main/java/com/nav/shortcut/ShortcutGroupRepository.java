package com.nav.shortcut;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 快捷方式分组数据访问。
 */
public interface ShortcutGroupRepository extends JpaRepository<ShortcutGroup, UUID> {
}
