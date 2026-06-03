# 个人导航主页 — 技术设计 (design.md)

> 本文承接 prd.md 的 D1–D11 决策,记录技术设计;执行步骤见 implement.md。
> 凡标注「默认」的技术点为成熟事实标准的推荐选择,review 时可调整。

## 1. 架构总览

- 单仓 monorepo:`backend/`(Spring Boot)+ `frontend/`(Vue3 + Vite)。**默认**
- 前后端分离:开发期 Vite 用 proxy 把 `/api` 转发到后端;生产期前端构建产物由 Nginx 或后端静态托管,Nginx 终止 HTTPS。
- 后端分层:`controller → service → repository(JPA) → PostgreSQL`;Redis 作旁路缓存与会话存储;`StorageService` 抽象文件存储。

## 2. 技术栈(对应 D8 / D11)

- 后端:Spring Boot 3.x, JDK 21, Maven, Spring Security, Spring Data JPA, Spring Data Redis, Flyway, jsoup(解析 favicon), Bean Validation。
- 前端:Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, axios, vuedraggable(基于 SortableJS,实现拖拽排序)。**默认**
- 测试:后端用 Testcontainers 起临时 PostgreSQL + Redis 做集成测试(与线上同款引擎,开发期不碰线上库,需本机 Docker);JUnit 5 + Spring Boot Test。线上 PG/Redis 连接方式由用户在代码完成后提供,仅经环境变量注入,代码不含任何硬编码连接信息。

## 3. 认证与会话(对应 D1 / D2 / D9)

- 受控开户:无公开注册入口。两条创建路径——管理员后台直接开户;或用有效邀请码自助注册。
- 密码:BCrypt 哈希存储,不存明文。**默认**
- 会话:登录成功后生成随机 opaque sessionId,会话数据存 Redis(带 TTL,可滑动续期);Cookie 下发 sessionId,属性 `HttpOnly + Secure + SameSite=Lax`。注销即删除对应 Redis key。
  - 选服务端 session 而非无状态 JWT 的原因:可即时注销、便于按会话做限流与管理(契合 D3/D9)。代价是有状态,但单机 + Redis 完全可接受。
- 授权:角色 `USER` / `ADMIN`。ADMIN 可进管理后台:开户、重置密码、签发邀请码。
- 限流:登录、开户/注册接口按「IP + 用户名」维度用 Redis 计数限流(固定窗口 + TTL),超阈值返回 429。
- 多租户隔离:所有用户数据表带 `user_id`;service 层统一以「当前登录用户 id」过滤与校验归属,杜绝越权(对应 D1)。

## 4. 数据模型(PostgreSQL,Flyway 管理)

- `users`(id, username 唯一, password_hash, role, status, created_at)
- `invite_codes`(id, code 唯一, created_by, used_by 可空, expires_at, used_at)
- `search_engines`(id, user_id, name, url_template, icon_asset_id 可空, icon_builtin 可空, is_default, sort_order, is_preset, created_at)
- `shortcut_groups`(id, user_id, name, sort_order, created_at)
- `shortcuts`(id, user_id, group_id, name, url, icon_asset_id 可空, sort_order, created_at)
- `media_assets`(id, user_id, type[upload/favicon/url], storage_key, source_url 可空, content_type, created_at)
- 索引:各表 `user_id`;排序用 `sort_order`;`username`、`code` 唯一索引。

预置引擎策略:用户首次初始化时,把 Google/Baidu/Bing/DuckDuckGo 作为该用户的 `search_engines` 记录写入(`is_preset=true`),之后用户可改、可删、可排序、可设默认。预置引擎图标用打包内置的静态资源,不进 `media_assets`;仅用户自定义图标才入库,减少存储与抓取开销。

### 4.1 数据层实现约定(M1 敲定,已与用户确认)

