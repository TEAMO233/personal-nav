/**
 * 后端接口对应的 TypeScript 类型(与各 *Response DTO 字段一一对应)。
 */

/** 用户角色 */
export type Role = 'USER' | 'ADMIN'

/** 账户状态 */
export type UserStatus = 'ACTIVE' | 'DISABLED'

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

/** 新建/更新引擎的入参(iconAssetId 传 null 即清除图标) */
export interface EngineInput {
  name: string
  urlTemplate: string
  iconAssetId?: string | null
}

/** 新建/更新分组的入参 */
export interface GroupInput {
  name: string
}

/** 新建/更新快捷方式的入参(更新时后端忽略 groupId,跨组移动走排序接口) */
export interface ShortcutInput {
  groupId: string
  name: string
  url: string
  iconAssetId?: string | null
}

/** 快捷方式排序 + 跨组移动的单项位置 */
export interface ShortcutOrderItem {
  id: string
  groupId: string
  sortOrder: number
}

/** 媒体资源(对应后端 MediaResponse) */
export interface MediaAsset {
  id: string
  /** 来源类型:UPLOAD / FAVICON / URL */
  type: string
  /** 文件 MIME 类型 */
  contentType: string
  /** 读取地址 /api/media/{id} */
  url: string
}

/** 管理后台用户列表项(对应后端 AdminUserResponse) */
export interface AdminUser {
  id: string
  username: string
  role: Role
  /** 账户状态:ACTIVE / DISABLED */
  status: UserStatus
  /** 创建时间(ISO 8601 字符串) */
  createdAt: string
}

/** 管理员开户入参 */
export interface CreateUserInput {
  username: string
  password: string
  role?: Role
}

/** 签发的邀请码(对应后端 InviteCodeResponse) */
export interface InviteCode {
  code: string
  /** 过期时间(ISO 8601 字符串) */
  expiresAt: string
}

/** 搜索历史项(对应后端 SearchHistoryResponse) */
export interface SearchHistoryItem {
  id: string
  keyword: string
  /** 最近搜索时间(ISO 8601 字符串) */
  searchedAt: string
}
