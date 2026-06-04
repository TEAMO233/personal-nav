/**
 * 灵感便签接口:首页内联列表、创建、更新与删除。
 */
import http from './http'
import type { NoteInput, NoteItem } from './types'

/**
 * 获取当前用户便签。
 *
 * @param limit 最大返回数量
 * @returns 便签列表
 */
export function listNotes(limit?: number): Promise<NoteItem[]> {
  // 1. 拉取本人便签
  return http.get<NoteItem[]>('/notes', { params: { limit } }).then((r) => r.data)
}

/**
 * 新建便签。
 *
 * @param input 便签内容
 * @returns 新便签
 */
export function createNote(input: NoteInput): Promise<NoteItem> {
  // 1. 提交新建
  return http.post<NoteItem>('/notes', input).then((r) => r.data)
}

/**
 * 更新便签。
 *
 * @param id    便签 id
 * @param input 更新内容
 * @returns 更新后便签
 */
export function updateNote(id: string, input: NoteInput): Promise<NoteItem> {
  // 1. 提交更新
  return http.put<NoteItem>(`/notes/${id}`, input).then((r) => r.data)
}

/**
 * 删除便签。
 *
 * @param id 便签 id
 */
export function deleteNote(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/notes/${id}`).then(() => undefined)
}
