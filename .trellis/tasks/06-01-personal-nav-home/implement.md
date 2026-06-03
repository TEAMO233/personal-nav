# 个人导航主页 — 执行计划 (implement.md)

> 承接 prd.md(D1–D11)与 design.md。按里程碑从地基到表层推进,每步可独立验证。
> 注:`implement.jsonl` / `check.jsonl` 当前为模板示例——本项目 `.trellis/spec` 尚无真实 spec 可引用,进入 sub-agent 模式前再 curate;否则保持空。
> 数据库:开发期用 Testcontainers 起临时 PG+Redis 做集成测试(需本机 Docker),不碰线上库;线上 PG/Redis 连接方式由用户在代码完成后提供,仅经环境变量注入。

## 里程碑顺序

### M0 项目脚手架
- [x] 建 monorepo:`backend/`(Spring Initializr 选 Web, Security, Data JPA, Data Redis, Validation, Flyway, PostgreSQL Driver)、`frontend/`(Vite `vue-ts` 模板)。
- [x] `backend` 的 `application.yml`:PostgreSQL、Redis、存储目录、Cookie 配置(用环境变量占位)。
- [x] `backend` 引入 Testcontainers(PostgreSQL 模块 + Redis 用 GenericContainer)作测试依赖。
- [x] `frontend` 安装 Element Plus、Pinia、Vue Router、axios、vuedraggable。
- 验证:`cd backend && ./mvnw -q -DskipTests package` 通过;`cd frontend && npm run build` 通过;Testcontainers 能起临时 PG/Redis(需 Docker)、后端集成测试上下文加载成功;线上库连接代码完成后由用户提供,届时再真机联调。

### M1 数据层与迁移
- [ ] Flyway `V1__init.sql`:users / invite_codes / search_engines / shortcut_groups / shortcuts / media_assets + 索引。
- [ ] JPA 实体与 Repository。
- 验证:Testcontainers 起 PG 后 Flyway 成功建表;Repository CRUD 集成测试通过。

### M2 认证与会话(D1 / D2 / D9)
- [ ] Spring Security 配置:JSON 登录、session 存 Redis、Cookie(HttpOnly/Secure/SameSite)、CSRF、角色 USER/ADMIN。
- [ ] BCrypt 密码;`login` / `logout` / `me` 接口。
- [ ] 管理接口:开户、重置密码、邀请码;邀请码注册接口。
- [ ] Redis 登录/开户限流。
- 验证(Testcontainers PG+Redis 集成测试):登录拿到 Cookie;未登录访问受保护接口 401;越权 403;限流触发 429;管理员开户后新用户可登录。

### M3 引擎管理(R1 / R3 / D4)
- [ ] 新用户初始化预置 4 引擎(`is_preset`);引擎 CRUD + 排序 + 设默认接口,均按 `user_id` 隔离。
- [ ] 预置引擎图标内置静态资源。
- 验证:用户 A/B 引擎互不可见;增删改、排序、设默认均生效。

### M4 文件存储与图标(D5 / D6)
- [x] `StorageService` 接口 + `LocalDiskStorageService`;`media_assets` 落库;`GET /api/media/{id}`(校验归属)。
- [x] 上传图片、从图片 URL 保存、favicon 抓取(jsoup + 回退 + SSRF 防护 + 超时/大小上限)。
- 验证:三种来源图标均能存并经 `/api/media/{id}` 显示;抓取内网地址被拒。

### M5 分组与快捷方式(R2 / R3 / D7)
- [x] 分组 CRUD + 排序;快捷方式 CRUD + 排序 + 跨组移动接口(单事务原子更新 `sort_order` / `group_id`)。
- 验证:增删改、组内/组间移动、分组排序均持久化且按用户隔离。

### M6 前端首页(R1 / R2 / D8 / D10)
- [x] `SearchBar`:引擎下拉(图标随选中变化)+ 输入 + 按 URL 模板跳转。
- [x] `GroupGrid`:分组 + 快捷方式卡片,点击新标签打开;响应式布局。
- [x] `authStore` + 登录页 + axios 401 拦截 + 主题切换。
- 验证:登录后首页展示本人引擎与快捷方式;切引擎图标变化、搜索跳转正确;暗/亮主题切换并记忆。

### M7 前端设置 / 管理(D7 / D8 / D10)

> 体量较大,拆 3 个可独立验证的子提交(M7-1/2/3)。两处偏离原 design 并经用户确认,详见 design §7.2:
> (a) 后端给引擎 create/update 补 `iconAssetId`(根因修复:M3 时 media 未就绪,致请求体漏该字段;表已有列,无需新迁移)。
> (b) 拖拽库 `vuedraggable@4.1.0` → `vue-draggable-plus`(规避 Vite 8 下的 CommonJS interop 报错、TS 类型需手写)。

