/**
 * 待办 / 日程接口:首页内联列表、创建、更新与删除。
 */
import http from './http'
import type { TodoInput, TodoItem } from './types'

/**
 * 获取当前用户待办。
 *
 * @param limit 最大返回数量
 * @returns 待办列表
 */
export function listTodos(limit?: number): Promise<TodoItem[]> {
  // 1. 拉取本人待办
  return http.get<TodoItem[]>('/todos', { params: { limit } }).then((r) => r.data)
}

/**
 * 新建待办。
 *
 * @param input 待办内容
 * @returns 新待办
 */
export function createTodo(input: TodoInput): Promise<TodoItem> {
  // 1. 提交新建
  return http.post<TodoItem>('/todos', input).then((r) => r.data)
}

/**
 * 更新待办。
 *
 * @param id    待办 id
 * @param input 更新内容
 * @returns 更新后待办
 */
export function updateTodo(id: string, input: TodoInput): Promise<TodoItem> {
  // 1. 提交更新
  return http.put<TodoItem>(`/todos/${id}`, input).then((r) => r.data)
}

/**
 * 删除待办。
 *
 * @param id 待办 id
 */
export function deleteTodo(id: string): Promise<void> {
  // 1. 删除后端返回 204
  return http.delete(`/todos/${id}`).then(() => undefined)
}
