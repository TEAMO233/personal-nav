# Quality Guidelines

> Code quality standards for frontend development.

---

## Forbidden Patterns

### No fake data / fake features for visual effect
The UI must render **only real data** backed by the API/stores. If a design/template asks for a module with no data source (e.g. todos, "focus score", visit history), do NOT fabricate front-end mock data to fill it — either propose a real backend change or drop the module. This mirrors the project's anti-fallback rule (never mask a missing capability with fake output).

### No new styling/runtime stacks
No SCSS/Sass, no alternative UI framework, no Three.js for backgrounds. Backgrounds are pure CSS.

---

## Required Patterns

- **Real-data mapping**: when a "dashboard" is desired, build it from real stores (e.g. category nav from `shortcutStore.grouped`, counts from store array lengths) instead of inventing metrics.
- Pair `backdrop-filter` with `-webkit-` prefix; respect `prefers-reduced-motion`.
- Define both light and dark values for any new visual (see styling.md).

---

## Testing Requirements

- No automated frontend test suite yet. The current bar is:
  - `npm run build` (= `vue-tsc -b && vite build`) must pass — **type-check is part of the build**.
  - Manual smoke test of affected flows (theme toggle, search/engine/history, navigation).

---

## Build Gotcha

When adding components that rely on auto-import generated `*.d.ts`, run `npx vite build` once to refresh the d.ts files before `npm run build` (avoids spurious type errors). See project memory `frontend-build-ep-dts`.
