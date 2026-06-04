# 完善待办日程功能

## Goal

Make the existing "待办 / 日程" and "今日概览 > 日程安排" home widgets useful and internally consistent. Today the UI mostly behaves like a todo list, while the overview already exposes a schedule count that users cannot conveniently create or manage from the home page.

## Confirmed Facts

- Frontend stack is Vue 3 + TypeScript + Pinia + plain CSS.
- Home desktop dashboard renders `TodoSchedulePanel.vue` and `OverviewPanel.vue` through `DashboardGrid.vue`.
- `OverviewPanel.vue` shows:
  - `todoStore.pendingCount` as "待办事项"
  - `todoStore.scheduleCount` as "日程安排"
  - `todoStore.todayCompletionRate` as "今日完成率"
- `todoStore.scheduleCount` is computed from existing todos: unfinished items with a non-null `scheduledAt`.
- `todoStore.todayItems` treats an item as today-related when `scheduledAt` is today, or when `scheduledAt` is null and `createdAt` is today.
- `TodoSchedulePanel.vue` currently only has a title-only add form and calls `todoStore.create({ title, tag: '工作' })`, so home-created items never get `scheduledAt`.
- `TodoSchedulePanel.vue` displays `scheduledAt` when present, but hides time and tag on small screens.
- Frontend API/types already include `TodoInput.scheduledAt?: string | null` and `TodoItem.scheduledAt: string | null`.
- Backend already supports `scheduledAt` in `CreateTodoRequest`, `UpdateTodoRequest`, `Todo`, and `TodoResponse`.
- Database table `todos` already has `scheduled_at TIMESTAMPTZ` and comments describe it as "待办 / 日程".
- Backend sorting is unfinished first, scheduled time ascending, sort order ascending, created time descending.
- Backend notification logic uses overdue unfinished todos with `scheduledAt` to create/resolve `TODO_OVERDUE` notifications.
- No separate `schedules` table, API, or frontend store exists today.

## Problem

The product labels imply two concepts, "待办" and "日程", but the current user-facing creation flow only supports plain todos. As a result:

- users cannot create a scheduled item from the home panel;
- the "日程安排" number in the overview can remain unused or confusing;
- the "待办 / 日程" panel title over-promises compared with its controls.

## Requirements Draft

- Reuse the existing real todo API and model schedules as todos with optional `scheduledAt`; do not introduce a separate schedule entity/API for this MVP.
- Let users create an item with an optional scheduled date+time from the home "待办 / 日程" panel; date-only schedules are not supported in this MVP.
- Let users lightly edit existing items from the home panel to add, change, or clear `scheduledAt`.
- Use an inline expanded editor inside the home panel for create/edit, matching the lightweight home-panel pattern used by `NotePanel.vue`.
- Make scheduled items visibly distinct enough that the overview "日程安排" count feels connected to the panel.
- Keep scheduled items and unscheduled todos in one mixed list, but visually distinguish scheduled items with time/schedule styling.
- Preserve existing todo CRUD behavior, completion toggling, deletion, overdue notification behavior, and per-user isolation.
- Avoid fake dashboard metrics or placeholder schedule data.
- Keep implementation aligned with existing Vue/Pinia/plain-CSS patterns and `AppIcon`.

## Acceptance Criteria Draft

- [ ] Users can create a plain todo with no scheduled time.
- [ ] Users can create a scheduled item with `scheduledAt` from the home panel by providing both date and time.
- [ ] Users can add, change, and clear an existing item's `scheduledAt` from the home panel.
- [ ] Create/edit uses an inline expanded editor inside `TodoSchedulePanel.vue`, not a modal/dialog.
- [ ] Scheduled unfinished items increase "今日概览 > 日程安排".
- [ ] The "待办 / 日程" panel clearly displays scheduled items and unscheduled todos.
- [ ] Scheduled and unscheduled items remain in one mixed list; scheduled items have a visible time/schedule affordance.
- [ ] Completion and deletion still work for both scheduled and unscheduled items.
- [ ] Existing overdue notification behavior still works for scheduled overdue unfinished items.
- [ ] Build/type-check passes with `npm run build` in `frontend/`.
- [ ] Relevant backend tests pass if backend code changes are required.

## Likely Out Of Scope

- Calendar month/week/day view.
- Date-only schedules.
- Recurring schedules.
- External calendar sync.
- Multi-attendee events.
- Reminder configuration beyond the existing overdue notification behavior.
- A separate schedule table/API.

## Decisions

- Schedules in this MVP are existing todos with optional `scheduledAt`; no separate schedule entity/API.
- Home panel supports lightweight editing of existing items' planned time.
- Create/edit interaction uses an inline expanded editor inside the panel.
- Scheduled items require both date and time; date-only schedules are out of scope.
- The panel keeps one mixed list; scheduled items are visually distinguished rather than split into a separate group.

## Open Questions

- None blocking planning.
