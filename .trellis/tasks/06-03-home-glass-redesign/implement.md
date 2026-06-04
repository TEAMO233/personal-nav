# 首页苹果风玻璃拟态像素级还原 — 执行计划

> 配合 `prd.md` + `design.md`。本计划先补真实数据能力，再进行像素级首页整合；避免用假数据把 UI 先填满。

## Ordered Checklist

1. **快捷入口建模（已定）**：扩展现有 `Shortcut`，新增首页精选与展示字段，不新增独立首页入口表。
2. **后端迁移**：新增 Flyway `V4__home_dashboard_resources.sql`，包含待办、便签、最近访问、首页书签；如选扩展 Shortcut，则同时加 featured/description/accent 字段。
3. **后端待办模块**：Entity / Repository / DTO / Service / Controller，实现列表、新建、更新、完成状态、删除和用户隔离。
4. **后端便签模块**：Entity / Repository / DTO / Service / Controller，实现列表、新建、更新、删除和用户隔离。
5. **后端最近访问模块**：Entity / Repository / DTO / Service / Controller，实现记录访问、列表、可选删除/清空；记录时校验 shortcut 归属。
6. **后端首页书签模块（新增变更）**：Entity / Repository / DTO / Service / Controller，实现列表、新建、更新、删除、启用过滤、排序和用户隔离。
7. **后端通知模块**：Entity / Repository / DTO / Service / Controller，实现列表、未读数、标记已读、全部已读；逾期待办同步生成 `TODO_OVERDUE` 通知。
8. **后端概览聚合**：优先前端聚合；如实现后端 summary，则加 dashboard service/controller。
9. **后端测试**：为新增资源补集成测试，覆盖 CRUD、跨用户不可见、越权返回、最近访问记录、首页书签配置、逾期待办生成通知。
10. **前端类型/API**：更新 `types.ts`，新增 `todo.ts`、`note.ts`、`recentVisit.ts`、`homeBookmark.ts`、`notification.ts`，按后端 DTO 对齐。
11. **前端 stores**：新增 `todo`、`note`、`recentVisit`、`homeBookmark`、`notification` stores；更新 `shortcut` store 的 featured 派生数据。
12. **首页组件拆分**：新增 `FeaturedShortcutGrid`、`DashboardGrid`、`HomeBookmarkPanel`、`CategoryPanel`、`OverviewPanel`、`TodoSchedulePanel`、`NotePanel`、`NotificationMenu`。
13. **设置页接入（新增变更）**：`/settings` 新增“首页书签”标签页，支持配置图标、链接、名称等基础信息。
14. **像素级视觉调校**：重做 `HomeView`、`HomeTopbar`、`HomeHero`、`SearchBar`、`DynamicBackground` 的尺寸、间距、圆角、背景流线和玻璃材质。
15. **交互接线**：快捷入口和首页书签点击记录最近访问；待办支持完成/添加；逾期待办进入通知；便签支持展示/编辑或新增；搜索功能零回归。
16. **响应式**：实现 >=1400、1024-1399、768-1023、<768 四档布局。
17. **构建与测试**：跑后端测试、前端 build。
18. **Browser 验证**：启动本地服务，在 1672x941 和移动视口截图检查；按截图继续微调。

## Validation Commands

后端：

```bash
cd backend
./mvnw test
```

前端：

```bash
cd frontend
npm run build
npm run dev
```

Browser/Playwright 手动验证：

- 桌面：1672x941，对比参考图。
- 移动：390x844，确认无横向滚动、重叠、文字溢出。

## Review Gates

- PRD 中开放问题关闭后再继续实现。
- 新增后端表/字段必须有测试覆盖。
- 首页不得出现 mock/fallback 假数据。
- 截图中的模块如果暂时没有真实能力，必须在当前任务中补能力或从第一屏移除；本任务目标是补能力。

## Risk And Rollback Points

- **范围膨胀**：待办/便签只做首页可用 + 基础 CRUD，不做大型管理系统。
- **快捷入口模型重复**：优先复用 `Shortcut`，避免 `home_entries` 与 `shortcuts` 双系统。
- **Flyway 兼容**：新增字段 nullable/default，避免破坏旧数据。
- **视觉调校耗时**：先完成真实数据链路，再用截图迭代细调。
- **点击记录访问阻塞**：最近访问记录应 fire-and-forget，不阻塞打开链接。
- **全局快捷键泄漏**：SearchBar 卸载时必须解绑。

## Planning Blockers

- None. 用户已确认快捷入口、今日完成率、首页内联管理、真实通知与逾期通知生命周期。
