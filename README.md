# 个人导航主页

多用户的个人导航起始页:每个用户独立管理自己的搜索引擎与快捷方式分组,支持自定义图标(上传 / 图片外链 / 抓取站点 favicon)、亮暗主题切换。采用受控开户(管理员后台开户,或凭有效邀请码自助注册),没有公开注册入口。

## 技术栈

- 后端:Spring Boot 3.5 / JDK 21 / Maven / Spring Security 6 / Spring Session(Redis)/ Spring Data JPA / Flyway / PostgreSQL 16 / Redis 7 / jsoup
- 前端:Vue 3 / Vite 8 / TypeScript / Element Plus(按需引入)/ Pinia / Vue Router / axios
- 测试:JUnit 5 + Spring Boot Test;集成测试用 Testcontainers 起临时 PostgreSQL + Redis(需本机 Docker)

## 目录结构

```
.
├── backend/    Spring Boot 后端(API + 数据层 + 安全)
└── frontend/   Vue 3 前端(首页 / 登录 / 注册 / 设置 / 管理后台)
```

## 环境要求

- JDK 21
- Node.js ≥ 20.19(Vite 8 要求)
- PostgreSQL 16、Redis 7(线上或本地实例)
- 跑后端集成测试需本机 Docker(Testcontainers 会自动起临时 PG + Redis,测完即销毁,不碰线上库)

## 后端

