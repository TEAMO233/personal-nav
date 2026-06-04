<script setup lang="ts">
/**
 * 首页顶栏:左侧品牌,右侧搜索 / 通知 / 主题 / 用户胶囊菜单。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { useSearchHistoryStore } from '@/stores/searchHistory'
import { useTodoStore } from '@/stores/todo'
import { useNoteStore } from '@/stores/note'
import { useRecentVisitStore } from '@/stores/recentVisit'
import { useNotificationStore } from '@/stores/notification'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AppIcon from '@/components/AppIcon.vue'
import NotificationMenu from '@/components/home/NotificationMenu.vue'

const router = useRouter()
const auth = useAuthStore()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()
const searchHistoryStore = useSearchHistoryStore()
const todoStore = useTodoStore()
const noteStore = useNoteStore()
const recentVisitStore = useRecentVisitStore()
const notificationStore = useNotificationStore()

const userMenuOpen = ref(false)

/**
 * 触发搜索栏聚焦快捷键。
 */
function focusSearch(): void {
  // 1. 复用 SearchBar 的全局快捷键监听
  window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', metaKey: true }))
}

/**
 * 退出登录:销毁会话、清空本地数据、回登录页。
 */
async function onLogout(): Promise<void> {
  // 1. 收起菜单并登出
  userMenuOpen.value = false
  await auth.logout()
  // 2. 清空本地缓存的数据,避免残留
  engineStore.reset()
  shortcutStore.reset()
  searchHistoryStore.reset()
  todoStore.reset()
  noteStore.reset()
  recentVisitStore.reset()
  notificationStore.reset()
  // 3. 回登录页
  router.replace({ name: 'login' })
}

/**
 * 打开设置页。
 */
function onOpenSettings(): void {
  // 1. 收起菜单并跳转设置页
  userMenuOpen.value = false
  router.push({ name: 'settings' })
}

/**
 * 打开管理后台(仅 ADMIN 菜单可见)。
 */
function onOpenAdmin(): void {
  // 1. 收起菜单并跳转管理后台
  userMenuOpen.value = false
  router.push({ name: 'admin' })
}
</script>

<template>
  <header class="topbar">
    <div class="topbar__brand">
      <span class="brand-mark">
        <AppIcon name="star" :size="18" />
      </span>
      <span>个人导航</span>
    </div>

    <div class="topbar__actions">
      <button type="button" class="top-icon-btn" aria-label="搜索" @click="focusSearch">
        <AppIcon name="search" :size="22" />
      </button>

      <NotificationMenu />

      <ThemeToggle />

      <div class="user-menu">
        <button
          type="button"
          class="user-trigger"
          aria-label="用户菜单"
          @click="userMenuOpen = !userMenuOpen"
        >
          <span class="user-avatar">{{ auth.user?.username?.charAt(0).toUpperCase() || 'A' }}</span>
          <span class="user-name">{{ auth.user?.username }}</span>
          <AppIcon name="chevron-down" :size="14" />
        </button>

        <template v-if="userMenuOpen">
          <div class="dropdown-backdrop" @click="userMenuOpen = false"></div>
          <div class="user-dropdown glass-panel">
            <div class="user-info">
              <div class="user-info__name">{{ auth.user?.username }}</div>
              <div class="user-info__role">{{ auth.isAdmin ? '管理员' : '用户' }}</div>
            </div>
            <div class="user-divider"></div>
            <button type="button" class="user-item" @click="onOpenSettings">
              <AppIcon name="settings" :size="18" />
              <span>设置</span>
            </button>
            <button v-if="auth.isAdmin" type="button" class="user-item" @click="onOpenAdmin">
              <AppIcon name="shield" :size="18" />
              <span>管理后台</span>
            </button>
            <button type="button" class="user-item" @click="onLogout">
              <AppIcon name="logout" :size="18" />
              <span>退出登录</span>
            </button>
          </div>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: relative;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 32px;
  background:
    linear-gradient(180deg, rgba(4, 15, 34, 0.58) 0%, rgba(4, 15, 34, 0.26) 68%, rgba(4, 15, 34, 0.08) 100%),
    linear-gradient(90deg, rgba(35, 95, 180, 0.1), rgba(24, 55, 110, 0.04) 58%, rgba(5, 18, 42, 0.08));
  border: none;
  border-radius: 22px 22px 0 0;
}

.topbar::before {
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, transparent, rgba(190, 215, 255, 0.2), transparent);
  opacity: 0.42;
}

.topbar__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  white-space: nowrap;
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.94);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #ffffff;
  background: linear-gradient(135deg, #69a8ff, #1d5eff);
  border-radius: 11px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 10px 25px rgba(37, 99, 235, 0.35);
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.top-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: rgba(235, 242, 255, 0.86);
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

:deep(.theme-toggle) {
  color: rgba(235, 242, 255, 0.86);
}

:deep(.theme-toggle:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.user-menu {
  position: relative;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 14px 0 6px;
  color: rgba(255, 255, 255, 0.94);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default);
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(160, 190, 255, 0.32);
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #7c82ff, #4451c9);
  border-radius: var(--radius-full);
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  font-size: 15px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: 0;
  z-index: 21;
  min-width: 210px;
  padding: var(--space-2);
}

.user-info {
  padding: var(--space-2) var(--space-3);
}

.user-info__name {
  font-size: var(--text-subhead);
  font-weight: 700;
  color: var(--label-primary);
}

.user-info__role {
  font-size: var(--text-caption1);
  color: var(--label-secondary);
}

.user-divider {
  height: 1px;
  margin: var(--space-1) 0;
  background: var(--separator);
}

.user-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-subhead);
  color: var(--label-primary);
  text-align: left;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.user-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

@media (max-width: 768px) {
  .topbar {
    height: auto;
    min-height: 64px;
    padding: 10px 16px;
    border-radius: 0;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 420px) {
  .topbar {
    min-height: 72px;
    padding: 10px 12px;
  }

  .topbar__brand {
    gap: 8px;
    font-size: 18px;
  }

  .brand-mark {
    width: 32px;
    height: 32px;
    border-radius: 10px;
  }

  .topbar__actions {
    gap: 8px;
  }

  .top-icon-btn,
  :deep(.top-icon-btn),
  :deep(.theme-toggle) {
    width: 40px;
    height: 40px;
  }

  .user-trigger {
    gap: 6px;
    height: 40px;
    padding: 0 8px 0 4px;
  }

  .user-avatar {
    width: 30px;
    height: 30px;
  }
}
</style>
