/**
 * 后端接口对应的 TypeScript 类型(与各 *Response DTO 字段一一对应)。
 */

/** 用户角色 */
export type Role = 'USER' | 'ADMIN'

/** 当前用户信息(对应后端 UserResponse) */
export interface UserInfo {
  id: string
  username: string
  role: Role
}

/** 搜索引擎(对应后端 EngineResponse) */
export interface Engine {
  id: string
  name: string
  /** 搜索 URL 模板,含 {query} 占位 */
  urlTemplate: string
  /** 预置引擎的内置图标 key(google/baidu/bing/duckduckgo),自定义引擎为 null */
  iconBuiltin: string | null
  /** 自定义图标的媒体 id,可空 */
  iconAssetId: string | null
  isDefault: boolean
  sortOrder: number
  isPreset: boolean
}

/** 快捷方式分组(对应后端 GroupResponse) */
export interface Group {
  id: string
  name: string
  sortOrder: number
}

/** 快捷方式(对应后端 ShortcutResponse) */
export interface Shortcut {
  id: string
  groupId: string
  name: string
  url: string
  /** 自定义图标的媒体 id,可空 */
  iconAssetId: string | null
  sortOrder: number
}

/** 后端统一错误结构 */
export interface ApiError {
  code: string
  message: string
}