### 配置(全部经环境变量注入,无硬编码连接信息)

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/personal_nav` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | (空) | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `SERVER_PORT` | `8080` | 后端监听端口 |
| `SESSION_TTL_SECONDS` | `604800` | 会话有效期(秒,默认 7 天) |
| `SESSION_COOKIE_NAME` | `NAV_SESSION` | 会话 Cookie 名 |
| `COOKIE_SECURE` | `false` | 会话 Cookie 是否带 `Secure`,**生产 HTTPS 下必须设为 `true`** |
| `COOKIE_SAME_SITE` | `Lax` | 会话 Cookie 的 `SameSite` 属性 |
| `STORAGE_BASE_DIR` | `./data/media` | 媒体文件本地存储目录 |
| `MEDIA_UPLOAD_MAX_SIZE` | `2MB` | 单个上传文件大小上限 |
| `MEDIA_UPLOAD_MAX_REQUEST_SIZE` | `3MB` | 上传请求总大小上限 |
| `MEDIA_FETCH_MAX_BYTES` | `2097152` | 远程图标下载字节上限(2MB) |
| `MEDIA_CONNECT_TIMEOUT_MS` | `3000` | 抓取建立连接超时(毫秒) |
| `MEDIA_READ_TIMEOUT_MS` | `5000` | 抓取读取响应超时(毫秒) |
| `MEDIA_MAX_REDIRECTS` | `3` | 抓取最大重定向次数(每跳都重做 SSRF 校验) |
| `MEDIA_ALLOW_LOOPBACK` | `false` | 是否放行环回地址,**仅测试用,生产必须保持 `false`** |
| `APP_ADMIN_USERNAME` | (空) | 初始管理员用户名:仅当库中无任何 ADMIN 时,启动用这组凭据创建 |
| `APP_ADMIN_PASSWORD` | (空) | 初始管理员密码 |

### 本地运行

1. 准备 PostgreSQL 16 与 Redis 7 实例(表结构由 Flyway 在启动时自动建好,无需手动建表)。
2. 配置上表中至少 `DB_*`、`REDIS_*`,以及首次启动用的 `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`。
3. 启动(JDK 21):

   ```bash
   cd backend
   JAVA_HOME=<你的 JDK21 路径> ./mvnw spring-boot:run
   ```

   > macOS Homebrew 装的 JDK 21 通常在 `/usr/local/opt/openjdk@21`。请勿依赖 `/usr/libexec/java_home -v 21` 探测——系统默认是 Java 8 时它可能静默回退,导致编译报错。

### 测试(需 Docker)

```bash
cd backend
JAVA_HOME=<你的 JDK21 路径> ./mvnw test
```

Testcontainers 会自动起临时 PostgreSQL + Redis 跑全部集成测试,结束自动销毁。

### 打包

```bash
cd backend
JAVA_HOME=<你的 JDK21 路径> ./mvnw -DskipTests package   # 产物在 target/*.jar
```

## 前端

### 本地开发

```bash
cd frontend
npm install
npm run dev     # 监听 5173,/api 自动代理到 http://localhost:8080
```

### 构建

```bash
cd frontend
npm run build   # vue-tsc 类型检查 + vite 构建,产物在 dist/
```

生产部署:把 `dist/` 交由 Nginx 等静态服务器托管,并由其反向代理 `/api` 到后端、终止 HTTPS。前端不需要任何环境变量(接口走相对路径 `/api`)。

## 生产部署与安全(必读)

- **HTTPS**:由反向代理(Nginx 等)终止 TLS,并转发 `X-Forwarded-Proto` 等头。后端已配置 `server.forward-headers-strategy=framework`,会在可信边界统一解析真实协议与客户端 IP——**请务必把应用部署在可信代理之后**,不要直接对外暴露。
- **`COOKIE_SECURE=true`**:HTTPS 环境下必设,否则会话 Cookie 不带 `Secure`,有被明文链路窃取的风险。
- **`MEDIA_ALLOW_LOOPBACK=false`**:保持默认。该开关仅供本地测试抓取本机桩站点,生产放行会打开 SSRF 缺口。
- **强随机口令**:`APP_ADMIN_PASSWORD` 及数据库 / Redis 口令都用强随机值,且只经环境变量注入。
- **Redis keyspace notifications**:「管理员禁用用户 / 重置密码后即时踢下线」依赖带索引的 Spring Session 仓库(`spring.session.redis.repository-type=indexed`)。Spring Boot 启动时会尝试 `CONFIG SET notify-keyspace-events` 自动开启过期事件通知。若线上 Redis 禁用了 `CONFIG` 命令(部分云托管如此),请在 Redis 侧手动开启 `notify-keyspace-events Egx`;主动注销(删除会话)功能本身不依赖该通知,仍可正常工作。
- **媒体存储**:`STORAGE_BASE_DIR` 指向持久化卷,避免容器重启丢失已上传图标。

## 安全特性概览

- **会话**:登录态存服务端(Redis),Cookie 仅持不可猜的 sessionId;登录成功换发 sessionId 防会话固定;管理员禁用用户或重置密码后,立即失效该用户的全部会话。
- **CSRF**:基于 Cookie 会话的双提交令牌(`XSRF-TOKEN` Cookie + `X-XSRF-TOKEN` 请求头)。
- **密码**:BCrypt 哈希存储,不存明文;禁用账户(`DISABLED`)拒绝登录。
- **SSRF**:图标抓取仅放行公网 http/https,拒绝环回 / 私网 / 链路本地 / 多播地址,按解析出的全部 IP 判定,重定向逐跳重新校验,并有连接 / 读取超时与下载大小上限。
- **多租户隔离**:所有用户数据按 `user_id` 过滤,访问他人资源一律返回 404(不暴露存在性)。
- **限流**:登录、注册、开户接口按 IP 及 IP+用户名维度做 Redis 固定窗口限流,超阈值返回 429。
- **邀请码**:并发同码注册用「仅当未被使用时原子消费」杜绝重复消费(防 TOCTOU)。

## 开发说明

- 数据库迁移由 Flyway 管理(`backend/src/main/resources/db/migration`),**只增不改**:新增 `V{n}__*.sql`,不回写历史迁移。
- 预置引擎(Google / 百度 / Bing / DuckDuckGo)在用户创建时按用户落库,之后可自由增删改、排序、设默认。
