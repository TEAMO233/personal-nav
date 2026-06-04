# 首页苹果风玻璃拟态像素级还原 — 技术设计

> 配合 `prd.md` 阅读。本次范围为跨前后端：像素级还原参考图，同时把截图中的缺失模块补成真实持久化能力。

## 1. Architecture And Boundaries

- 路由仍以 `/` -> `HomeView.vue` 为首页入口。
- 首页专属组件放在 `frontend/src/components/home/`；共享组件仍放在 `frontend/src/components/` 根目录。
- 数据流统一为：Vue component -> Pinia store -> `src/api/*.ts` -> Spring Controller -> Service -> Repository -> DB。
- 新增后端资源全部按当前项目范式实现：
  - Entity extends `BaseEntity`。
  - `user_id` 用户隔离。
  - Repository 通过 `findByIdAndUserId` 防越权。
  - Service 负责归属校验和业务排序。
  - Controller 使用 `SecurityUtils.currentUserId()`。
  - DTO record 使用 jakarta validation。
  - Flyway 新增 `V4__home_dashboard_resources.sql`。
  - Testcontainers 集成测试覆盖 CRUD 和用户隔离。

## 2. Backend Resources

### 2.1 Todo / Schedule

建议包名：`com.nav.dashboard.todo` 或 `com.nav.todo`。为避免过度嵌套，推荐 `com.nav.todo`。

表：`todos`

| column | type | notes |
| --- | --- | --- |
| `id` | UUID | primary key |
| `user_id` | UUID | references users(id), required |
| `title` | varchar(160) | required |
| `tag` | varchar(32) | optional; screenshot: 工作/学习 |
| `scheduled_at` | timestamptz | optional; screenshot time |
| `done` | boolean | default false |
| `sort_order` | integer | default 0 |
| `created_at` | timestamptz | inherited pattern |
| `updated_at` | timestamptz | optional; if not adding globally, skip |

接口：

- `GET /api/todos?limit=3`：首页取最近待办/日程。
- `POST /api/todos`：新建。
- `PUT /api/todos/{id}`：更新标题、标签、计划时间、完成状态。
- `DELETE /api/todos/{id}`：删除。

首页展示规则：

- 默认按 `done ASC, scheduled_at ASC NULLS LAST, sort_order ASC, created_at DESC`。
- 首页展示 3 条，与截图一致。
- “添加待办”以内联输入/轻量弹层创建；不做独立管理页。首页内支持完成/取消完成、删除，更新可按实现复杂度做轻量编辑。

### 2.2 Notes

推荐包名：`com.nav.note`。

表：`notes`

| column | type | notes |
| --- | --- | --- |
| `id` | UUID | primary key |
| `user_id` | UUID | references users(id), required |
| `content` | text | required |
| `pinned` | boolean | default false |
| `sort_order` | integer | default 0 |
| `created_at` | timestamptz | required |
| `updated_at` | timestamptz | optional |

接口：

- `GET /api/notes?limit=6`：首页优先取置顶/最新便签列表。
- `POST /api/notes`：新建。
- `PUT /api/notes/{id}`：更新内容/置顶。
- `DELETE /api/notes/{id}`：删除。

首页展示规则：

- 优先 `pinned DESC, sort_order ASC, created_at DESC`。
- 首页显示便签列表；“星标/固定”对应 `pinned`，列表按置顶和创建时间排序。
- 只做首页内联新增、编辑、删除单条便签，不做独立便签管理页。

### 2.3 Recent Visits

推荐包名：`com.nav.visit`。

表：`recent_visits`

| column | type | notes |
| --- | --- | --- |
| `id` | UUID | primary key |
| `user_id` | UUID | references users(id), required |
| `shortcut_id` | UUID | nullable references shortcuts(id) |
| `name` | varchar(96) | snapshot, required |
| `url` | varchar(2048) | required |
| `domain` | varchar(255) | derived/snapshot |
| `visited_at` | timestamptz | required |
| `created_at` | timestamptz | required |

接口：

- `GET /api/recent-visits?limit=6`：查询真实访问日志；首页主面板已改为 `home_bookmarks`。
- `POST /api/recent-visits`：记录一次访问，body 可传 `shortcutId` 或 `name/url`。
- `DELETE /api/recent-visits/{id}`：可选，方便清理。
- `DELETE /api/recent-visits`：可选，清空。

记录方式：

- 前端点击快捷入口/快捷方式时先 fire-and-forget 调 `recordVisit`，再打开链接。
- 如果传 `shortcutId`，后端必须校验 shortcut 属于当前用户，并用数据库中的 name/url 生成记录。
- 列表只返回当前用户最近 6 条。

