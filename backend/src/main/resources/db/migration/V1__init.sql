-- V1__init.sql:个人导航初始库结构
-- 约定:全表 UUID 主键(gen_random_uuid 兜底),时间戳用 timestamptz,枚举用 varchar + CHECK。
-- 枚举值统一大写,与 Java 枚举名 + @Enumerated(STRING) 对齐。
-- 建表顺序满足外键依赖:users -> invite_codes -> media_assets -> search_engines -> shortcut_groups -> shortcuts。

-- 用户表:账户、密码哈希、角色与状态
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER'   CHECK (role IN ('USER', 'ADMIN')),
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 邀请码表:由管理员签发,可被一个用户消费
CREATE TABLE invite_codes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(64) NOT NULL UNIQUE,
    created_by UUID        NOT NULL REFERENCES users(id),
    used_by    UUID        REFERENCES users(id),
    expires_at TIMESTAMPTZ,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- 按签发者查邀请码
CREATE INDEX idx_invite_codes_created_by ON invite_codes (created_by);

-- 媒体资源表:用户上传/抓取/外链图标的存储记录
CREATE TABLE media_assets (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    type         VARCHAR(16)  NOT NULL CHECK (type IN ('UPLOAD', 'FAVICON', 'URL')),
    storage_key  VARCHAR(512) NOT NULL,
    source_url   VARCHAR(2048),
    content_type VARCHAR(128) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- 按用户查媒体资源
CREATE INDEX idx_media_assets_user_id ON media_assets (user_id);

-- 搜索引擎表:按用户隔离,含预置与自定义引擎
CREATE TABLE search_engines (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users(id),
    name          VARCHAR(64)   NOT NULL,
    url_template  VARCHAR(1024) NOT NULL,
    icon_asset_id UUID          REFERENCES media_assets(id),
    is_default    BOOLEAN       NOT NULL DEFAULT false,
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    is_preset     BOOLEAN       NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- 按用户 + 排序查引擎
CREATE INDEX idx_search_engines_user_sort ON search_engines (user_id, sort_order);

-- 快捷方式分组表:按用户隔离
CREATE TABLE shortcut_groups (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id),
    name       VARCHAR(64) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 按用户 + 排序查分组
CREATE INDEX idx_shortcut_groups_user_sort ON shortcut_groups (user_id, sort_order);

-- 快捷方式表:归属分组,按用户隔离
CREATE TABLE shortcuts (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users(id),
    group_id      UUID          NOT NULL REFERENCES shortcut_groups(id),
    name          VARCHAR(64)   NOT NULL,
    url           VARCHAR(2048) NOT NULL,
    icon_asset_id UUID          REFERENCES media_assets(id),
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- 按用户查快捷方式
CREATE INDEX idx_shortcuts_user_id ON shortcuts (user_id);
-- 按分组 + 排序查快捷方式
CREATE INDEX idx_shortcuts_group_sort ON shortcuts (group_id, sort_order);
