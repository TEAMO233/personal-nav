# Styling & Design Tokens

> How visual styling, theming, and glassmorphism are implemented in this project.

---

## Approach

- **Plain CSS + CSS custom properties.** No SCSS/Sass, no Tailwind, no CSS-in-JS. (`sass` is NOT a dependency — do not introduce it.)
- Global tokens and shared classes live in `src/style.css`. Component styles use `<style scoped>` and consume tokens via `var(--token)`.
- Icons: in-house `AppIcon.vue` (inline SVG using `currentColor`). `@element-plus/icons-vue` and `lucide` are NOT used — add new glyphs to `AppIcon`'s `IconName`/`paths`.

---

## Design Tokens (`src/style.css`)

Apple-HIG-flavored token set. Always prefer a token over a hard-coded value.

| Group | Examples |
|-------|----------|
| Type scale | `--text-caption2` … `--text-large-title` (NOTE: there is no `--text-headline`) |
| System colors | `--system-blue` / `-red` / `-green` / `-gray` |
| Label colors | `--label-primary` / `-secondary` / `-tertiary` / `-quaternary` |
| Backgrounds | `--bg-primary` / `-secondary` / `-tertiary` / `-elevated` |
| Separators | `--separator`, `--separator-opaque` |
| Material | `--material-bar` (translucent top bar) |
| Spacing (8pt grid) | `--space-1` … `--space-12` |
| Radius | `--radius-sm/md/lg/xl/2xl/full` |
| Shadow | `--shadow-card`, `--shadow-elevated` |
| Motion | `--ease-default` / `-spring`, `--duration-instant/fast/normal` |
| Glass (home) | `--glass-bg` / `-border` / `-highlight` / `-blur` / `-shadow` |
| Home background | `--home-bg`, `--home-glow-1/2/3` |
| Accent gradient | `--accent-grad` |

---

## Theming: light + dark

- Light values on `:root`; dark overrides on `html.dark`. Toggled by the `theme` Pinia store, persisted to `localStorage` key `nav-theme`, first run follows `prefers-color-scheme`.
- The same `html.dark` class drives both these tokens and Element Plus dark mode.
- **Every new visual must define both light and dark values.** Glass differs per theme: dark = low-opacity white over deep blue; light = higher-opacity white over soft light.

---

## Glassmorphism Convention

- Shared panel class: global `.glass-panel` (in `style.css`) = bg + border + radius + blur + shadow from `--glass-*`. Compose it with a scoped class for layout/padding.
- **Always pair `backdrop-filter` with `-webkit-backdrop-filter`.** Provide an opaque fallback where readability matters if the property is unsupported.
- The home animated background (`components/home/DynamicBackground.vue`) is **home-scoped only** (rendered inside `HomeView`, `position:absolute` under content), pure CSS, and must stop animating under `@media (prefers-reduced-motion: reduce)`.

**Example**

```vue
<section class="glass-panel my-widget">...</section>

<style scoped>
.my-widget { padding: var(--space-5); }
</style>
```
