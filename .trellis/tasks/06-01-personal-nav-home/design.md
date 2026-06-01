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
- 管理(ADMIN):`POST /api/admin/users`、`POST /api/admin/users/{id}/reset-password`、`POST /api/admin/invite-codes`
- 引擎:`GET/POST /api/engines`、`PUT/DELETE /api/engines/{id}`、`PUT /api/engines/order`、`PUT /api/engines/{id}/default`
- 分组:`GET/POST /api/groups`、`PUT/DELETE /api/groups/{id}`、`PUT /api/groups/order`
- 快捷方式:`GET/POST /api/shortcuts`、`PUT/DELETE /api/shortcuts/{id}`、`PUT /api/shortcuts/order`(支持跨组移动:body 带 group_id + sort)
- 媒体:`POST /api/media/upload`、`POST /api/media/fetch-favicon`、`POST /api/media/from-url`、`GET /api/media/{id}`
- 统一错误结构 `{code, message}`;未认证 401,越权 403,校验失败 400,限流 429。

## 7. 前端结构(对应 D8 / D10)

- 路由:`/`(首页)、`/login`、`/settings`(个人引擎与快捷方式管理)、`/admin`(ADMIN 后台)。
- 状态(Pinia):`authStore`、`engineStore`、`shortcutStore`、`themeStore`。
- 首页:中央 `SearchBar`(引擎下拉 + 输入框 + 按 URL 模板跳转;框头图标随选中引擎变化)、下方 `GroupGrid`(分组 + 快捷方式卡片,点击新标签打开)。响应式:桌面多列网格,移动单列。
- 设置/后台:Element Plus 表单与表格;桌面用 vuedraggable 实现组内/组间/分组拖拽排序,移动端降级为「上移/下移/移动到分组」按钮。
- 主题:Element Plus 暗色方案 + CSS 变量;`themeStore` 持久化到 localStorage,首次按系统偏好初始化。
- axios 封装:`withCredentials` 携带 Cookie;响应 401 拦截并跳转登录。

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
