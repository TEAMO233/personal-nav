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

---

## 2026-06-02 — M6 前端首页(任务 06-01-personal-nav-home)

完成 M6,前端构建全绿:`npm run build`(vue-tsc -b + vite build)通过,118 模块,最大 chunk ~106KB(axios);相比含 Element Plus 全量引入的 ~974KB 大幅瘦身,无 500KB 警告。

- 视觉:用户选定 Apple HIG 风格(经 apple-hig-designer skill 取规范)。设计令牌集中 `src/style.css`(亮 `:root` / 暗 `html.dark` 手动切换);清理 Vite 模板残留样式与 hero.png/vue.svg/vite.svg;index.html title/lang 改中文。
- API 层:`api/http.ts` axios 实例(withCredentials + withXSRFToken + XSRF-TOKEN/X-XSRF-TOKEN,对齐后端 CookieCsrfTokenRepository);响应拦截把 `{code,message}` 规范成 `ApiClientError`;业务 401 清登录态跳登录页,探测/登录请求标 `skipAuthInterceptor` 跳过。types + auth/engine/group/shortcut/media 模块。
- 状态:Pinia setup store —— authStore(init/login/logout/clear)、themeStore(localStorage + 系统偏好)、engineStore(默认引擎 getter)、shortcutStore(按组分桶 getter)。
- 路由:加 `/login` + 全局守卫(首次导航 `await init()` 探测登录态并拿 CSRF cookie;requiresAuth 未登录跳登录,已登录访问登录页回首页;未知路径回首页)。
- 组件:AppIcon(内联 SVG 线性图标,零图标库依赖)、ThemeToggle、EngineIcon、SearchBar(引擎下拉图标随选中变、`{query}` 编码后新标签跳转)、ShortcutCard(`a target=_blank rel=noopener`,无图标按名 hash 取色首字母)、GroupGrid(grid auto-fill 响应式 + 空状态)、HomeView(毛玻璃吸顶栏 + 用户菜单 + 并发加载 + 加载/错误态)、LoginView(仅登录,注册留 M7)。

关键决策/取舍:
1. 移除 Element Plus 全局引入,M6 全用原生 HIG 组件:EP 默认视觉与 HIG 差异大、覆盖不划算,且大幅减小 bundle。EP 依赖保留,M7 设置/管理按需引入(unplugin-vue-components + ElementPlusResolver)。已与用户说明,偏离 design 原「全局 EP」,已同步 design §7.1。
2. CSRF 鸡生蛋:登录 POST 需 token,故启动守卫先 `GET /me` 让后端下发 XSRF-TOKEN cookie;axios `withXSRFToken: true` 确保 token 一定附带。
3. 破循环依赖:http.ts 顶层不 import router/store,拦截器内动态 `import()`;build 的 `INEFFECTIVE_DYNAMIC_IMPORT` 提示即此,为预期、无害。
4. 工具链约束:`erasableSyntaxOnly` 禁 enum(改 union 类型),`verbatimModuleSyntax` 类型导入用 `import type`;svg 静态 import 经 `vite/client` 声明为 URL。

待真机验收(前端无单测,以 build + 真机为准):`cd frontend && npm run dev` + 后端起 PG/Redis 后,验证登录 → 首页展示本人引擎/快捷方式、切引擎图标变化、搜索跳转、暗/亮主题切换并记忆、会话过期 401 跳登录。

下一步:M7 前端设置/管理(引擎/分组/快捷方式 CRUD + vuedraggable 拖拽 + 图标上传/抓取;ADMIN 后台开户/重置密码/邀请码;Element Plus 按需引入)。

---

## 2026-06-02 — M7-1 工具链 + API/Store + 后端引擎图标(任务 06-01-personal-nav-home)

完成 M7-1(M7 拆为 3 子提交的第 1 个)。后端 `./mvnw test` 47 全绿(原 46 + 引擎图标 1);前端 `npm run build` 通过。