- 主键:全表统一 UUID。DDL 用 `gen_random_uuid()`(PG16 内置,无需扩展)做兜底默认值;实体侧用 Hibernate `@UuidGenerator` 在保存时生成,`save()` 后即有 id。media id 会进 `/api/media/{id}` URL,UUID 天然防枚举遍历。
- 时间戳:列用 `timestamptz`,实体用 `Instant`;`created_at` 由 `@CreationTimestamp` 填充,DDL 配 `DEFAULT now()` 兜底。
- 枚举:`role` / `status` / media `type` 用 `varchar + CHECK` 存字符串,实体用 `@Enumerated(EnumType.STRING)`。
- 外键:所有 `user_id` / `group_id` / `icon_asset_id` / `created_by` / `used_by` 加 `REFERENCES` 约束;未带 `ON DELETE` 子句,采用 PostgreSQL 默认 `NO ACTION`(同样阻止删除被引用行,区别仅在约束检查时机),分组删除时如何级联处理其下快捷方式留到 M5 业务层定。
- 实体关联:实体只持有外键 UUID 字段(如 `userId`),不映射 `@ManyToOne` 关联对象——契合「service 层统一按当前登录用户 id 过滤」的多租户风格,规避 N+1 与懒加载坑,也让按功能分包时实体零跨包耦合。
- 公共基类:`com.nav.common.BaseEntity`(`@MappedSuperclass`)抽出 `id` 与 `createdAt`,各实体继承。
- 包结构:按功能分包 —— `com.nav.user`(User / InviteCode 账户体系)、`com.nav.engine`、`com.nav.shortcut`(ShortcutGroup + Shortcut)、`com.nav.media`,公共件放 `com.nav.common`。
- 建表顺序(满足外键依赖):users → invite_codes → media_assets → search_engines → shortcut_groups → shortcuts。完整列定义以 `V1__init.sql` 为准,本文不重复。

### 4.2 引擎管理实现约定(M3 敲定,已与用户确认)

- 新增列 `icon_builtin VARCHAR(64)` 可空(迁移 `V2__add_engine_icon_builtin.sql`,Flyway 只增不改):存预置引擎的内置图标 key(google/baidu/bing/duckduckgo)。预置引擎 `icon_builtin` 非空、`icon_asset_id` 为空;自定义引擎反之(自定义图标 M4 启用)。图标 svg 内置在前端 `src/assets/engine-icons/`,前端按 key 映射渲染;响应同时返回 `iconBuiltin` 与 `iconAssetId` 供前端拼图标。
- URL 模板占位符统一用 `{query}`(如 `https://www.google.com/search?q={query}`);新建/更新引擎时 service 强制校验模板含此占位,否则 400(`ENGINE_URL_TEMPLATE_INVALID`)。前端跳转时把 `{query}` 替换为 `encodeURIComponent(关键词)`。
- 预置 4 引擎:Google(默认)、百度、Bing、DuckDuckGo,`sort_order` 0–3、`is_preset=true`;在用户创建(邀请码注册 / 管理员开户 / 初始 ADMIN)的同一事务内由 `EngineService.initPresetEngines` 写入。
- 多租户隔离:所有引擎操作经 `findByIdAndUserId` 校验归属;访问或修改他人引擎返回 **404**(不暴露资源存在性),非 403。
- 新建引擎追加到末尾(`sort_order = 当前引擎数`);排序接口要求传当前全部引擎 id 的一个排列,否则 400(`ENGINE_ORDER_MISMATCH`)。
- 删除默认引擎后,把剩余 `sort_order` 最前的引擎补设为默认,保证始终有可用默认引擎。

## 5. 文件存储与图标(对应 D5 / D6)

- `StorageService` 接口:`store(bytes, contentType) -> key`、`load(key) -> stream`、`delete(key)`、`exists(key)`。
- `LocalDiskStorageService` 实现:存到配置目录(如 `./data/media/{yyyy}/{MM}/{uuid}`),返回相对 key。后端 `GET /api/media/{assetId}` 流式读取并校验归属当前用户。
- favicon 抓取 `FaviconService`:请求目标站点 HTML → jsoup 解析 `<link rel="icon" / "shortcut icon" / "apple-touch-icon">` 取最优 → 失败回退 `origin/favicon.ico` → 下载字节经 `StorageService` 存储并建 `media_assets(type=favicon)`。强制超时与大小上限;做 SSRF 防护(仅允许公网 http/https,禁环回/私网地址)。

## 6. API 契约(RESTful JSON,统一 `/api` 前缀)

