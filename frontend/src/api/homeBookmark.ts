/**
 * 首页书签接口:首页读取启用项,设置页管理全部项。
 */
import http from './http'
import type { HomeBookmark, HomeBookmarkInput } from './types'

/**
 * 获取当前用户首页书签。
 *
 * @param enabledOnly 是否只返回启用项
 * @returns 首页书签列表
 */
export function listHomeBookmarks(enabledOnly = true): Promise<HomeBookmark[]> {
  // 1. 首页默认只拉启用项
  return http.get<HomeBookmark[]>('/home-bookmarks', { params: { enabledOnly } }).then((r) => r.data)
}

/**
 * 新建首页书签。
 *
 * @param input 书签内容
 * @returns 新书签
 */
export function createHomeBookmark(input: HomeBookmarkInput): Promise<HomeBookmark> {
  // 1. 提交新建
  return http.post<HomeBookmark>('/home-bookmarks', input).then((r) => r.data)
}

/**
 * 更新首页书签。
 *
 * @param id    书签 id
 * @param input 书签内容
 * @returns 更新后书签
 */
export function updateHomeBookmark(id: string, input: HomeBookmarkInput): Promise<HomeBookmark> {
  // 1. 提交更新
  return http.put<HomeBookmark>(`/home-bookmarks/${id}`, input).then((r) => r.data)
}

/**
 * 删除首页书签。
 *
 * @param id 书签 id
 */
export function deleteHomeBookmark(id: string): Promise<void> {
  // 1. 后端返回 204
  return http.delete(`/home-bookmarks/${id}`).then(() => undefined)
}

/**
 * 重排首页书签。
 *
 * @param orderedIds 有序 id 列表
 * @returns 重排后书签
 */
export function reorderHomeBookmarks(orderedIds: string[]): Promise<HomeBookmark[]> {
  // 1. 提交完整排序快照
  return http.put<HomeBookmark[]>('/home-bookmarks/order', { orderedIds }).then((r) => r.data)
}
