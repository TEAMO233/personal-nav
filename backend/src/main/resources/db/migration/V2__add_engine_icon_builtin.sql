-- V2__add_engine_icon_builtin.sql:搜索引擎加内置图标标识列
-- 预置引擎(Google/百度/Bing/DuckDuckGo)图标用前端打包内置资源,不进 media_assets。
-- 本列存内置图标的 key(google/baidu/bing/duckduckgo);自定义引擎此列为空,改用 icon_asset_id。
ALTER TABLE search_engines ADD COLUMN icon_builtin VARCHAR(64);
