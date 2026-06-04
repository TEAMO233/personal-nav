# Personal Navigation Homepage / 个人导航主页

一个面向多用户场景的个人导航起始页。每个用户都拥有独立的搜索引擎配置、快捷方式分组和首页工作台，支持自定义图标、亮暗主题切换，以及受控开户与邀请码注册。

这个项目不是“公开注册的导航站模板”，而是一套更适合个人部署、小团队内部使用、家庭共享或私有化托管的导航首页系统。

## 项目特性

- 多用户隔离：每个用户拥有独立的搜索引擎、快捷方式、首页书签、待办、便签、搜索历史和最近访问记录。
- 受控注册：不提供公开注册入口，只支持管理员后台开户，或使用有效邀请码自助注册。
- 首页工作台：包含搜索区、精选快捷方式、分类导航、首页书签、概览面板、待办日程、便签等模块。
- 搜索引擎管理：支持新增、编辑、删除、排序、设默认，用户初始化时自动带预置引擎。
- 快捷方式管理：支持分组、排序、自定义图标、首页展示。
- 图标能力：支持上传图片、保存远程图片地址、抓取站点 favicon，并带基础安全限制。
- 管理后台：支持用户列表、开户、重置密码、启用/禁用用户、签发邀请码。
- 服务端会话：基于 Spring Session + Redis 存储登录态，管理员禁用用户或重置密码后可即时踢下线。
- 安全防护：内置 CSRF、防会话固定、限流、SSRF 防护、多租户数据隔离。

## 当前已实现的页面

- `/login`：登录
- `/register`：邀请码注册
- `/`：首页工作台
- `/settings`：搜索引擎、快捷方式、首页书签管理
- `/admin`：管理员后台

## 技术栈

### 后端

- Spring Boot 3.5.14
- JDK 21
- Spring Security 6
- Spring Session (Redis)
- Spring Data JPA
- Flyway
- PostgreSQL 16
- Redis 7
- jsoup

### 前端

- Vue 3
- Vite 8
- TypeScript
- Element Plus
- Pinia
- Vue Router
- axios

### 测试

- JUnit 5
- Spring Boot Test
- Testcontainers

## 仓库结构

```text
.
├── backend/    Spring Boot 后端（认证 / 管理 / 数据层 / 安全）
└── frontend/   Vue 3 前端（首页 / 登录 / 注册 / 设置 / 管理后台）
```

## 适用场景

- 自己部署一个可登录、可管理、可扩展的导航首页
- 给家庭成员或小团队提供独立导航面板
- 需要管理员控制开户，不希望任何人都能公开注册
- 想在导航页上聚合常用搜索、收藏、待办、便签等轻量个人信息

## 快速开始

### 环境要求

- JDK 21
- Node.js 20.19 及以上
- PostgreSQL 16
- Redis 7
- Docker（仅后端集成测试需要）

### 1. 启动后端

先准备 PostgreSQL 和 Redis，再设置至少以下环境变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`

启动命令：

```bash
cd backend
JAVA_HOME=<你的 JDK21 路径> ./mvnw spring-boot:run
```

后端默认监听 `http://localhost:8080`。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，开发环境下 `/api` 会自动代理到 `http://localhost:8080`。

### 3. 首次登录

如果数据库里还没有任何 `ADMIN` 用户，后端启动时会使用：

- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`

自动创建初始管理员。之后可使用该账号登录后台并继续开户或签发邀请码。

## 配置说明

后端配置全部通过环境变量注入，不在代码中硬编码连接信息。

### 常用后端环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/personal_nav` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `SERVER_PORT` | `8080` | 后端端口 |
| `SESSION_TTL_SECONDS` | `604800` | 会话有效期，默认 7 天 |
| `SESSION_COOKIE_NAME` | `NAV_SESSION` | 会话 Cookie 名称 |
| `COOKIE_SECURE` | `false` | HTTPS 生产环境必须设为 `true` |
| `COOKIE_SAME_SITE` | `Lax` | 会话 Cookie 的 SameSite 属性 |
| `STORAGE_BASE_DIR` | `./data/media` | 本地图标/媒体存储目录 |
| `MEDIA_UPLOAD_MAX_SIZE` | `2MB` | 单个上传文件大小上限 |
| `MEDIA_UPLOAD_MAX_REQUEST_SIZE` | `3MB` | 上传请求总大小上限 |
| `MEDIA_FETCH_MAX_BYTES` | `2097152` | 远程抓取最大字节数 |
| `MEDIA_CONNECT_TIMEOUT_MS` | `3000` | 远程抓取连接超时 |
| `MEDIA_READ_TIMEOUT_MS` | `5000` | 远程抓取读取超时 |
| `MEDIA_MAX_REDIRECTS` | `3` | 远程抓取最大重定向次数 |
| `MEDIA_ALLOW_LOOPBACK` | `false` | 仅测试可开启，生产必须保持 `false` |
| `APP_ADMIN_USERNAME` | 空 | 初始管理员用户名 |
| `APP_ADMIN_PASSWORD` | 空 | 初始管理员密码 |

## 本地开发

### 后端测试

```bash
cd backend
JAVA_HOME=<你的 JDK21 路径> ./mvnw test
```

集成测试会通过 Testcontainers 拉起临时 PostgreSQL 与 Redis，测试结束后自动销毁。

### 前端构建

```bash
cd frontend
npm run build
```

该命令会先执行 `vue-tsc` 类型检查，再执行 Vite 构建。

### 后端打包

```bash
cd backend
JAVA_HOME=<你的 JDK21 路径> ./mvnw -DskipTests package
```

产物位于 `backend/target/*.jar`。

## 安全设计

- 服务端会话：登录态存 Redis，浏览器 Cookie 仅保存 session id。
- 防会话固定：登录成功后会重新签发 session id。
- CSRF 防护：使用 Cookie + Header 的双提交令牌方案。
- 密码存储：使用 BCrypt 哈希，不保存明文密码。
- 管理员干预：管理员禁用用户或重置密码后，可立即让该用户全部会话失效。
- SSRF 防护：图标抓取仅允许公网 `http/https`，拒绝环回、私网、链路本地、多播地址，并限制超时、大小和重定向次数。
- 多租户隔离：用户数据按 `user_id` 隔离，访问他人资源返回 404，不暴露存在性。
- 限流：对登录、注册、开户等接口按 IP 或 IP+用户名维度限流。

## 生产部署建议

- 使用 Nginx、Caddy 或其他可信反向代理终止 HTTPS。
- 在 HTTPS 环境下将 `COOKIE_SECURE=true`。
- `STORAGE_BASE_DIR` 指向持久化卷，避免容器重启后图标丢失。
- 如果 Redis 禁用了 `CONFIG SET`，请手动开启 `notify-keyspace-events Egx`，以确保部分会话失效联动能力正常工作。
- 不要直接将应用裸露在公网入口后面，建议始终放在可信代理之后。

## 开发约束

- 数据库迁移由 Flyway 管理，只新增 `V{n}__*.sql`，不修改历史迁移。
- 前端接口统一走相对路径 `/api`，方便反向代理部署。
- 预置搜索引擎会在用户创建时写入数据库，用户之后可自行调整。

## 开源前建议检查

如果你准备把这个仓库公开到 GitHub，建议至少再确认以下几项：

- 是否补充 `LICENSE`
- 是否需要 `CONTRIBUTING.md`
- 是否需要 `SECURITY.md`
- 是否确认仓库内不存在真实密钥、内网地址、生产账号或调试残留
- 是否补充项目截图、演示 GIF 或部署示意图

## License

当前仓库尚未声明开源许可证。公开发布前，建议补充一个明确的 `LICENSE` 文件。