### 2.3.1 Home Bookmarks

推荐包名：`com.nav.bookmark`。

表：`home_bookmarks`

| column | type | notes |
| --- | --- | --- |
| `id` | UUID | primary key |
| `user_id` | UUID | references users(id), required |
| `name` | varchar(96) | required |
| `url` | varchar(2048) | required |
| `description` | varchar(160) | optional; shown as subtitle |
| `icon_asset_id` | UUID | optional references media_assets(id) |
| `enabled` | boolean | default true |
| `sort_order` | integer | default 0 |
| `created_at` | timestamptz | required |

接口：

- `GET /api/home-bookmarks?enabledOnly=true`：首页取启用书签。
- `GET /api/home-bookmarks?enabledOnly=false`：设置页取全部书签。
- `POST /api/home-bookmarks`：新建。
- `PUT /api/home-bookmarks/{id}`：更新名称、链接、说明、图标、启用状态。
- `PUT /api/home-bookmarks/order`：按完整 id 列表排序。
- `DELETE /api/home-bookmarks/{id}`：删除。

展示与配置：

- 首页底部左侧面板由原“最近访问”改为“首页书签”。
- `/settings` 新增“首页书签”标签页，支持新增、编辑、删除、启用/隐藏、上移/下移和图标选择。
- 书签点击打开链接，同时可 fire-and-forget 写入 `recent_visits`，保留真实访问日志能力。

### 2.4 Dashboard Overview

不强制新增单独表，优先用现有资源聚合：

- 待办事项：`todos.done=false` 数量。
- 日程安排：`scheduled_at` 非空且未来/今日范围数量，具体算法实现时写明。
- 系统状态：首页必要接口均加载成功则前端显示“正常”；如果要后端聚合，可新增 `GET /api/dashboard/summary`。
- 今日完成率（已定）：用今天相关待办中已完成数量 / 今天相关待办总数计算。若今天没有相关待办，显示 0% 或空状态文案，不能写死 75%。不新增专注时长/手动专注度记录。

### 2.5 Notifications

推荐包名：`com.nav.notification`。

表：`notifications`

| column | type | notes |
| --- | --- | --- |
| `id` | UUID | primary key |
| `user_id` | UUID | references users(id), required |
| `type` | varchar(32) | e.g. `TODO_OVERDUE` |
| `source_id` | UUID | nullable; overdue todo id |
| `title` | varchar(160) | required |
| `content` | varchar(512) | optional |
| `read` | boolean | default false |
| `resolved` | boolean | default false; lifecycle decision pending |
| `notified_at` | timestamptz | required |
| `created_at` | timestamptz | required |

接口：

- `GET /api/notifications?limit=10`：列出当前用户通知。
- `GET /api/notifications/unread-count`：未读数。
- `PUT /api/notifications/{id}/read`：标记单条已读。
- `PUT /api/notifications/read-all`：全部已读。

逾期待办生成规则：

- NotificationService 在 `list` / `unread-count` 前执行一次同步：查找当前用户 `done=false` 且 `scheduled_at < now()` 的待办。
- 对每个逾期待办生成一条 `TODO_OVERDUE` 通知。
- 用唯一约束 `(user_id, type, source_id)` 防止同一待办重复生成多条超时通知。
- 待办完成后，NotificationService 自动把对应 `TODO_OVERDUE` 标记为 `resolved=true`、`read=true`，从未读数和默认通知列表隐藏。

## 3. Shortcut / Quick Entry Modeling

截图中的 8 个快捷入口需要真实可点击/可持久化。

已定方案：扩展现有 `shortcuts`，新增 `description`、`icon_key`/`accent`、`featured`、`featured_order` 字段。首页顶部只取 `featured=true` 的前 8 个；设置页继续管理快捷方式，后续可补 featured 开关。

候选方案记录：

- 方案 A（已定）：扩展现有 `shortcuts`，新增 `description`、`icon_key`/`accent`、`featured`、`featured_order` 字段。首页顶部只取 `featured=true` 的前 8 个；设置页继续管理快捷方式，后续可补 featured 开关。
- 方案 B：新增独立 `home_entries` 表，字段专为首页卡片设计。优点是像素还原自由度高；缺点是与现有快捷方式模型重复，会让“导航资源”出现两套管理路径。
- 方案 C：8 个固定模块入口，不走快捷方式。优点是快；缺点是大部分模块没有真实功能，且违背“不能假功能”。

