/**
 * 快捷方式接口:列表 + 增删改、组内排序与跨组移动(均按当前用户隔离)。
 */
import http from './http'
import type { Shortcut, ShortcutInput, ShortcutOrderItem } from './types'

/**
 * 获取当前用户的全部快捷方式(后端按 sort_order + 创建时间排序)。
 *
 * @returns 快捷方式列表
 */
export function listShortcuts(): Promise<Shortcut[]> {
  // 1. 拉取本人快捷方式
  return http.get<Shortcut[]>('/shortcuts').then((r) => r.data)
}

/**
 * 获取当前用户首页精选快捷方式。
 *
 * @returns 首页精选快捷方式列表
 */
export function listFeaturedShortcuts(): Promise<Shortcut[]> {
  // 1. 拉取本人首页精选快捷方式
  return http.get<Shortcut[]>('/shortcuts/featured').then((r) => r.data)
}

/**
 * 新建快捷方式(追加到所属分组末尾)。
 *
 * @param input 分组、名称、URL、可选图标
 * @returns 新快捷方式
 */
export function createShortcut(input: ShortcutInput): Promise<Shortcut> {
  // 1. 提交新建
  return http.post<Shortcut>('/shortcuts', input).then((r) => r.data)
}

/**
 * 更新快捷方式的名称、URL 与图标(跨组移动走排序接口,不在此处)。
 *
 * @param id    快捷方式 id
 * @param input 更新内容(仅取 name/url/iconAssetId)
 * @returns 更新后的快捷方式
 */
export function updateShortcut(id: string, input: ShortcutInput): Promise<Shortcut> {
  // 1. 只提交可改字段(groupId 由排序接口处理)
  const body = {
    name: input.name,
    url: input.url,
    iconAssetId: input.iconAssetId,
    description: input.description,
    iconKey: input.iconKey,
    accent: input.accent,
    featured: input.featured,
    featuredOrder: input.featuredOrder,
  }
  return http.put<Shortcut>(`/shortcuts/${id}`, body).then((r) => r.data)
}

/**
 * 删除快捷方式。
 *
 * @param id 快捷方式 id
 */
export function deleteShortcut(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/shortcuts/${id}`).then(() => undefined)
}

/**
 * 重排 + 跨组移动:items 须覆盖当前全部快捷方式的新位置。
 *
 * @param items 全部快捷方式的 {id, groupId, sortOrder}
 * @returns 重排后的快捷方式列表
 */
export function reorderShortcuts(items: ShortcutOrderItem[]): Promise<Shortcut[]> {
  // 1. 提交全量新位置
  return http.put<Shortcut[]>('/shortcuts/order', { items }).then((r) => r.data)
}