- 认证:`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`POST /api/auth/register`(仅邀请码模式)
- 管理(ADMIN):`GET /api/admin/users`(列出用户)、`POST /api/admin/users`、`POST /api/admin/users/{id}/reset-password`、`POST /api/admin/users/{id}/status`(启用/禁用)、`POST /api/admin/invite-codes`
- 引擎:`GET/POST /api/engines`、`PUT/DELETE /api/engines/{id}`、`PUT /api/engines/order`、`PUT /api/engines/{id}/default`
- 分组:`GET/POST /api/groups`、`PUT/DELETE /api/groups/{id}`、`PUT /api/groups/order`
- 快捷方式:`GET/POST /api/shortcuts`、`PUT/DELETE /api/shortcuts/{id}`、`PUT /api/shortcuts/order`(支持跨组移动:body 带 group_id + sort)
- 媒体:`POST /api/media/upload`、`POST /api/media/fetch-favicon`、`POST /api/media/from-url`、`GET /api/media/{id}`
- 统一错误结构 `{code, message}`;未认证 401,越权 403,校验失败 400,限流 429。

## 7. 前端结构(对应 D8 / D10)

- 路由:`/`(首页)、`/login`、`/settings`(个人引擎与快捷方式管理)、`/admin`(ADMIN 后台)。
- 状态(Pinia):`authStore`、`engineStore`、`shortcutStore`、`themeStore`。
- 首页:中央 `SearchBar`(引擎下拉 + 输入框 + 按 URL 模板跳转;框头图标随选中引擎变化)、下方 `GroupGrid`(分组 + 快捷方式卡片,点击新标签打开)。响应式:桌面多列网格,移动单列。
- 设置/后台:Element Plus 表单与表格;桌面用 vue-draggable-plus 实现组内/组间/分组拖拽排序,移动端降级为「上移/下移/移动到分组」按钮。
- 主题:CSS 变量双主题(亮 `:root` / 暗 `html.dark`,手动切换);`themeStore` 持久化到 localStorage,首次按系统偏好初始化。
- axios 封装:`withCredentials` 携带 Cookie;响应 401 拦截并跳转登录。

### 7.1 前端首页实现约定(M6 敲定,已与用户确认)

- 视觉风格:采用 Apple HIG(用户选定)。设计令牌(字体/颜色/间距/圆角/动效)集中在 `src/style.css`,亮色 `:root`、暗色 `html.dark` 覆盖(手动切换,不用 media query)。
- UI 库取舍:M6 首页与登录页全部用原生 HIG 组件,**移除 `main.ts` 的 Element Plus 全局引入**(EP 默认视觉与 HIG 差异大,全局引入再逐一覆盖不划算)。收益:打包产物由含全量 EP 的 ~974KB 降至最大 chunk ~106KB(axios),提前达成「打包优化」目标。Element Plus 依赖保留在 `package.json`,M7 的设置/管理界面(表单/表格/拖拽对话框)按官方推荐(`unplugin-vue-components` + `ElementPlusResolver`)按需引入。
- CSRF:axios 配 `withCredentials` + `withXSRFToken` + 默认名 `XSRF-TOKEN` / `X-XSRF-TOKEN`,与后端 `CookieCsrfTokenRepository.withHttpOnlyFalse()` 对齐;应用启动经路由守卫调 `GET /api/auth/me` 触发后端下发 CSRF cookie,确保后续登录 POST 带得上 token。
- 401 策略:登录态探测(`/me`)与登录接口标 `skipAuthInterceptor`(其 401 是正常分支,不跳转);其余业务请求遇 401 视为会话失效,清登录态并跳登录页。拦截器内用动态 `import()` 取 router/authStore,打破 http→router→store→api 循环依赖。
- 搜索跳转:把引擎 `urlTemplate` 的 `{query}` 用 `encodeURIComponent(关键词)` 替换后 `window.open(_blank, noopener)`。
- 引擎图标:`iconBuiltin`(google/baidu/bing/duckduckgo)映射 `src/assets/engine-icons/*.svg`;`iconAssetId` 走 `GET /api/media/{id}`;均无则用名称首字母占位。
- 前端结构:`api/`(http + types + auth/engine/group/shortcut/media)、`stores/`(auth/theme/engine/shortcut)、`components/`(AppIcon/ThemeToggle/EngineIcon/SearchBar/ShortcutCard/GroupGrid)、`views/`(HomeView/LoginView)。

### 7.2 前端设置/管理实现约定(M7 敲定,已与用户确认)

- 引擎自定义图标(根因修复):prd 要求自定义引擎可配图标,但 M3 建引擎时 M4 媒体未就绪,致 `CreateEngineRequest`/`UpdateEngineRequest` 漏了 `iconAssetId`。M7 给二者补 `iconAssetId`(可空 UUID),`EngineService.create/update` 一并保存;`search_engines` 表已有 `icon_asset_id` 列,无需新迁移。引擎与快捷方式共用同一套三来源图标(上传 / 图片 URL / 抓 favicon)。
- 拖拽库:由原定 `vuedraggable@4.1.0` 改为 `vue-draggable-plus`。原因:vuedraggable 维护停滞,且与 Vite 8 有 CommonJS interop 报错史,TS 类型需手写;vue-draggable-plus 为 Vue 3 原生、内置 TS 类型与 typed events、现代 ESM,契合本项目 Vue 3.5 + Vite 8 + TS 6 栈。
- Element Plus 按需引入:装 `unplugin-auto-import` + `unplugin-vue-components`,`vite.config.ts` 用 `ElementPlusResolver` 自动引入用到的 EP 组件与样式(免全局注册、免手写 import),延续 M6「不全局引入 EP」的瘦身目标。
- 拖拽移动端降级(D10):触摸端把组内/组间拖拽降级为「上移 / 下移 / 移动到分组」按钮,排序结果同样走 reorder 接口持久化。
- 管理后台列用户接口(M7-3 敲定,根因修复):后端原管理接口只有开户 / 按 UUID 重置密码 / 签发邀请码,缺「列出用户」途径,导致重置密码 UI 拿不到用户 UUID 而无法落地。M7-3 补 `GET /api/admin/users`(返回 id / 用户名 / 角色 / 状态 / 创建时间),后台用表格展示并定位重置目标。已与用户确认。注:管理后台只读展示状态,「禁用 / 启用用户」与「主动失效会话」仍归 M8。

### 7.3 安全收尾实现约定(M8 敲定,已与用户确认)

- 即时注销落实:Spring Session 由默认仓库切到带索引的 `RedisIndexedSessionRepository`(`spring.session.redis.repository-type=indexed`),`AdminService` 注入 `FindByIndexNameSessionRepository`,按用户名 `findByPrincipalName` 查删会话。管理员**禁用用户**或**重置密码**后,立即失效该用户全部会话。新增 `POST /api/admin/users/{id}/status` 启用/禁用接口(§6 原缺、`UserStatus` 此前仅用于拒登,M8 补全),并禁止管理员禁用自己(400 `CANNOT_DISABLE_SELF`)。生产 Redis 若禁用 `CONFIG` 命令,需手动开启 keyspace notifications(主动删会话本身不依赖该通知)。
- 邀请码并发消费(防 TOCTOU):注册改为原子 `UPDATE invite_codes SET used_by=?, used_at=? WHERE id=? AND used_by IS NULL`,按受影响行数判定是否抢到;并发同码注册只会有一个成功,另一个回滚(400 `INVITE_CODE_USED`)。
- 图标归属校验:引擎 / 快捷方式写入 `iconAssetId` 时,经 `MediaService.assertOwned` 校验该媒体属于本人,不存在或越权一律 400 `ICON_ASSET_INVALID`(此前直接写库会触发外键异常变 500)。
- Cookie 安全:`COOKIE_SECURE` 生产 HTTPS 下必设为 `true`;`MEDIA_ALLOW_LOOPBACK` 生产保持 `false`。新增根目录 `README.md` 列全部环境变量、构建运行步骤与生产安全必读项;`.gitignore` 显式忽略构建产物使仓库自包含。

## 8. 安全与运维(对应 D9)

- 生产强制 HTTPS(反向代理终止 TLS,后端识别 `X-Forwarded-Proto`);Cookie `Secure`。
- CSRF:基于 Cookie 会话,启用 Spring Security CSRF token(或 `SameSite=Lax` + 自定义头校验)。
- SSRF:favicon / 图片 URL 抓取校验目标 host,禁私网与环回地址。
- 配置经环境变量注入(DB / Redis 连接、存储目录、Cookie 域、邀请码开关),严禁硬编码密钥。

## 9. 关键取舍

- 服务端 session(Redis) vs JWT:选前者,换取即时注销与限流便利,代价是有状态(可接受)。
- 预置引擎「按用户落库」vs「全局只读」:选按用户落库,换取用户可自由改/删/排序,代价是初始化写入若干行(可接受)。
- 图标内置 vs 全走 media:预置引擎图标内置,仅自定义图标入 `media_assets`,降低存储与抓取开销。

## 10. 兼容 / 迁移

- 全新项目,无存量迁移。Flyway `V1__init.sql` 建全部表。
- 存储抽象层为日后切 MinIO/OSS 预留:新增实现 + 配置开关即可,不改业务代码。
