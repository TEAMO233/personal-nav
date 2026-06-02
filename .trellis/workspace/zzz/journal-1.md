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

---

## 2026-06-02 — M3 引擎管理(任务 06-01-personal-nav-home)

完成 M3,后端测试全绿:`./mvnw test` BUILD SUCCESS,23 tests(M1/M2 的 15 + 引擎 8)。
- 数据层:`V2__add_engine_icon_builtin.sql` 给 search_engines 加 `icon_builtin` 列;SearchEngine 实体 + Repository 补按用户查询/归属校验/计数方法。
- `EngineService`:预置初始化 + 按 user_id 隔离的 list/create/update/delete/reorder/setDefault;`EngineController` 暴露 GET/POST `/api/engines`、PUT/DELETE `/{id}`、PUT `/order`、PUT `/{id}/default`。
- 预置 4 引擎(Google 默认 / 百度 / Bing / DuckDuckGo)在邀请码注册、管理员开户、初始 ADMIN 三处同事务写入。
- 前端 `src/assets/engine-icons/` 放 4 个内置图标 svg(简洁 monogram,M7 可换为品牌图标)。

关键决策:
1. 预置图标用前端内置 svg,后端只存 `icon_builtin` key(google 等);自定义图标走 `icon_asset_id`(M4)。响应里两者都返回,前端据此拼图标。无跨域/proxy 问题。
2. URL 模板占位符定为 `{query}`,service 强制校验含此占位(否则 400);前端跳转替换为 `encodeURIComponent(关键词)`。
3. 多租户隔离用 `findByIdAndUserId`,越权返回 404(不暴露存在性),非 403。
4. 新引擎追加末尾;reorder 要求传全部引擎 id 的一个排列(否则 400);删默认引擎后把剩余最前的补设为默认。

测试覆盖:预置 4 引擎(Google 默认 + iconBuiltin)、A/B 用户隔离、增删改、逆序重排、设默认切换、越权 404、删默认兜底、缺占位模板 400。

踩坑:无新增踩坑。Spring Data Redis 对 JPA Repository 报 "Could not safely identify store assignment" 为 M1 即有的无害提示(多模块共存时的存储归属探测日志),不影响功能。

下一步:M4 文件存储与图标(StorageService + LocalDiskStorageService、media_assets 落库、`GET /api/media/{id}` 校验归属、上传 / 图片 URL / favicon 抓取含 SSRF 防护与超时大小上限)。

---

## 2026-06-02 — M4 文件存储与图标(任务 06-01-personal-nav-home)

完成 M4,后端测试全绿:`./mvnw test` BUILD SUCCESS,37 tests(M1-M3 的 23 + media 14:ImageTypeDetector 3 + SsrfGuard 3 + MediaIntegration 8)。
- `StorageService` 接口 + `LocalDiskStorageService`:按「年/月/随机uuid」组织文件,返回相对 key;`resolve` 拦截 `../` 目录穿越。
- `MediaService` 三来源(上传 / 图片外链 / 站点 favicon)统一落 `media_assets`;`loadForOwner` 按 `findByIdAndUserId` 读取,越权当不存在(404)。
- `MediaController` 四接口(upload / from-url / fetch-favicon / `GET {id}`);读取响应加 `nosniff` + CSP `sandbox` + `Content-Disposition: inline` + 私有缓存,防 SVG 等内嵌脚本执行。
- `SsrfGuard`:仅放行公网 http/https,拒环回(可配置开关)、私网、链路本地、多播,补 100.64/10 与 fc00::/7;按解析出的所有 IP 判定。
- `RemoteContentFetcher`:连接/读取超时 + 边读边限大小上限;关自动重定向改手动逐跳,每跳重做 SSRF 校验。
- `FaviconService`:jsoup 选 `link[rel~=icon]` 中 sizes 最大者,失败回退站点根 `/favicon.ico`。
- 配置:`spring.servlet.multipart` 大小限制、`app.media.fetch.*`(大小/超时/重定向/环回开关)、`app.storage.local.base-dir`;`GlobalExceptionHandler` 补 `MaxUploadSizeExceededException` → 413。

