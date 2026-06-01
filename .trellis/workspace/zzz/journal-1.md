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

---

## 2026-06-02 — M2 认证与会话(任务 06-01-personal-nav-home)

完成 M2,后端测试全绿:`./mvnw test` BUILD SUCCESS,15 tests(M1 数据层 6 + 认证 5 + 管理后台 3 + 限流 1)。
- Spring Security 6.5 + Spring Session Redis:JSON 登录、服务端会话存 Redis、Cookie(HttpOnly/Secure/SameSite=Lax)。
- CSRF 双提交(CookieCsrfTokenRepository + 官方 SpaCsrfTokenRequestHandler)。
- BCrypt;UserDetailsService 从库加载(DISABLED 拒登);login/logout/me;邀请码注册(单事务建用户+消费码);管理后台开户/重置密码/签发邀请码。
- Redis 固定窗口限流(Lua 原子 INCR+EXPIRE)。
- 初始 ADMIN:`APP_ADMIN_USERNAME/PASSWORD` 启动幂等注入。
- 统一错误结构 `{code,message}` + 全局异常处理。

关键决策/踩坑:
1. Spring Security 6 手动登录(controller 内 `authenticate`)需显式 `saveContext` 到 session(6.x 不再自动保存);并须显式补会话固定防护(见下 H1)。
2. SPA CSRF:查证官方 6.5 文档,`SpaCsrfTokenRequestHandler.handle()` 自调 `csrfToken.get()` 渲染 cookie,无需额外 `CsrfCookieFilter`。
3. Cookie/会话用 Boot 标准 `server.servlet.session.*` + `spring.session.redis.*`,Spring Session 自动接管。

trellis-check 安全复核发现并已修复:
- H1(高危)会话固定:手动认证绕过了过滤器的 `ChangeSessionIdAuthenticationStrategy`,登录后未换 sessionId。修复:login 认证成功后调 `ChangeSessionIdAuthenticationStrategy.onAuthentication` 换发 session id,并补回归测试(带旧会话再登录,sessionId 必变)。
- M1(中危)X-Forwarded-For 可伪造绕过限流:改用 `server.forward-headers-strategy=framework` 由框架在可信边界统一处理,`RequestUtils` 改用 `getRemoteAddr()`;生产须部署在可信代理后。
- M2(中危)限流缺纯 IP 维度:login 叠加 `rl:login-ip`(30 次/5 分)防单 IP 喷洒 / key 膨胀。

留待后续(低危,记 M8 收尾清单):
- 管理员禁用 / 重置密码后主动失效该用户的 Redis 会话(落实 design 的「即时注销」)。
- 邀请码并发同码消费的 TOCTOU:用 `UPDATE ... WHERE used_by IS NULL` 行数判定或唯一约束。
- 生产强制 `COOKIE_SECURE=true`(M8 在 README 列为必设项)。

下一步:M3 引擎管理(新用户初始化预置 4 引擎 + 引擎 CRUD / 排序 / 设默认,均按 user_id 隔离)。
