/**
 * 最近访问接口:记录用户点击过的导航资源。
 */
import http from './http'
import type { RecentVisit, RecordVisitInput } from './types'

/**
 * 获取最近访问。
 *
 * @param limit 最大返回数量
 * @returns 最近访问列表
 */
export function listRecentVisits(limit?: number): Promise<RecentVisit[]> {
  // 1. 拉取本人最近访问
  return http.get<RecentVisit[]>('/recent-visits', { params: { limit } }).then((r) => r.data)
}

/**
 * 记录一次访问。
 *
 * @param input 访问目标
 * @returns 新访问记录
 */
export function recordVisit(input: RecordVisitInput): Promise<RecentVisit> {
  // 1. 提交访问记录
  return http.post<RecentVisit>('/recent-visits', input).then((r) => r.data)
}

/**
 * 删除一条最近访问。
 *
 * @param id 访问记录 id
 */
export function deleteRecentVisit(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/recent-visits/${id}`).then(() => undefined)
}

/**
 * 清空最近访问。
 */
export function clearRecentVisits(): Promise<void> {
  // 1. 清空后端返回 204
  return http.delete('/recent-visits').then(() => undefined)
}
