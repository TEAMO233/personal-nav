# Directory Structure

> How frontend code is organized in this project (`frontend/src`).

---

## Directory Layout

```
src/
├── api/            # axios clients + types.ts (one module per resource: auth, engine, group, shortcut, ...)
├── assets/         # static assets (e.g. engine-icons/*.svg)
├── components/     # shared components (AppIcon, SearchBar, GroupGrid, ShortcutCard, ThemeToggle, ...)
│   ├── home/       # components used ONLY by HomeView (DynamicBackground, HomeTopbar, HomeHero, CategoryNav, StatsPanel)
│   └── settings/   # components used ONLY by SettingsView
├── composables/    # useXxx.ts (e.g. useIsMobile)
├── router/         # index.ts (routes + global auth/admin guards)
├── stores/         # Pinia stores (auth, engine, shortcut, searchHistory, theme)
├── views/          # route-level pages: <Name>View.vue (FLAT)
├── App.vue
├── main.ts
└── style.css       # global design tokens + shared classes
```

---

## Module Organization

- **Views are flat** `*View.vue` files under `views/`, registered in `router/index.ts`. Do NOT create nested view folders (e.g. `views/navigation/`).
- **Page-specific subcomponents** go under `components/<page>/` (e.g. `components/home/`). Cross-page reusable components stay at the `components/` root.
- Data access is centralized: components read from Pinia `stores/`, stores call `api/` clients. Components do not call `api/` directly.

---

## Naming Conventions

- Views: `PascalCaseView.vue`. Components: `PascalCase.vue`. Stores/composables/api: `camelCase.ts`.
- Components are referenced via **explicit `import`** (auto-import is configured, but the code style is explicit imports for readability).