关键决策/踩坑:
1. 图片类型只认文件头魔数(`ImageTypeDetector`),不信任 multipart 声称的 content-type,挡住伪装成图片的可执行内容;SVG 走 CSP sandbox + nosniff + inline 隔离。
2. SSRF 防护下沉到抓取链路统一做:按域名解析出的全部 IP 判定(防域名指向内网),重定向每跳重校验(防跳转绕过);环回放行仅做成测试开关,生产保持 false。
3. 集成测试用 JDK 自带 `com.sun.net.httpserver.HttpServer` 起本机桩站点 + `allow-loopback=true` 跑真实抓取链路;私网/链路本地(169.254.169.254)即便放行环回仍被拒,单独验证。
4. multipart 上传在 Spring Security CSRF 双提交下:token 走 `X-XSRF-TOKEN` 头、`CsrfFilter` 不消费请求体,集成测试验证通过。

诊断澄清:IDE 报 `pom.xml:79 jsoup 缺 version` 与 `TestcontainersConfiguration 资源泄漏` 均为误报——jsoup 版本实写于 pom 第 87 行(`1.22.2`),Testcontainers 容器生命周期由框架管;`./mvnw test` 全绿为准。

下一步:M5 分组与快捷方式(分组 CRUD + 排序;快捷方式 CRUD + 排序 + 跨组移动,单事务原子更新 sort_order / group_id,均按 user_id 隔离)。

---

## 2026-06-02 — M5 分组与快捷方式(任务 06-01-personal-nav-home)

完成 M5,后端测试全绿:`./mvnw test` BUILD SUCCESS,46 tests(M1-M4 的 37 + 快捷方式 9)。
- 分组(本次起始已有):`GroupService`/`GroupController` 暴露 `GET/POST /api/groups`、`PUT/DELETE /{id}`、`PUT /order`,按 user_id 隔离、越权 404、新建排末尾、reorder 要求传全部分组 id 的一个排列。
- 快捷方式(本次新增):`ShortcutService`/`ShortcutController` 暴露 `GET/POST /api/shortcuts`、`PUT/DELETE /{id}`、`PUT /order`;新建排到组内末尾,update 改名称/URL/图标。
- 跨组移动 + 排序合一:`PUT /api/shortcuts/order` body `{items:[{id,groupId,sortOrder}]}`,单事务原子更新 group_id + sort_order。

关键决策:
1. shortcuts/order 采用「全集快照」契约:items 必须覆盖当前用户全部快捷方式(id 集合须完全一致,否则 400 SHORTCUT_ORDER_MISMATCH),且每个 groupId 必须属本人(否则 400 GROUP_NOT_OWNED)。沿用引擎/分组 reorder 的全集排列模式,保证组内 sort_order 一致、原子覆盖;前端拖拽后提交完整快照即可。
2. update 不改 groupId——跨组移动统一走 order 接口(遵循 design §6 接口划分)。M7 若要「编辑时换组」再评估是否给 update 加 groupId。
3. 删分组级联删快捷方式(落实 design §4.5 留待 M5 的决策):GroupService.delete 先删组内快捷方式再删分组(外键 NO ACTION,不先删会被阻止),同一事务。
4. iconAssetId 写入侧不校验归属:读取侧 M4 已按 user 隔离(loadForOwner 越权当不存在),引用他人 media 也读不到、无泄露;若传不存在的 id 会触发外键异常 → 500,属异常输入,记 M8 收尾酌情加显式校验。
5. sort_order 语义为「组内序号」:list 全局按 sort_order+createdAt 取出,前端按 groupId 分桶展示,组内即有序。

测试覆盖(9):增删改全流程、用户隔离、越权 404、组内重排、跨组移动持久化、order 漏传快照 400、移到他人组 400、在他人组建快捷方式 404、删分组级联删快捷方式。

踩坑:无新增。IDE 仍报 pom.xml:79 jsoup 缺 version 与 Testcontainers 资源泄漏,均为 M4 已澄清的误报,`./mvnw test` 全绿为准。

下一步:M6 前端首页(SearchBar 引擎下拉 + 按 URL 模板跳转;GroupGrid 分组 + 快捷方式卡片;authStore + 登录页 + axios 401 拦截 + 主题切换)。
