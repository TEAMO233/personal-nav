-- V3__create_search_history.sql:搜索历史
-- 每个用户的每个关键词只存一条(同词重复搜索更新 searched_at 置顶),按用户隔离。

CREATE TABLE search_history (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    keyword     VARCHAR(256) NOT NULL,
    searched_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- 同一用户同一关键词去重
    UNIQUE (user_id, keyword)
);
-- 按用户 + 搜索时间倒序取最近历史
CREATE INDEX idx_search_history_user_time ON search_history (user_id, searched_at DESC);
