/**
 * 通知接口:顶栏铃铛列表、未读数与已读状态。
 */
import http from './http'
import type { NotificationItem, UnreadCount } from './types'

/**
 * 获取通知列表。
 *
 * @param limit 最大返回数量
 * @returns 通知列表
 */
export function listNotifications(limit?: number): Promise<NotificationItem[]> {
  // 1. 拉取本人通知
  return http.get<NotificationItem[]>('/notifications', { params: { limit } }).then((r) => r.data)
}

/**
 * 获取未读通知数。
 *
 * @returns 未读数
 */
export function getUnreadCount(): Promise<UnreadCount> {
  // 1. 拉取未读数
  return http.get<UnreadCount>('/notifications/unread-count').then((r) => r.data)
}

/**
 * 标记单条已读。
 *
 * @param id 通知 id
 * @returns 更新后通知
 */
export function markNotificationRead(id: string): Promise<NotificationItem> {
  // 1. 标记已读
  return http.put<NotificationItem>(`/notifications/${id}/read`).then((r) => r.data)
}

/**
 * 全部标记已读。
 */
export function markAllNotificationsRead(): Promise<void> {
  // 1. 全部已读后端返回 204
  return http.put('/notifications/read-all').then(() => undefined)
}
