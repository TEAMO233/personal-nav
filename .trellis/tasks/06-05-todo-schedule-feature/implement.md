# Implementation Plan

## Checklist

- [x] Run `trellis-before-dev` before coding and read frontend/backend specs as needed.
- [x] Update `TodoSchedulePanel.vue` state for create/edit modes.
- [x] Add helper functions to convert between backend ISO `scheduledAt` and local date/time inputs.
- [x] Replace title-only add form with inline expanded editor.
- [x] Add row edit action that pre-fills title, tag, and scheduled date/time.
- [x] Add local validation: title required; date and time must be both present or both empty.
- [x] Preserve complete/delete behavior.
- [x] Style scheduled rows with a visible time/schedule affordance and unscheduled rows with quieter "未排期" styling.
- [x] Verify `OverviewPanel.vue` schedule count updates after creating scheduled items.
- [x] Run `npm run build` in `frontend/`.
- [x] If backend code is touched unexpectedly, run relevant backend tests and update this plan.
- [x] Sort overdue unfinished scheduled todos to the top of the home panel and highlight them with an alert color.

## Validation Commands

```bash
cd frontend
npm run build
```

If backend changes become necessary:

```bash
cd backend
./mvnw test
```

## Manual Checks

- Create a plain todo from the home panel.
- Create a scheduled item with date+time.
- Confirm "今日概览 > 日程安排" increments for scheduled unfinished items.
- Edit an existing plain todo to add a scheduled time.
- Edit a scheduled item to change its time.
- Clear an existing scheduled time and confirm schedule count updates.
- Toggle done and delete for both plain and scheduled items.
- Check panel at desktop dashboard width and narrow/mobile-like width.

## Risky Files

- `frontend/src/components/home/TodoSchedulePanel.vue`: primary behavior and layout.
- `frontend/src/stores/todo.ts`: only touch if helper state is genuinely needed.
- `frontend/src/api/types.ts`: should not need changes unless the existing type proves insufficient.

## Non-Goals

- No backend migration.
- No new schedule API/store.
- No calendar view or recurring schedules.
- No date-only schedule semantics.
