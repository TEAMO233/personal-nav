-- V4__home_dashboard_resources.sql:首页工作台真实资源
-- 为首页玻璃拟态工作台补齐精选快捷方式、待办、便签、最近访问与通知。

-- 快捷方式扩展:首页精选卡片展示字段。全部向后兼容,旧数据默认非精选。
ALTER TABLE shortcuts
    ADD COLUMN description    VARCHAR(160),
    ADD COLUMN icon_key       VARCHAR(32),
    ADD COLUMN accent         VARCHAR(32),
    ADD COLUMN featured       BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN featured_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_shortcuts_user_featured
    ON shortcuts (user_id, featured, featured_order, sort_order);

-- 待办 / 日程:按用户隔离,首页内联管理。
CREATE TABLE todos (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    title        VARCHAR(160) NOT NULL,
    tag          VARCHAR(32),
    scheduled_at TIMESTAMPTZ,
    done         BOOLEAN      NOT NULL DEFAULT false,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_todos_user_home
    ON todos (user_id, done, scheduled_at, sort_order, created_at DESC);

CREATE INDEX idx_todos_user_overdue
    ON todos (user_id, done, scheduled_at);

-- 灵感便签:首页展示置顶 / 最新便签。
CREATE TABLE notes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    pinned     BOOLEAN     NOT NULL DEFAULT false,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notes_user_home
    ON notes (user_id, pinned DESC, sort_order, created_at DESC);

-- 最近访问:点击快捷方式时记录,保留名称 / URL 快照。
CREATE TABLE recent_visits (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users(id),
    shortcut_id UUID          REFERENCES shortcuts(id),
    name        VARCHAR(96)   NOT NULL,
    url         VARCHAR(2048) NOT NULL,
    domain      VARCHAR(255)  NOT NULL,
    visited_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_recent_visits_user_time
    ON recent_visits (user_id, visited_at DESC);

-- 首页书签:替代首页"最近访问"面板,由用户在设置页配置。
CREATE TABLE home_bookmarks (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users(id),
    name          VARCHAR(96)   NOT NULL,
    url           VARCHAR(2048) NOT NULL,
    description   VARCHAR(160),
    icon_asset_id UUID          REFERENCES media_assets(id),
    enabled       BOOLEAN       NOT NULL DEFAULT true,
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_home_bookmarks_user_home
    ON home_bookmarks (user_id, enabled, sort_order, created_at DESC);

-- 通知:真实铃铛下拉;逾期待办生成 TODO_OVERDUE 通知。
CREATE TABLE notifications (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    type        VARCHAR(32)  NOT NULL CHECK (type IN ('TODO_OVERDUE')),
    source_id   UUID,
    title       VARCHAR(160) NOT NULL,
    content     VARCHAR(512),
    is_read     BOOLEAN      NOT NULL DEFAULT false,
    resolved    BOOLEAN      NOT NULL DEFAULT false,
    notified_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, type, source_id)
);

CREATE INDEX idx_notifications_user_default
    ON notifications (user_id, resolved, is_read, notified_at DESC);