- 后端引擎图标(根因修复 prd 缺口):`CreateEngineRequest`/`UpdateEngineRequest` 补 `iconAssetId`,`EngineService.create/update` 保存。`search_engines.icon_asset_id` V1 即有(外键 →media_assets),无需迁移。测试走「上传图→建引擎带图标→更新传 null 清除」真实链路。
- 前端工具链:EP 按需引入(`unplugin-auto-import` + `unplugin-vue-components` + `ElementPlusResolver`,css 样式模式免 sass;`dirs:[]` 让本地组件保持显式 import;dts 输出到 `src/`)。拖拽库 `vuedraggable@4.1.0` → `vue-draggable-plus`(查证:前者维护停滞 + 与 Vite 8 有 CommonJS interop 报错史 + TS 类型需手写)。
- 前端 API:M6 只封装了只读 list,本步补齐 engine/group/shortcut 的增删改/排序、media 上传/外链/抓 favicon,新增 `admin.ts`(开户/重置密码/邀请码)与 `auth.register`;`types.ts` 补 EngineInput/GroupInput/ShortcutInput/ShortcutOrderItem/MediaAsset/InviteCode/CreateUserInput。
- 前端 Store:`engineStore` 补 create/update/remove/reorder/setDefault,`shortcutStore` 补分组与快捷方式的增删改/排序;用 `import * as xxxApi` 命名空间避免与 action 同名冲突;group 操作并入 shortcutStore(不另建,合 design)。写操作策略:create/update 用返回值精确更新,删引擎(默认重分配)/删分组(级联删快捷方式)用 reload/本地同步保证一致。

关键决策/踩坑:
1. EP 按需引入与 `vue-tsc -b && vite build` 的顺序:d.ts 由 vite 阶段的 unplugin 生成、vue-tsc 在前;M7-1 尚无 EP 使用,首次 build 生成空 d.ts 脚手架,后续写 EP 代码时由 dev/build 填充并提交。
2. 引擎图标外键约束:`icon_asset_id REFERENCES media_assets(id)`,故测试必须先真实上传媒体拿 id,不能传随机 UUID(否则外键违反 500)。

下一步:M7-2 用户设置页 `/settings`(引擎/分组/快捷方式管理 + vue-draggable-plus 拖拽 + IconPicker 三来源图标 + 移动端按钮降级)。

---

## 2026-06-02 — M7-2 用户设置页 /settings(任务 06-01-personal-nav-home)

完成 M7-2。前端 `npm run build`(vue-tsc -b + vite build)通过,1736 模块;EP 按需引入生效——EP 组件只进 SettingsView 独立 chunk(JS 310KB / CSS 110KB,gzip 104KB+15KB,访问 /settings 才加载),首页 HomeView(5.9KB)/LoginView 不受影响。布局经用户选定 EP Tabs(搜索引擎/快捷方式两个标签页)。

- 地基:main.ts 补 `element-plus/theme-chalk/dark/css-vars.css`(M6 移除全局 EP 后缺此,设置页 EP 组件靠 html.dark 驱动暗色;放 style.css 前,让 --el-* 覆盖对齐 HIG)。AppIcon 扩 13 个线性图标(plus/edit/trash/grip/arrow-up/down/left/folder/check/upload/link/image/close)。router 加 /settings(requiresAuth)。HomeView 启用"设置"入口跳 /settings,清理"即将上线"dead CSS。
- 共用:`composables/useIsMobile.ts`(matchMedia 监听 640px,拖拽降级用),引擎/快捷方式两区共用;style.css 加"设置页共用控件"(.s-icon-btn/.s-link-btn/.s-badge-default/.s-move-btns)避免两区重复样式。
- IconPicker(引擎/快捷方式共用):v-model 绑 iconAssetId,三来源(上传/图片外链/抓 favicon)+ 预览 + 清除;favicon 输入默认填关联站点(引擎取 urlTemplate 的 {query} 前段,快捷方式取 url);原生 UI + HIG 样式,错误走 ElMessage。
- EngineSection:引擎列表 + vue-draggable-plus 拖拽排序(@update 提交全量 id)+ 设默认 + 增删改(EP Dialog/Form,URL 模板前端校验含 {query})+ IconPicker;窄屏禁拖拽改上移/下移。
- ShortcutSection:两层拖拽——外层分组排序(group="groups")、内层快捷方式组内+组间(group="shortcuts",v-model 自动跨组搬移);拖拽/移动后按"全量快照"提交 reorderShortcuts(遍历所有组算 {id, 所在 groupId, 组内下标});分组/快捷方式增删改;窄屏降级为上移/下移 + ElDropdown"移到分组"。
- SettingsView:毛玻璃顶栏(返回首页 + 主题切换)+ EP Tabs,onMounted 确保两 store 已加载(直接访问/刷新场景)。

