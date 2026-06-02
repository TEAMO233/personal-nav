/**
 * 快捷方式分组接口:列表 + 增删改与排序(均按当前用户隔离)。
 */
import http from './http'
import type { Group, GroupInput } from './types'

/**
 * 获取当前用户的全部分组(后端已按 sort_order 排序)。
 *
 * @returns 分组列表
 */
export function listGroups(): Promise<Group[]> {
  // 1. 拉取本人分组
  return http.get<Group[]>('/groups').then((r) => r.data)
}

/**
 * 新建分组(追加到末尾)。
 *
 * @param input 分组名称
 * @returns 新分组
 */
export function createGroup(input: GroupInput): Promise<Group> {
  // 1. 提交新建
  return http.post<Group>('/groups', input).then((r) => r.data)
}

/**
 * 更新分组名称。
 *
 * @param id    分组 id
 * @param input 新名称
 * @returns 更新后的分组
 */
export function updateGroup(id: string, input: GroupInput): Promise<Group> {
  // 1. 更新该分组
  return http.put<Group>(`/groups/${id}`, input).then((r) => r.data)
}

/**
 * 删除分组(后端级联删除组内快捷方式)。
 *
 * @param id 分组 id
 */
export function deleteGroup(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/groups/${id}`).then(() => undefined)
}

/**
 * 重排分组:orderedIds 须为当前全部分组 id 的一个排列。
 *
 * @param orderedIds 有序分组 id 列表
 * @returns 重排后的分组列表
 */
export function reorderGroups(orderedIds: string[]): Promise<Group[]> {
  // 1. 提交新顺序
  return http.put<Group[]>('/groups/order', { orderedIds }).then((r) => r.data)
}