#### M7-1 工具链 + API/Store 补全 + 后端引擎图标
- [ ] 后端:`CreateEngineRequest`/`UpdateEngineRequest` 加 `iconAssetId`(可空);`EngineService.create/update` 保存;补/改测试。
- [ ] 前端工具链:装 `unplugin-auto-import` + `unplugin-vue-components`,`vite.config.ts` 配 `ElementPlusResolver` 按需引入;装 `vue-draggable-plus`、移除 `vuedraggable`。
- [ ] 前端 API:`engine/group/shortcut/media` 补齐写操作,新增 `admin.ts`(开户/重置密码/邀请码),`auth.ts` 加 `register`;`types.ts` 补相应类型。
- [ ] 前端 Store:`engineStore`/`shortcutStore` 补 CRUD action,新增 `groupStore`(或并入 shortcutStore)。
- 验证:`cd backend && ./mvnw -q test`(引擎图标新增用例通过);`cd frontend && npm run build`(按需引入生效、产物未因 EP 暴涨)。

#### M7-2 用户设置页 /settings
- [x] 引擎管理:列表 + 新增/编辑/删除 + 拖拽排序 + 设默认 + 图标选择。
- [x] 分组管理:新增/编辑/删除 + 拖拽排序。
- [x] 快捷方式管理:新增/编辑/删除 + 组内拖拽 + 组间拖拽(改 `groupId`);移动端降级为上移/下移/移动到分组按钮。
- [x] `IconPicker` 复用组件:三来源(上传 / 图片 URL / 抓 favicon),引擎与快捷方式共用。
- [x] 路由 `/settings` + HomeView 用户菜单加入口。
- 验证:对照 prd 验收(引擎增删改排序设默认、分组与快捷方式增删改、组内/组间拖拽持久化、三来源图标);`npm run build`。

#### M7-3 ADMIN 后台 + 邀请码注册
> 偏离 design §6 并经用户确认:后端补 `GET /api/admin/users` 列用户接口(根因修复——重置密码需 UUID 却无列用户途径)。详见 design §7.2「M7-3 敲定」。
- [x] 后端:新增 `GET /api/admin/users`(列出用户:id/用户名/角色/状态/创建时间)+ `AdminService.listUsers` + 集成测试(管理员列出 / 普通用户 403)。
- [x] `/admin`(仅 ADMIN,路由守卫限角色):用户表格 + 开户、重置密码、签发邀请码。
- [x] `/register` 邀请码注册页:邀请码 + 用户名 + 密码,成功后引导登录。
- [x] HomeView 用户菜单:ADMIN 显示「管理后台」入口。
- 验证:ADMIN 可开户/重置/发码;非 ADMIN 访问 `/admin` 被挡;邀请码注册成功后可登录;后端 `./mvnw test`、前端 `npm run build`。

### M8 安全与收尾(D9)
- [x] 复核 HTTPS / Cookie Secure / CSRF / SSRF / 限流;清理调试代码与未用依赖。
- [x] README:环境变量、构建运行、对接 PostgreSQL/Redis(含 Testcontainers 测试)说明。
- 验证:全量验收标准复检;lint / test / build 通过。(已完成:后端 55 测试全绿、前端 build 通过)

### M9 搜索历史(用户验收期间新增)
- [x] 后端:`V3__create_search_history.sql` + `com.nav.search`(实体/Repository/Service/Controller/DTO),按 user_id 隔离的列出 / 记录(去重置顶)/ 删除 / 清空。
- [x] 前端:`searchHistoryStore` + `api/searchHistory`;SearchBar 聚焦展示历史、点击直接搜、单条删除、一键清空;AppIcon 加 clock;HomeView 并发加载 + 登出 reset。
- 验证:后端 `./mvnw test`(新增 SearchHistoryIntegrationTest);前端 `npm run build`;重启后端跑 V3 迁移后真机验收。

## 验证命令

- 后端:`cd backend && ./mvnw -q test`(集成测试经 Testcontainers 自动起 PG+Redis,需 Docker 守护进程在运行)、`./mvnw -q -DskipTests package`
- 前端:`cd frontend && npm run lint && npm run build`
- 开发期依赖:本机 Docker(供 Testcontainers 用);无需手动起库,容器随测试生命周期自动创建销毁。
- 线上联调:PG/Redis 连接方式由用户在代码完成后提供,经环境变量注入后做真机验收。

## 风险点 / 回滚

- Spring Security 6 的 session / CSRF / SameSite 配置易错 → M2 单独验证,出问题回滚到最小登录态配置再逐项加固。
- favicon 抓取的 SSRF 与超时 → M4 先做 host 白名单/私网黑名单 + 超时,再开放功能。
- 跨组拖拽的 `sort_order` 一致性 → 后端单事务批量更新;前端乐观更新失败则回滚 UI。
- 无真实库做开发期联调 → 用 Testcontainers 保证数据层/认证逻辑正确;最终以用户线上 PG/Redis 做真机验收,留意线上版本与容器镜像版本保持一致。
- 回滚点:每个 Mx 为独立提交;Flyway 迁移只增不改(新增迁移文件),不回写历史。

## start 前检查

- [ ] 用户已 review `prd.md` / `design.md` / `implement.md`。
- [ ] `implement.jsonl` / `check.jsonl`:无真实 spec 可引用时保持空(删除示例行)或留待生成 spec 后补。
- [ ] 开发期数据库验证用 Testcontainers(已确认),本机需有 Docker;线上 PG/Redis 连接方式由用户在代码完成后提供。
