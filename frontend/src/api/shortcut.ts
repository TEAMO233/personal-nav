/**
 * 快捷方式接口。M6 只需列表;增删改排序与跨组移动留待 M7。
 */
import http from './http'
import type { Shortcut } from './types'

/**
 * 获取当前用户的全部快捷方式(后端按 sort_order + 创建时间排序)。
 *
 * @returns 快捷方式列表
 */
export function listShortcuts(): Promise<Shortcut[]> {
  // 1. 拉取本人快捷方式
  return http.get<Shortcut[]>('/shortcuts').then((r) => r.data)
}