关键决策/踩坑:
1. EP 按需 d.ts 鸡生蛋:build=`vue-tsc -b && vite build`,vue-tsc 在前,但 unplugin 的 EP 符号 d.ts 由 vite 阶段生成。首次须先单跑 `npx vite build` 填充 auto-imports.d.ts / components.d.ts(写入 ElDialog/ElForm/ElTabs/ElSelect/ElDropdown/ElMessage 等),再跑完整 build 才过 vue-tsc。两个 d.ts 已更新,需随提交。
2. 拖拽全量快照:后端 reorderShortcuts 要求覆盖全部快捷方式;组间拖拽后被搬 item 的 groupId 取"它现在所在的本地分组 id"(非 item.groupId 旧值),保证跨组移动正确持久化。
3. 拖拽本地副本 + 同步:各区维护 localEngines / localGroups 本地 ref 供拖拽,deep watch store 同步;拖拽只改本地、commit 才动 store,失败回滚为 store 现状,无 watch 循环。
4. @vueuse/core(vue-draggable-plus 依赖)的 #__PURE__ 注释被 Rolldown(Vite 8)报 INVALID_ANNOTATION,系上游库 + 打包器无害提示,产物正常。

已知/待办:
- 预置引擎图标优先级:EngineIcon 内置图标优先于 iconAssetId(M6 既定),故给"预置"引擎额外设自定义图标不会显示(自定义引擎正常)。prd 未要求预置可换图标,未改(改优先级会动首页行为,超 M7-2 范围);若要"自定义覆盖内置"再单独评估。
- 前端无单测,拖拽(尤其嵌套的组间拖拽)、三来源图标上传/抓取、暗色下 EP 组件观感等以真机为准:`cd frontend && npm run dev` + 后端起 PG/Redis 登录后验收。

下一步:M7-3 ADMIN 后台 `/admin` + 邀请码注册 `/register` + HomeView 给 ADMIN 显示"管理后台"入口。

---

## 2026-06-02 — M7-3 ADMIN 后台 + 邀请码注册(任务 06-01-personal-nav-home)

完成 M7-3(M7 收尾)。后端 `./mvnw test` 48 全绿(原 47 + 列用户 1);前端 `npm run build` 通过,AdminView 独立 chunk(98.8KB / gzip 33.6KB,访问 /admin 才加载),首页 HomeView(6.1KB)不受影响。

- 后端(根因修复 design §6 缺口):原管理接口只有开户 / 按 UUID 重置密码 / 签发邀请码,缺「列出用户」途径——管理员重置已有用户密码拿不到其 UUID,重置 UI 无法落地。经用户确认补 `GET /api/admin/users`:新建 `AdminUserResponse`(id/用户名/角色/状态/创建时间,比 UserResponse 多状态与时间),`AdminService.listUsers`(findAll 按 createdAt 升序),`AdminController` GET;`/api/admin/**` 已限 ADMIN,自动受保护。测试:管理员列出含初始 admin + 新建用户且字段齐全、普通用户 403。
- 前端 API/类型:`types.ts` 加 `AdminUser`,`admin.ts` 加 `listUsers`。
- AdminView(`/admin`):顶栏复用 settings 风格;用户管理(el-table 列用户名/角色 tag/状态/创建时间 + 每行重置密码 + 开户对话框含角色 select)+ 邀请码(签发可选有效天数 + 本次会话签发码列表 + 复制)。EP 用法、ElMessage 对齐 EngineSection。
- RegisterView(`/register`,公开):邀请码 + 用户名 + 密码,注册后不自动登录(后端语义),引导去登录页。
- 登录/注册共用样式:把 LoginView 卡片样式提到 style.css `.auth-*` 共用类(避免 RegisterView 复制),LoginView 改用共用类并加「去注册」链接;视觉值不变。
- 路由/入口:router 加 `/admin`(requiresAuth+requiresAdmin)、`/register`(公开);守卫加「非 ADMIN 访问 /admin 挡回首页」「已登录访问注册页回首页」;RouteMeta 加 requiresAdmin。HomeView 用户菜单 ADMIN 显示「管理后台」(shield 图标)。AppIcon 补 shield/copy 两图标。