选择方案 A 的原因：复用现有导航资源和设置页，不制造第二套链接系统；为像素级展示补必要字段。

## 4. Frontend Component Plan

### `HomeView.vue`

编排顺序按截图：

1. `DynamicBackground`
2. `HomeTopbar`
3. `HomeHero`
4. `FeaturedShortcutGrid`
5. `DashboardGrid`

`HomeView` 入场并发加载：

- engines
- groups/shortcuts
- search history
- home bookmarks
- todos
- notes

### Existing / Updated Components

| component | changes |
| --- | --- |
| `DynamicBackground.vue` | 背景从简单光斑升级为截图里的深蓝弧形流线 + 径向柔光，纯 CSS |
| `HomeTopbar.vue` | 左侧补蓝色圆角 logo；右侧增加搜索/通知/主题/用户胶囊，贴近截图 |
| `HomeHero.vue` | 欢迎语、副标题、SearchBar 的尺寸和间距按截图 |
| `SearchBar.vue` | 搜索栏从引擎型外观调整为截图外观；保留引擎逻辑可作为左侧/下拉增强 |
| `ShortcutCard.vue` | 如果采用方案 A，支持 description/accent/featured 展示 |
| `GroupGrid.vue` | 可能降级为设置页/下方备用区域；首页第一屏优先展示 8 个 featured |

### New Home Components

| component | role |
| --- | --- |
| `FeaturedShortcutGrid.vue` | 8 个顶部快捷入口，一行 8 列，响应式降列 |
| `HomeBookmarkPanel.vue` | 首页书签列表，展示 `/settings` 配置的启用项 |
| `CategoryPanel.vue` | 常用分类，展示真实分组 + count |
| `OverviewPanel.vue` | 今日完成率/待办数/日程数/系统状态 |
| `TodoSchedulePanel.vue` | 待办 / 日程 3 条 + 添加待办 |
| `NotePanel.vue` | 灵感便签 |
| `DashboardGrid.vue` | 底部 widget grid 总编排 |
| `NotificationMenu.vue` | 顶栏铃铛下拉，展示未读 badge 和通知列表 |

### Stores / API

新增：

- `frontend/src/api/todo.ts`
- `frontend/src/api/note.ts`
- `frontend/src/api/recentVisit.ts`
- `frontend/src/api/homeBookmark.ts`
- `frontend/src/api/notification.ts`
- `frontend/src/stores/todo.ts`
- `frontend/src/stores/note.ts`
- `frontend/src/stores/recentVisit.ts`
- `frontend/src/stores/homeBookmark.ts`
- `frontend/src/stores/notification.ts`

更新：

- `frontend/src/api/types.ts`：增加 `TodoItem`、`NoteItem`、`RecentVisit`、`HomeBookmark`、`NotificationItem`，并按最终决策扩展 `Shortcut`。
- `frontend/src/stores/shortcut.ts`：若采用 featured 扩展，增加 featured 派生列表。

## 5. Visual System

继续使用项目 CSS token，并扩展首页专属 token：

- `--home-shell-max-width`: 1560px 左右。
- `--home-bg`: 深蓝基础背景。
- `--home-arc-*`: 流线光效。
- `--glass-bg`、`--glass-border`、`--glass-highlight`、`--glass-shadow`。
- `--accent-grad`: 蓝色按钮渐变。

桌面第一屏目标：

- Header：约 64px。
- Hero top spacing：约 48-70px，标题 42px 级别。
- Search：约 800px 宽、64px 高。
- Featured cards：8 列，单卡约 176x154 或按容器宽等比。
- Dashboard：4 列主 grid，左两列/中/右结构接近截图。

## 6. Compatibility And Risk

- Flyway 迁移需向后兼容已有数据，新增字段必须 nullable 或有 default。
- 最近访问记录点击时不能阻塞打开链接；失败静默或轻提示即可。
- 首页新增多接口后需要统一加载态，避免局部闪烁；可用 Promise.all 并保留错误重试。
- `Cmd/Ctrl+K` 全局监听必须在组件卸载时解绑。
- 视觉调校必须通过 Browser/Playwright 截图核对，至少桌面 1672x941 和移动 390x844。

## 7. Rollback Shape

- 后端新增表可通过 Flyway 版本隔离；若回滚代码但数据库保留新增表，不影响旧功能。
- 前端新 store/API 与首页组件独立；现有设置页和认证页不应被改动。
- 扩展 `shortcuts` 字段若采用 nullable/default，不影响旧数据读取。
