# Technical Design

## Architecture

This MVP reuses the existing todo-as-schedule model:

- Backend table: `todos.scheduled_at`
- Backend API: `/api/todos` create/update already accepts `scheduledAt`
- Frontend type/store: `TodoInput.scheduledAt`, `TodoItem.scheduledAt`, `todoStore.scheduleCount`
- Home UI: `TodoSchedulePanel.vue` becomes the main user-facing place to create and lightly edit scheduled items

No new backend entity, endpoint, migration, Pinia store, route, or settings page is required for the MVP.

## Frontend Component Boundary

Primary change: `frontend/src/components/home/TodoSchedulePanel.vue`

Expected behavior:

- Replace the current title-only add form with one inline expanded editor reused for create and edit.
- Editor fields:
  - title, required
  - tag, optional
  - scheduled date+time, optional as a pair
- Create without scheduled date+time creates a plain todo (`scheduledAt: null`).
- Create with date+time creates a scheduled todo (`scheduledAt` ISO string).
- Editing an existing item pre-fills title, tag, and local date/time from `scheduledAt`.
- Editing can save a new scheduled date+time, change it, or clear it back to `null`.
- Completion and delete remain row-level actions.

Secondary optional change: `frontend/src/stores/todo.ts`

- Existing `create`, `update`, `toggleDone`, and `remove` already support the needed data flow.
- Add small computed helpers only if they remove duplication in the panel; do not create fake schedule data.

## Data Flow

Create plain todo:

1. User opens inline editor.
2. User enters title and leaves schedule empty.
3. Frontend calls `todoStore.create({ title, tag, scheduledAt: null })`.
4. Store reloads todos.
5. Overview `scheduleCount` stays unchanged.

Create scheduled item:

1. User opens inline editor.
2. User enters title and both date + time.
3. Frontend converts local date/time to ISO via `new Date(localValue).toISOString()`.
4. Frontend calls `todoStore.create({ title, tag, scheduledAt })`.
5. Store reloads todos.
6. Overview `scheduleCount` updates because unfinished scheduled items are counted.

Edit scheduled time:

1. User clicks edit on a row.
2. Inline editor pre-fills current values.
3. User changes or clears date/time.
4. Frontend calls `todoStore.update(id, { title, tag, scheduledAt, done })` preserving the current `done` state.
5. Existing overdue notification resolution behavior remains backend-owned.

## Date/Time Contract

- Scheduled items require both date and time.
- Date-only schedules are out of scope.
- Empty date+time means no schedule (`scheduledAt: null`).
- If one of date/time is missing while the other is filled, the editor should block save with a local validation message.
- Display uses existing local-time formatting: today, tomorrow, or MM-DD plus HH:mm.

## UI Composition

- Keep one mixed list; do not split into separate "日程" and "待办" groups.
- Scheduled items use a visible time/schedule affordance, e.g. a colored time pill or calendar icon, so they connect to "今日概览 > 日程安排".
- Unscheduled items show "未排期" in a quieter style.
- Keep controls compact enough for the home dashboard card.
- Avoid modal/dialog for this MVP; use an inline editor like `NotePanel.vue`.

## Compatibility

- Existing API contracts stay compatible.
- Existing todos with `scheduledAt: null` remain plain todos.
- Existing todos with `scheduledAt` become visible scheduled items in the mixed list.
- Existing overdue notification tests should continue to pass because scheduled values still flow through the same backend field.

## Trade-Offs

- Reusing `todos.scheduled_at` avoids backend migration and makes the overview metric immediately useful.
- A mixed list is denser and easier to fit in the dashboard, but less semantically separated than dedicated "日程" and "待办" groups.
- Inline editing is lightweight, but less spacious than a modal; this matches the home workspace pattern and avoids backend/admin-page feel.

## Rollback

Rollback can restore `TodoSchedulePanel.vue` to title-only add behavior. No schema or API rollback is expected.
