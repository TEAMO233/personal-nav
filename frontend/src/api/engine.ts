/**
 * 搜索引擎接口。M6 只需列表;增删改排序留待 M7。
 */
import http from './http'
import type { Engine } from './types'

/**
 * 获取当前用户的全部引擎(后端已按 sort_order 排序)。
 *
 * @returns 引擎列表
 */
export function listEngines(): Promise<Engine[]> {
  // 1. 拉取本人引擎
  return http.get<Engine[]>('/engines').then((r) => r.data)
}
