# Home Dashboard Resource Contracts

## Scenario: Homepage dashboard resources

### 1. Scope / Trigger

- Trigger: the homepage dashboard is a cross-layer feature with Flyway schema,
  Spring controllers/services, frontend API clients, Pinia stores, and Vue
  widgets.
- Applies when adding or changing homepage todos, notes, recent visits, home
  bookmarks, notifications, or featured shortcut fields.
- The dashboard must render real user-owned data. Do not add frontend mock data
  or visual fallback rows to make the Apple-style homepage look populated.

### 2. Signatures

- DB migration owner: `backend/src/main/resources/db/migration/V*_*.sql`.
- Existing table extension: `shortcuts.description`, `shortcuts.icon_key`,
  `shortcuts.accent`, `shortcuts.featured`, `shortcuts.featured_order`.
- Tables: `todos`, `notes`, `recent_visits`, `home_bookmarks`, `notifications`.
- APIs:
  - `GET /api/shortcuts/featured`
  - `GET /api/todos`, `POST /api/todos`, `PUT /api/todos/{id}`,
    `DELETE /api/todos/{id}`
  - `GET /api/notes`, `POST /api/notes`, `PUT /api/notes/{id}`,
    `DELETE /api/notes/{id}`
  - `GET /api/recent-visits`, `POST /api/recent-visits`
  - `GET /api/home-bookmarks?enabledOnly=true|false`,
    `POST /api/home-bookmarks`, `PUT /api/home-bookmarks/{id}`,
    `PUT /api/home-bookmarks/order`, `DELETE /api/home-bookmarks/{id}`
  - `GET /api/notifications`, `GET /api/notifications/unread-count`,
    `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`

### 3. Contracts

- All new resource rows include `user_id`; controllers derive the user from
  `SecurityUtils.currentUserId()`, never from request bodies.
- `Todo` requires `title`; optional fields are `tag`, `scheduledAt`,
  `done`, and `sortOrder`.
- `Note` requires `content`; optional fields are `pinned` and `sortOrder`.
- Homepage notes are a list, not a single active note. The frontend should load
  several notes (currently six), render every returned row, and delete/update by
  note id so a new note never visually overwrites the previous one.
- `RecentVisit` accepts either `shortcutId` or explicit `name` plus `url`.
  When `shortcutId` is present, the service must load the shortcut by
  `(id, user_id)` and snapshot `name`, `url`, and derived `domain`.
- `HomeBookmark` requires `name` and `url`; optional fields are `description`,
  `iconAssetId`, and `enabled`. If `iconAssetId` is present, the service must
  validate media ownership before saving.
- Homepage calls home bookmarks with `enabledOnly=true`. Settings calls with
  `enabledOnly=false` and can reorder only by submitting a complete ordered id
  list for the current user.
- `Notification` currently supports `TODO_OVERDUE`; overdue notifications are
  keyed by `(user_id, type, source_id)` so one overdue todo cannot create
  duplicate active notifications.
- Completing, deleting, rescheduling, or clearing the schedule of an overdue
  todo resolves its related notification by setting `resolved=true` and
  `read=true`.

### 4. Validation & Error Matrix

| Condition | Behavior |
| --- | --- |
| Empty todo title | Bean validation rejects the request |
| Empty note content | Bean validation rejects the request |
| Recent visit `shortcutId` belongs to another user | Return 404-style ownership failure |
| Home bookmark id belongs to another user | Return 404-style ownership failure |
| Home bookmark icon belongs to another user | Return invalid icon ownership failure before saving |
| Home bookmark order omits/adds/duplicates ids | Return bad request |
| Todo/note/notification id belongs to another user | Return 404-style ownership failure |
| Duplicate overdue sync for same todo | Reuse existing notification, do not insert another |
| Resolved notification | Hide from default notification list and unread count |

### 5. Good/Base/Bad Cases

- Good: user creates an overdue todo, opens notifications, sees one unread
  `TODO_OVERDUE`, completes the todo, and unread count drops because the
  notification is resolved.
- Base: user with no todos sees a real empty state and `0%` today completion
  rate, not hard-coded dashboard metrics.
- Bad: frontend fills recent visits, notes, or todos with static sample rows
  when the API returns an empty list.

### 6. Tests Required

- Integration tests must cover default featured shortcuts, todo CRUD, note CRUD,
  note list preservation after multiple creates, recent visit ownership
  validation, home bookmark CRUD/order/isolation, and overdue notification
  lifecycle.
- Frontend build (`npm run build`) is the required type check gate.
- Browser verification must include desktop `1672x941` and a mobile viewport
  such as `390x844`; desktop should not introduce first-screen overflow, and
  mobile must not introduce horizontal scrolling.

### 7. Wrong vs Correct

#### Wrong

```typescript
const notes = apiNotes.length ? apiNotes : [{ content: 'Sample note' }]
```

#### Correct

```typescript
const notes = await noteApi.listNotes(6)
```

The empty state belongs in the component UI, and the call to action must create
real persisted data through the store/API path.
