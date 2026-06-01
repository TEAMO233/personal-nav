package com.nav.shortcut;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 快捷方式数据访问。
 */
public interface ShortcutRepository extends JpaRepository<Shortcut, UUID> {
}
