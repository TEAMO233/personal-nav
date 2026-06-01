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
- [ ] `StorageService` 接口 + `LocalDiskStorageService`;`media_assets` 落库;`GET /api/media/{id}`(校验归属)。
- [ ] 上传图片、从图片 URL 保存、favicon 抓取(jsoup + 回退 + SSRF 防护 + 超时/大小上限)。
- 验证:三种来源图标均能存并经 `/api/media/{id}` 显示;抓取内网地址被拒。

### M5 分组与快捷方式(R2 / R3 / D7)
- [ ] 分组 CRUD + 排序;快捷方式 CRUD + 排序 + 跨组移动接口(单事务原子更新 `sort_order` / `group_id`)。
- 验证:增删改、组内/组间移动、分组排序均持久化且按用户隔离。

### M6 前端首页(R1 / R2 / D8 / D10)
- [ ] `SearchBar`:引擎下拉(图标随选中变化)+ 输入 + 按 URL 模板跳转。
- [ ] `GroupGrid`:分组 + 快捷方式卡片,点击新标签打开;响应式布局。
- [ ] `authStore` + 登录页 + axios 401 拦截 + 主题切换。
- 验证:登录后首页展示本人引擎与快捷方式;切引擎图标变化、搜索跳转正确;暗/亮主题切换并记忆。

### M7 前端设置 / 管理(D7 / D8 / D10)
- [ ] 引擎管理、分组/快捷方式管理界面;桌面 vuedraggable 拖拽(组内/组间/分组),移动端按钮降级。
- [ ] 图标选择:上传 / 填图片 URL / 自动抓取 favicon。
- [ ] ADMIN 后台:用户开户、重置密码、邀请码。
- 验证:对照 prd.md 验收标准逐条勾选。

### M8 安全与收尾(D9)
- [ ] 复核 HTTPS / Cookie Secure / CSRF / SSRF / 限流;清理调试代码与未用依赖。
- [ ] README:环境变量、构建运行、对接 PostgreSQL/Redis(含 Testcontainers 测试)说明。
- 验证:全量验收标准复检;lint / test / build 通过。

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
