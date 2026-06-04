/**
 * 通知状态:顶栏铃铛列表、未读数与已读操作。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as notificationApi from '@/api/notification'
import type { NotificationItem } from '@/api/types'

export const useNotificationStore = defineStore('notification', () => {
  const items = ref<NotificationItem[]>([])
  const unreadCount = ref(0)
  const loaded = ref(false)

  /**
   * 加载通知列表与未读数。
   */
  async function load(): Promise<void> {
    // 1. 通知列表和未读数一起拉;后端会同步逾期待办通知
    const [list, count] = await Promise.all([
      notificationApi.listNotifications(10),
      notificationApi.getUnreadCount(),
    ])
    items.value = list
    unreadCount.value = count.count
    loaded.value = true
  }

  /**
   * 标记单条已读。
   *
   * @param id 通知 id
   */
  async function markRead(id: string): Promise<void> {
    // 1. 标记后替换本地项并刷新未读数
    const updated = await notificationApi.markNotificationRead(id)
    const i = items.value.findIndex((item) => item.id === id)
    if (i >= 0) items.value[i] = updated
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  /**
   * 全部标记已读。
   */
  async function markAllRead(): Promise<void> {
    // 1. 后端全部已读后同步本地状态
    await notificationApi.markAllNotificationsRead()
    items.value = items.value.map((item) => ({ ...item, read: true }))
    unreadCount.value = 0
  }

  /**
   * 清空本地状态。
   */
  function reset(): void {
    items.value = []
    unreadCount.value = 0
    loaded.value = false
  }

  return { items, unreadCount, loaded, load, markRead, markAllRead, reset }
})
