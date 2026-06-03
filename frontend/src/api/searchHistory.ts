/**
 * 搜索历史接口:列出、记录、删除、清空(均按当前用户隔离)。
 */
import http from './http'
import type { SearchHistoryItem } from './types'

/**
 * 取当前用户最近的搜索历史。
 *
 * @returns 搜索历史列表
 */
export function listSearchHistory(): Promise<SearchHistoryItem[]> {
  // 1. 拉取本人搜索历史
  return http.get<SearchHistoryItem[]>('/search-history').then((r) => r.data)
}

/**
 * 记录一次搜索(同词去重置顶),返回更新后的最近列表。
 *
 * @param keyword 搜索关键词
 * @returns 更新后的搜索历史列表
 */
export function recordSearch(keyword: string): Promise<SearchHistoryItem[]> {
  // 1. 提交关键词,后端返回最新列表
  return http.post<SearchHistoryItem[]>('/search-history', { keyword }).then((r) => r.data)
}

/**
 * 删除一条搜索历史。
 *
 * @param id 记录 id
 */
export function removeSearch(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/search-history/${id}`).then(() => undefined)
}

/**
 * 清空当前用户的全部搜索历史。
 */
export function clearSearchHistory(): Promise<void> {
  // 1. 清空后端返回 204
  return http.delete('/search-history').then(() => undefined)
}
