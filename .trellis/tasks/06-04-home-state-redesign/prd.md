# 重新设计首页四端状态

## Goal

Redesign the home page as four explicit experiences instead of one responsive layout:

- Logged-in desktop home
- Logged-in mobile home
- Guest desktop home
- Guest mobile home

The logged-in experience should feel like a focused personal navigation workspace. The guest experience should be Google-like: the search entry remains the only primary content, while a minimal login entry must remain available on the home page. There should be no register CTA, feature preview, or marketing sections.

## Confirmed Facts

- Frontend stack is Vue 3 + TypeScript + Pinia + Vue Router + plain CSS.
- `frontend/src/views/HomeView.vue` is the current home route component.
- The current page branches by `auth.isLoggedIn` inside one component tree and uses media queries for mobile layout.
- Login state comes from `useAuthStore()`.
- The project already has `useIsMobile()` for media-query based runtime branching.
- Search is handled by `SearchBar.vue`; guest users get a public Google engine fallback.
- Existing home data stores load engines, shortcuts, search history, todos, notes, home bookmarks, and notifications only for logged-in users.
- Styling must follow frontend specs: scoped CSS, global tokens from `style.css`, in-house `AppIcon`, no new icon dependency.
- `ui-ux-pro-max` recommended a directory/search-first pattern with high contrast, visible focus states, and responsive accessibility checks.
- After the guest scope was narrowed, `ui-ux-pro-max` was run again for a minimal search-only homepage. The relevant recommendation is "Minimal Single Column" plus "search bar is the CTA"; marketing sections, category sections, and CTAs are intentionally excluded by user request.
- Overall color/style was also researched with `ui-ux-pro-max`. Relevant results: Portfolio/Personal monochrome + blue accent, Productivity Tool teal + orange, OLED/dark high-contrast, Glassmorphism with 4.5:1 contrast caution, and Inclusive Design high-contrast focus states.
- After implementation review, the user required a home-page login entry. `ui-ux-pro-max` follow-up guidance supports a minimal single auxiliary action with high contrast and visible focus state, while keeping search as the primary CTA.

## Requirements

- All UI design details must be derived from `ui-ux-pro-max` recommendations. Implementation may adapt those recommendations only to satisfy existing project constraints such as Vue/plain CSS, semantic tokens, `AppIcon`, and current data contracts.
- Split home rendering into explicit logged-in/guest and desktop/mobile components, selected by state in `HomeView.vue`.
- Keep shared data loading in `HomeView.vue` so logged-in stores still load once and guest users skip private data requests.
- Guest desktop home must display a centered search experience in the style of Google search, plus a minimal login entry.
- Guest mobile home must display a centered/mobile-ergonomic search experience in the style of Google search, plus a minimal login entry.
- Guest search-only pages should use large whitespace, high-contrast search affordance, system typography, and minimal visual noise.
- Guest views must not show register CTAs, feature previews, marketing copy, dashboard widgets, or shortcut grids. The only non-search action allowed is a minimal login entry.
- Logged-in desktop home must present a dense but polished workspace: search, featured shortcuts, dashboard panels, and topbar actions.
- Logged-in mobile home must be a separate mobile composition, not just desktop blocks squeezed by media queries.
- Preserve existing search behavior, engine selection, search history behavior for logged-in users, and public Google fallback for guests.
- Preserve existing theme and palette support through semantic CSS variables.
- Overall default color direction should come from the design research: high-contrast graphite/neutral foundation, restrained accent usage, readable light/dark variants, and no one-note saturated theme.
- Maintain accessibility basics: visible focus states, touch targets at least 44px, meaningful labels, and announced loading/error states.

## Acceptance Criteria

- [ ] Planning and implementation notes identify the `ui-ux-pro-max` design guidance used for layout, color, interaction, and accessibility decisions.
- [ ] `HomeView.vue` selects among four home components by `auth.isLoggedIn` and mobile state.
- [ ] Guest desktop view contains the search box as the only primary page content, includes a minimal login entry, and has no register or feature-preview UI.
- [ ] Guest mobile view contains the search box as the only primary page content, includes a minimal login entry, and has no register or feature-preview UI.
- [ ] Logged-in desktop view keeps full workspace functionality: topbar, hero/search, featured shortcuts, dashboard widgets, loading, error retry.
- [ ] Logged-in mobile view has a distinct layout optimized for narrow screens, with stable spacing and no overlapping text or controls.
- [ ] Existing stores still load for logged-in users and are not loaded for guests.
- [ ] Search works in guest and logged-in states.
- [ ] Build/type-check passes with `npm run build` in `frontend/`.
- [ ] The home page is visually checked in desktop and mobile viewport widths.

## Out Of Scope

- Backend API changes.
- Login/register page redesign.
- New search engines or new search suggestion behavior.
- New third-party icon, CSS, or UI libraries.
- Marketing landing-page sections for guest users.

## Open Questions

- None blocking planning. Guest page direction is confirmed as Google-like search-only.
