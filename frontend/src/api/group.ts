/**
 * 快捷方式分组接口。M6 只需列表;增删改排序留待 M7。
 */
import http from './http'
import type { Group } from './types'

/**
 * 获取当前用户的全部分组(后端已按 sort_order 排序)。
 *
 * @returns 分组列表
 */
export function listGroups(): Promise<Group[]> {
  // 1. 拉取本人分组
  return http.get<Group[]>('/groups').then((r) => r.data)
}