关键决策/踩坑:
1. 构建 JDK 坑:系统默认 Java 8,`/usr/libexec/java_home -v 21` 找不到 21 时会静默回退 Java 8(退出码 0,故 `||` 兜底不触发),导致 record DTO 全报"需要 class/interface/enum"。正解:JDK 21 在 brew 的 `/usr/local/opt/openjdk@21`,构建固定 `JAVA_HOME=/usr/local/opt/openjdk@21`,不靠 java_home 探测。
2. EP el-table slot 类型:`#default="{ row }"` 的 row 是 EP 的 `DefaultRow`,显示用属性访问不报错,但传给强类型函数(openReset(user: AdminUser))报 TS2345;在模板内标注 `{ row }: { row: AdminUser }` 反而与 EP slot 签名(含 column/$index)不兼容(参数逆变)。正解:slot 不标注,仅在调用处 `openReset(row as AdminUser)` 断言。
3. EP 按需 d.ts 鸡生蛋(同 M7-2):AdminView 新用 el-table/el-table-column/el-tag/el-input-number,先单跑 `npx vite build` 由 unplugin 填 components.d.ts,再跑完整 `npm run build`(vue-tsc 在前)才过。两个 d.ts 随提交。

待真机验收(前端无单测):ADMIN 登录见「管理后台」→ 开户/列表刷新/重置密码/签发并复制邀请码;普通用户无该入口且直访 /admin 被挡;登录页「去注册」→ 邀请码注册成功 → 跳登录 → 新账号可登录;无效/过期码报错。

下一步:M8 安全与收尾(复核 HTTPS / Cookie Secure / CSRF / SSRF / 限流;清理调试代码与未用依赖;README;并落实前述留待项:禁用用户即时失效会话、邀请码并发 TOCTOU、生产强制 COOKIE_SECURE)。

---

## 2026-06-03 — M8 安全与收尾(任务 06-01-personal-nav-home)

完成 M8(末个里程碑)。后端 `./mvnw test` 55 全绿(原 48 + 即时失效会话 4 + 并发 TOCTOU 1 + 图标归属 2);前端 `npm run build` 通过(AdminView chunk 99.6KB / gzip 33.9KB)。落实 design §3「即时注销」与各里程碑留待的安全加固项。

- 即时失效会话(落实 design §7.2 归 M8 的「禁用/启用 + 主动失效会话」):Spring Session 切带索引仓库(`spring.session.redis.repository-type=indexed`),AdminService 注入 `FindByIndexNameSessionRepository`,按用户名 `findByPrincipalName` 删会话。新增 `POST /api/admin/users/{id}/status` 启用/禁用(原 design §6 缺,UserStatus 此前仅用于拒登);禁用用户、重置密码后即时失效其全部会话;禁止管理员禁用自己(400 CANNOT_DISABLE_SELF)。前端 AdminView 用户表加启用/禁用(禁用走 ElMessageBox 二次确认,隐藏自己那行的按钮)。
- 邀请码并发 TOCTOU:InviteCodeRepository 加原子 `consumeIfUnused`(`UPDATE...WHERE used_by IS NULL`,`flushAutomatically` 保证 user 先落库满足外键),AuthService 建用户后原子消费、0 行则抛错回滚;保留前置 findByCode 给精确错误。
- 图标归属校验(M5 留待):MediaService 加 `assertOwned` + Repository `existsByIdAndUserId`;引擎/快捷方式 create/update 写 iconAssetId 非空时校验归属,不存在/越权 400 ICON_ASSET_INVALID(原直接写库触发外键异常 500)。
- 复核与收尾:确认 HTTPS(forward-headers framework)/Cookie 三属性/CSRF 双提交/SSRF/限流均无回归;无调试代码;补全 `.gitignore` 显式忽略 target/node_modules/dist(原靠全局忽略,不自包含);新建根 README(环境变量全表 + 构建运行 + 生产安全必读)。

