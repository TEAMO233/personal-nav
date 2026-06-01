# Journal - zzz (Part 1)

> AI development session journal
> Started: 2026-06-01

---

## 2026-06-01 — M0 项目脚手架(任务 06-01-personal-nav-home)

完成 M0,三项验证全绿:
- backend 打包:`./mvnw -DskipTests package` BUILD SUCCESS
- backend 集成测试:Testcontainers 起 PG+Redis,contextLoads 通过(1 passed)
- frontend 构建:`npm run build`(vue-tsc + vite build)通过

关键决策与踩坑:
1. Spring Boot 版本:Initializr 默认给 4.0.6,经确认选定 3.5.14(符合 design.md 锁定的 3.x;Security 6 / Testcontainers 生态更成熟)。
2. JDK:系统默认是 Java 8,JDK 21 装在 /usr/local/opt/openjdk@21;不改全局默认,构建时注入 JAVA_HOME 指向 21。
3. jsoup 未被 Boot 3.5.14 的 BOM 管理,pom 里显式定版 1.22.2(Maven Central 最新稳定)。
4. 前端 TS 6.0 已废弃 baseUrl(TS5101),tsconfig.app.json 改用相对 paths(@/* → ./src/*)。
5. Testcontainers 镜像暂用 postgres:16-alpine / redis:7-alpine;Redis 无官方模块,用 GenericContainer + @ServiceConnection(name="redis")。线上库版本待用户提供后对齐镜像。
6. 前端已知优化项:Element Plus 全量引入致打包 chunk ~990KB,留待 M6/M8 做按需引入。

下一步:M1 数据层与迁移(Flyway V1__init.sql 建 6 表 + JPA 实体 + Repository + 集成测试),数据模型见 design.md 第 4 节。

---

## 2026-06-01 — M1 数据层与迁移(任务 06-01-personal-nav-home)

完成 M1,后端测试全绿:`./mvnw test` BUILD SUCCESS,6 tests(1 contextLoads + 5 数据层)全过。
- Flyway `V1__init.sql` 建 6 表(users/invite_codes/media_assets/search_engines/shortcut_groups/shortcuts)+ 索引/外键/唯一/CHECK。
- 7 实体 + 6 Repository;`ddl-auto:validate` 确认实体与表严格对齐。

关键决策:
1. 主键:全表 UUID(用户拍板)。DDL `gen_random_uuid()` 兜底,实体 `@UuidGenerator` 在 persist 时生成。media id 进 URL,UUID 防枚举。
2. 时间戳 `timestamptz`↔`Instant` 配 `@CreationTimestamp`;枚举 `varchar+CHECK`↔`@Enumerated(STRING)`,枚举值统一大写以对齐枚举名。
3. 实体只映射外键 UUID 字段,不映射 `@ManyToOne`——契合多租户按 user_id 过滤,零跨包耦合;按功能分包(user/engine/shortcut/media + common.BaseEntity)。
4. 外键未带 `ON DELETE`,用 PG 默认 `NO ACTION`(等价阻止删除);级联策略留 M5。

踩坑:
1. `@Transactional` 测试里 `save()` 不 flush,`@CreationTimestamp` 在 insert(flush)时才赋值,导致 `save()` 直后 `getCreatedAt()` 为 null;而 `@UuidGenerator` 的 id 在 persist 时即分配(故 `getId()` 正常)。修法:断言"持久化后状态"改用 `saveAndFlush`。
2. LSP 报 `expected package ''` 是 monorepo 源根识别滞后的误报,Maven 编译/测试正常。

trellis-check 复核:通过(可进入 M2),仅 3 项低危;已把 design §4.1 的 RESTRICT 措辞修正为 NO ACTION 以对齐已验证的迁移。

下一步:M2 认证与会话(Spring Security JSON 登录 + Redis session + Cookie/CSRF + 角色 + BCrypt + 限流)。
