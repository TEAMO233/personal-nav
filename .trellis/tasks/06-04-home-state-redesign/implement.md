# Implementation Plan

## Checklist

- [x] Read frontend specs before editing.
- [x] Before each UI-facing implementation choice, use the existing `ui-ux-pro-max` findings in `design.md`; run an additional focused `ui-ux-pro-max` search if a detail is not covered.
- [x] Refactor `HomeView.vue` into route container and four-way renderer.
- [x] Add `GuestDesktopHome.vue` with search-only desktop composition.
- [x] Add `GuestMobileHome.vue` with search-only mobile composition.
- [x] Add `LoggedInDesktopHome.vue` with full workspace composition.
- [x] Add `LoggedInMobileHome.vue` with dedicated mobile workspace composition.
- [x] Apply the `ui-ux-pro-max` color synthesis: graphite/neutral foundation, restrained cool accent, minimal warm emphasis, high contrast in light/dark.
- [x] Adjust existing home component CSS only where needed for reuse.
- [x] Ensure loading/error states are announced and retry remains available.
- [x] Run `npm run build` in `frontend/`.
- [x] Run a local dev server and visually inspect desktop + mobile home states where feasible.

## Implementation Notes

- Design source: `ui-ux-pro-max` Minimal Single Column, Search-as-CTA, Portfolio/Personal monochrome + blue accent, OLED/dark high contrast, Glassmorphism contrast cautions, Inclusive Design focus/contrast guidance, mobile touch target and spacing guidance.
- Follow-up design source for login entry: `ui-ux-pro-max` Minimal Single Column + focus/contrast guidance; login is a single auxiliary text action, not a second primary CTA.
- Follow-up correction: logged-in desktop also treats search as the primary home CTA, so its hero uses a centered vertical search-first composition instead of a right-column search block.
- Project adaptations: no external font imports, no icon libraries, no fake data, scoped CSS, and semantic tokens in `style.css`.
- Guest desktop/mobile were visually checked at 1440px and 390px widths. Login-state components were verified by build/type-check and structural review; no fake store data was added for visual fill.

## Validation Commands

```bash
cd frontend
npm run build
npm run dev
```

Use browser viewport checks around:

- desktop: 1440px wide
- mobile: 390px wide

## Risky Files

- `frontend/src/views/HomeView.vue`: route-level behavior and store loading.
- `frontend/src/components/SearchBar.vue`: should remain behavior-compatible; avoid unnecessary changes.
- `frontend/src/style.css`: token changes affect global theme and multiple palette variants.

## Review Gates

- UI design choices should trace back to `ui-ux-pro-max` guidance, with only project-constraint adaptations made by the implementer.
- Guest pages must not accidentally include login/register CTAs via `HomeTopbar`.
- Guest pages must not load authenticated store data.
- Logged-in mobile must not depend on desktop grid compression as its main layout.
- New icons must use existing `AppIcon`; no new icon library.
- Overall color should not become a single-hue amber/purple/blue page; accents should support hierarchy instead of dominating every surface.
