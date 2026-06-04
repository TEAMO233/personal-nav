# Technical Design

## Architecture

`HomeView.vue` remains the route-level container and owns state selection:

- initialize/load private home stores
- detect `auth.isLoggedIn`
- detect mobile state with `useIsMobile('(max-width: 760px)')` or a nearby breakpoint aligned to the existing home layout
- choose exactly one presentational home component

New component boundary under `frontend/src/components/home/`:

- `LoggedInDesktopHome.vue`
- `LoggedInMobileHome.vue`
- `GuestDesktopHome.vue`
- `GuestMobileHome.vue`

Existing reusable components remain in place where appropriate:

- `DynamicBackground.vue` for home background
- `HomeTopbar.vue` for authenticated workspace actions
- `HomeHero.vue` or a replacement section for authenticated search/header
- `FeaturedShortcutGrid.vue`
- `DashboardGrid.vue`
- `SearchBar.vue`

## Data Flow

`HomeView.vue`:

1. Reads `auth.isLoggedIn`.
2. If logged in, loads all existing home stores concurrently.
3. Tracks `loading` and `error`.
4. Passes `loading`, `error`, and `onRetry` props to logged-in components.
5. Renders guest components without loading private stores.

Guest components use only `SearchBar.vue`. `SearchBar.vue` already falls back to a public Google engine when `auth.isLoggedIn` is false, so no new API contract is needed.

## Visual Direction

All UI design decisions for this task must come from `ui-ux-pro-max`. The implementation role is to translate those design recommendations into the existing Vue/CSS codebase. When a generated recommendation conflicts with project constraints, keep the project constraint and document the adaptation. Known constraints:

- No new icon library; use `AppIcon`.
- No external font imports unless explicitly approved; use the project system font stack.
- Use semantic CSS tokens and scoped CSS.
- Keep existing search/auth/store behavior.

### Overall Color System

`ui-ux-pro-max` color/style findings for this project:

- Portfolio/Personal: monochrome foundation with a blue accent (`#18181B`, `#FAFAFA`, `#2563EB`).
- Productivity Tool: calm teal focus with orange action (`#0D9488`, `#F0FDFA`, `#EA580C`).
- Dark/OLED and Modern Dark: deep graphite surfaces, high-contrast foreground, minimal glow.
- Glassmorphism: works for dashboards when text contrast is guarded at 4.5:1 or better.
- Inclusive Design: visible focus rings, high contrast, and no color-only states.

Chosen synthesis:

- Use a graphite/neutral base as the overall default visual foundation.
- Use a restrained cool accent for search/focus and a warm accent only for emphasis, not as the whole page identity.
- Guest search-only pages should feel closer to monochrome high-contrast search than to the existing dramatic amber glass scene.
- Logged-in workspace can keep glass depth, but should reduce heavy single-hue dominance and improve panel readability.
- Preserve existing user-selectable palettes, but route new page styles through semantic home tokens so every palette can remain coherent.

Guest desktop/mobile:

- Google-like search-only composition.
- Calm centered layout, generous whitespace, brand text/logo may exist only as part of the search identity.
- Keep a minimal login entry available on the page; no register CTA, no feature preview, no marketing claims.
- Search control must be prominent and usable on mobile.
- `ui-ux-pro-max` follow-up design input: use the "Minimal Single Column" and "search bar is the CTA" guidance, then remove all CTA/benefit/category sections because the requested guest page is search-only.
- `ui-ux-pro-max` login-entry follow-up: a single auxiliary action may sit away from the centered search, with high text contrast and a visible focus ring, so search remains the primary CTA.
- Keep project system typography instead of adding external font imports from the generated recommendation.

Logged-in desktop:

- Workspace/dashboard composition.
- Search remains the primary action.
- Featured shortcuts and dashboard panels remain visible and scannable.
- Avoid nested-card clutter and oversized marketing hero treatment.

Logged-in mobile:

- Dedicated vertical composition.
- Prioritize topbar/search, shortcuts, then dashboard panels.
- Use stable one-column sections and compact headings.

## Compatibility

- Route path `/` does not change.
- Search behavior and store contracts do not change.
- Auth behavior does not change.
- Existing palette/theme tokens are reused; any new tokens must be added to `style.css` for light/dark and palette variants only if necessary.

## Trade-Offs

- Four explicit home components create more files, but reduce brittle media-query coupling and make state-specific polish easier.
- `HomeView.vue` keeps data loading centralized to avoid duplicated store orchestration across logged-in components.
- Guest pages intentionally remove conversion affordances to match the user's requested Google-style search-only experience.

## Rollback

The change is mostly frontend component composition. Rollback can restore `HomeView.vue` to the previous single layout and remove the new presentational components.
