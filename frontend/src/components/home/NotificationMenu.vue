<script setup lang="ts">
/**
 * 首页通知菜单:铃铛按钮 + 未读 badge + 玻璃下拉列表。
 */
import { ref } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import AppIcon from '@/components/AppIcon.vue'

const notificationStore = useNotificationStore()
const open = ref(false)

/**
 * 切换通知菜单。
 */
async function toggle(): Promise<void> {
  // 1. 打开前刷新通知,确保逾期待办同步进来
  if (!open.value) {
    await notificationStore.load()
  }
  // 2. 切换开关
  open.value = !open.value
}

/**
 * 标记通知已读。
 *
 * @param id 通知 id
 */
async function markRead(id: string): Promise<void> {
  // 1. 标记已读
  await notificationStore.markRead(id)
}
</script>

<template>
  <div class="notify">
    <button type="button" class="top-icon-btn" aria-label="通知" @click="toggle">
      <AppIcon name="bell" :size="22" />
      <span v-if="notificationStore.unreadCount" class="notify__badge">
        {{ notificationStore.unreadCount > 9 ? '9+' : notificationStore.unreadCount }}
      </span>
    </button>

    <template v-if="open">
      <div class="dropdown-backdrop" @click="open = false"></div>
      <div class="notify-menu glass-panel">
        <div class="notify-menu__head">
          <span>通知</span>
          <button type="button" class="notify-menu__action" @click="notificationStore.markAllRead">
            全部已读
          </button>
        </div>

        <div v-if="notificationStore.items.length" class="notify-list">
          <button
            v-for="item in notificationStore.items"
            :key="item.id"
            type="button"
            class="notify-item"
            :class="{ 'notify-item--unread': !item.read }"
            @click="markRead(item.id)"
          >
            <span class="notify-item__dot"></span>
            <span class="notify-item__body">
              <span class="notify-item__title">{{ item.title }}</span>
              <span class="notify-item__content">{{ item.content || '待办已超过计划时间' }}</span>
            </span>
          </button>
        </div>
        <div v-else class="notify-empty">暂无通知</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.notify {
  position: relative;
}

.top-icon-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: var(--home-text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.top-icon-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.notify__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  color: #ffffff;
  text-align: center;
  background: var(--system-blue);
  border-radius: var(--radius-full);
  box-shadow: 0 0 0 2px rgba(3, 14, 35, 0.9);
}

.dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
}

.notify-menu {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: -52px;
  z-index: 21;
  width: 320px;
  padding: var(--space-3);
}

.notify-menu__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
  font-size: var(--text-subhead);
  font-weight: 700;
  color: var(--label-primary);
}

.notify-menu__action {
  padding: 4px 8px;
  font-size: var(--text-caption1);
  color: var(--system-blue);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.notify-item {
  display: flex;
  gap: var(--space-2);
  align-items: flex-start;
  width: 100%;
  padding: var(--space-2);
  text-align: left;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.notify-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.notify-item__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  background: transparent;
  border-radius: var(--radius-full);
}

.notify-item--unread .notify-item__dot {
  background: var(--system-blue);
  box-shadow: 0 0 16px color-mix(in srgb, var(--system-blue) 80%, transparent);
}

.notify-item__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.notify-item__title {
  font-size: var(--text-footnote);
  font-weight: 650;
  color: var(--label-primary);
}

.notify-item__content {
  overflow: hidden;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-empty {
  padding: var(--space-8) 0;
  font-size: var(--text-footnote);
  color: var(--label-secondary);
  text-align: center;
}
</style>
