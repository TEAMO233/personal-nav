# Component Guidelines

> How components are built in this project.

---

## Component Structure

- `<script setup lang="ts">` with a **top-of-file block comment** stating the component's role (project-wide doc convention).
- Functions get JSDoc; method bodies use **numbered inline steps** (`// 1. ...`, `// 2. ...`) in plain, concrete Chinese.
- Order: script → template (with `<!-- section -->` comments) → `<style scoped>`.

---

## Props Conventions

- Type-only props: `defineProps<{ shortcut: Shortcut }>()`; use `withDefaults` for optional props with defaults.
- Prefer `computed` for derived display values (e.g. extracting a domain from a URL with `new URL(url).hostname`, wrapped in try/catch).

---

## Styling Patterns

- `<style scoped>`; consume global tokens; reuse the global `.glass-panel`. Full detail in [styling.md](./styling.md).

---

## Common Mistakes

### Undefined CSS variable
**Symptom**: A style silently has no effect / falls back to inherited value.
**Cause**: Referencing a token that doesn't exist (e.g. `--text-headline` was never defined).
**Prevention**: Only use tokens defined in `style.css`.

### Anchor scroll hidden by the sticky bar
**Symptom**: `scrollIntoView` lands with the target tucked under the fixed top bar.
**Fix**: Put `scroll-margin-top` on the scroll target (the home group containers use `80px`).

### Leaked global key listener
**Symptom**: A shortcut (e.g. ⌘K to focus search) keeps firing after leaving the page.
**Cause**: `window.addEventListener('keydown', ...)` in `onMounted` without cleanup.
**Fix**: Always `removeEventListener` in `onUnmounted`.

### Browser autocomplete covering custom menus
**Symptom**: Chrome's native autocomplete popup appears over an app-owned search/history menu.
**Cause**: A stable search-like `name`/`id` can trigger browser field history even when `autocomplete="new-password"` is present.
**Fix**: Put `autocomplete="off"` on the form and input, avoid stable search-like field names, and add autocomplete-ignore attributes when the field owns a custom suggestion menu.
