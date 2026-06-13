# Fix search overlay autocomplete clash

## Goal

Prevent browser autocomplete from overlapping the custom search overlay and improve overlay readability.

## Requirements

- The search input must not trigger the browser's native autocomplete/password suggestion popup while the app's custom search history menu is open.
- The custom search history and engine menus must remain visually readable over the home dashboard content.
- The fix must preserve existing search behavior: engine selection, keyword submit, history click, per-item delete, clear all, and Cmd/Ctrl+K focus.
- Keep the change scoped to the frontend search UI unless repository evidence shows a backend change is required.

## Acceptance Criteria

- [ ] Focusing the search input on the home page shows only the app-owned search history menu, with no browser autocomplete popup overlapping it.
- [ ] Search history menu contents are readable against the dashboard behind it in both light and dark themes.
- [ ] Existing search history interactions still work.
- [ ] `npm run build` passes for the frontend.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
