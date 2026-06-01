package com.nav;

import com.nav.media.MediaAsset;
import com.nav.media.MediaAssetRepository;
import com.nav.media.MediaType;
import com.nav.shortcut.Shortcut;
import com.nav.shortcut.ShortcutGroup;
import com.nav.shortcut.ShortcutGroupRepository;
import com.nav.shortcut.ShortcutRepository;
import com.nav.user.Role;
import com.nav.user.User;
import com.nav.user.UserRepository;
import com.nav.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 数据层集成测试。
 * 经 Testcontainers 起真实 PG16:验证 Flyway 建表、实体与表对齐、各 Repository 基础 CRUD、外键与唯一约束。
 * 每个测试方法独立事务并在结束回滚,互不影响数据。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DataLayerIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShortcutGroupRepository shortcutGroupRepository;
    @Autowired
    private ShortcutRepository shortcutRepository;
    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    /**
     * 用户的增查改删,并验证主键、创建时间、枚举默认值自动生成。
     */
    @Test
    void userCrud() {
        // 1. 新建用户并保存
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("$2a$10$dummybcrypthashvalueforunittest0000000000000000000000");
        User saved = userRepository.saveAndFlush(user);

        // 2. 持久化后应自动得到 UUID 主键、创建时间和枚举默认值
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // 3. 按 id 查回
        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("alice");

        // 4. 改名后再查应生效
        found.setUsername("alice2");
        userRepository.save(found);
        assertThat(userRepository.findById(saved.getId()).orElseThrow().getUsername()).isEqualTo("alice2");

        // 5. 删除后查不到
        userRepository.delete(found);
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    /**
     * 用户名唯一约束:重复用户名应保存失败。
     */
    @Test
    void usernameMustBeUnique() {
        // 1. 先存一个用户
        User u1 = new User();
        u1.setUsername("bob");
        u1.setPasswordHash("hash1");
        userRepository.saveAndFlush(u1);

        // 2. 再存同名用户应触发唯一约束异常
        User u2 = new User();
        u2.setUsername("bob");
        u2.setPasswordHash("hash2");
        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 外键链路:用户 -> 分组 -> 快捷方式 的保存与查询。
     */
    @Test
    void shortcutForeignKeyChain() {
        // 1. 建用户
        User user = new User();
        user.setUsername("carol");
        user.setPasswordHash("hash");
        UUID userId = userRepository.save(user).getId();

        // 2. 建该用户的分组
        ShortcutGroup group = new ShortcutGroup();
        group.setUserId(userId);
        group.setName("常用");
        UUID groupId = shortcutGroupRepository.save(group).getId();

        // 3. 在分组下建快捷方式
        Shortcut shortcut = new Shortcut();
        shortcut.setUserId(userId);
        shortcut.setGroupId(groupId);
        shortcut.setName("GitHub");
        shortcut.setUrl("https://github.com");
        Shortcut savedShortcut = shortcutRepository.save(shortcut);

        // 4. 查回校验归属关系
        Shortcut found = shortcutRepository.findById(savedShortcut.getId()).orElseThrow();
        assertThat(found.getGroupId()).isEqualTo(groupId);
        assertThat(found.getUserId()).isEqualTo(userId);
    }

    /**
     * 引用不存在的用户时,外键约束应拦截。
     */
    @Test
    void foreignKeyViolationRejected() {
        // 1. 用随机不存在的 user_id 建分组
        ShortcutGroup group = new ShortcutGroup();
        group.setUserId(UUID.randomUUID());
        group.setName("孤儿分组");

        // 2. 保存应因外键约束失败
        assertThatThrownBy(() -> shortcutGroupRepository.saveAndFlush(group))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 媒体资源枚举类型存取一致。
     */
    @Test
    void mediaAssetEnumPersistence() {
        // 1. 建用户
        User user = new User();
        user.setUsername("dave");
        user.setPasswordHash("hash");
        UUID userId = userRepository.save(user).getId();

        // 2. 存一条 favicon 类型媒体
        MediaAsset asset = new MediaAsset();
        asset.setUserId(userId);
        asset.setType(MediaType.FAVICON);
        asset.setStorageKey("2026/06/abc.ico");
        asset.setContentType("image/x-icon");
        MediaAsset saved = mediaAssetRepository.save(asset);

        // 3. 查回枚举值一致
        MediaAsset found = mediaAssetRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getType()).isEqualTo(MediaType.FAVICON);
    }
}
