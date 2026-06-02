/**
 * 搜索引擎接口:列表 + 增删改、排序、设默认(均按当前用户隔离)。
 */
import http from './http'
import type { Engine, EngineInput } from './types'

/**
 * 获取当前用户的全部引擎(后端已按 sort_order 排序)。
 *
 * @returns 引擎列表
 */
export function listEngines(): Promise<Engine[]> {
  // 1. 拉取本人引擎
  return http.get<Engine[]>('/engines').then((r) => r.data)
}

/**
 * 新建引擎(追加到末尾)。
 *
 * @param input 名称、URL 模板、可选图标
 * @returns 新引擎
 */
export function createEngine(input: EngineInput): Promise<Engine> {
  // 1. 提交新建,返回带 id 的引擎
  return http.post<Engine>('/engines', input).then((r) => r.data)
}

/**
 * 更新引擎的名称、URL 模板与图标。
 *
 * @param id    引擎 id
 * @param input 更新内容
 * @returns 更新后的引擎
 */
export function updateEngine(id: string, input: EngineInput): Promise<Engine> {
  // 1. 全量更新该引擎
  return http.put<Engine>(`/engines/${id}`, input).then((r) => r.data)
}

/**
 * 删除引擎(若删的是默认,后端会把剩余最前的补设为默认)。
 *
 * @param id 引擎 id
 */
export function deleteEngine(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/engines/${id}`).then(() => undefined)
}

/**
 * 重排引擎:orderedIds 须为当前全部引擎 id 的一个排列。
 *
 * @param orderedIds 有序引擎 id 列表
 * @returns 重排后的引擎列表
 */
export function reorderEngines(orderedIds: string[]): Promise<Engine[]> {
  // 1. 提交新顺序,返回重排后的列表
  return http.put<Engine[]>('/engines/order', { orderedIds }).then((r) => r.data)
}

/**
 * 设某引擎为默认(其余自动取消默认)。
 *
 * @param id 引擎 id
 * @returns 设为默认后的引擎
 */
export function setDefaultEngine(id: string): Promise<Engine> {
  // 1. 设默认,返回该引擎
  return http.put<Engine>(`/engines/${id}/default`).then((r) => r.data)
}