关键决策/踩坑:
1. Spring Session 默认 `RedisSessionRepository` 不支持按用户查会话;须切 indexed(查证 Spring Boot 3.0+ 稳定属性 `repository-type=indexed`),indexed 仓库自动索引 Spring Security 用户名,`findByPrincipalName` 即可批量删。生产 Redis 禁 CONFIG 时 keyspace 通知需手动开,但主动删会话不依赖它。
2. 原子消费的外键时序:`@Modifying(flushAutomatically=true)` 确保 UPDATE 前先 flush user 的 insert(used_by 外键指向新用户),否则外键违反;`clearAutomatically=true` 避免读到陈旧实体。
3. 图标归属校验放 media 包(MediaService.assertOwned),engine/shortcut 注入 MediaService,领域内聚、错误码统一;media 不反向依赖,无循环。
4. 并发 TOCTOU 测试用两线程 + CountDownLatch 同时打同码注册,断言恰好 1 成功 1 被拒;无论时序,原子 UPDATE 保证只一个消费成功。
5. 前端 ElMessageBox 首次使用,沿用 EP 按需 d.ts 经验先 `npx vite build` 再完整 build(本次 d.ts 无变化,符号已在)。

待真机验收:管理员禁用某用户 → 该用户下次请求即被踢回登录页、无法重登;启用后恢复;改密后旧会话失效;引擎/快捷方式选他人图标被拒。生产部署按 README「生产部署与安全」逐项配置(尤其 COOKIE_SECURE=true、Redis keyspace notifications)。

M0–M8 全部完成;项目主体里程碑收尾。

---

## 2026-06-03 — M9 搜索历史(任务 06-01-personal-nav-home,验收期间新增)

完成 M9,验证全绿:后端 `./mvnw test` 62(原 55 + 搜索历史 7),前端 `npm run build` 通过。沿引擎/快捷方式既有的分层与多租户模式实现,零新坑。

- 后端:`V3__create_search_history.sql` 建 `search_history` 表(`UNIQUE(user_id, keyword)` 去重 + `(user_id, searched_at DESC)` 索引);`com.nav.search` 全套(实体/Repository/Service/Controller/DTO),按 user_id 隔离的列出(最近 20)/记录(去重置顶)/删除(越权 404 `SEARCH_HISTORY_NOT_FOUND`)/清空。
- 前端:`searchHistoryStore`(load/record/remove/clear/reset)+ `api/searchHistory`;SearchBar 输入框聚焦展示历史、输入时按「包含」过滤、点历史项用当前引擎搜、单条删除 + 一键清空;AppIcon 加 clock;HomeView 并发加载 + 登出 reset。

关键决策/踩坑:
1. 存储选后端账户持久化(跨设备),非 localStorage(用户拍板)。新增表与一套分层,接口 §6:`GET/POST/DELETE{id}/DELETE /api/search-history`;`/**` 由 `anyRequest().authenticated()` 自动要求登录,未在 SecurityConfig 单列。
2. 去重置顶:`UNIQUE(user_id, keyword)`,record 命中同词更新 `searchedAt` 置顶、否则新建;空白词不记录;关键词上限 256(DTO `@NotBlank @Size(max=256)` + Service `trim` 双保险)。列表只取最近 20 条(`findTop20...`),库内不限总量。
3. record 返回更新后的最近列表,前端直接覆盖本地(免二次拉取);删除/清空本地同步移除。记录失败前端静默(不阻塞搜索跳转)。
4. SearchBar 历史下拉与引擎下拉互斥:历史项 `@mousedown.prevent` 防输入框失焦抢先关闭下拉,失焦延迟 150ms 关闭兜底。

待真机验收:重启后端跑 V3 迁移后,登录搜索 → 历史出现在输入框聚焦下拉、去重置顶、点历史直接搜、单条删/清空、A/B 用户互不可见。

下一步:首页苹果风深色玻璃拟态改版(`未命名.md`),进入 Trellis 规划。
