<script setup lang="ts">
/**
 * 首页顶栏:左侧品牌,右侧搜索 / 通知 / 主题 / 用户入口。
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
 * 点击用户入口:未登录直接去登录页,已登录展开菜单。
 */
function onUserTrigger(): void {
  // 1. 匿名态把头像作为登录入口
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }
  // 2. 登录态正常展开菜单
  userMenuOpen.value = !userMenuOpen.value
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
  // 3. 留在公开首页
  router.replace({ name: 'home' })
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

      <NotificationMenu v-if="auth.isLoggedIn" />

      <ThemeToggle />

      <div class="user-menu">
        <button
          type="button"
          class="user-trigger"
          :aria-label="auth.isLoggedIn ? '用户菜单' : '登录'"
          @click="onUserTrigger"
        >
          <span class="user-avatar">
            <template v-if="auth.isLoggedIn">
              {{ auth.user?.username?.charAt(0).toUpperCase() || 'U' }}
            </template>
            <AppIcon v-else name="user" :size="18" />
          </span>
          <span v-if="auth.isLoggedIn" class="user-name">{{ auth.user?.username }}</span>
          <AppIcon v-if="auth.isLoggedIn" name="chevron-down" :size="14" />
        </button>

        <template v-if="auth.isLoggedIn && userMenuOpen">
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
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 64px;
  padding: 0 32px;
  background: var(--home-topbar-bg);
  border: none;
  border-radius: 22px 22px 0 0;
}

.topbar::before {
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, transparent, var(--home-line-highlight), transparent);
  opacity: 0.42;
}

.topbar__brand {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 14px;
  min-width: 0;
  white-space: nowrap;
  font-size: 20px;
  font-weight: 700;
  color: var(--home-text-primary);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #ffffff;
  background: var(--brand-grad);
  border-radius: 11px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 10px 25px var(--brand-shadow);
}

.topbar__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 14px;
}

.top-icon-btn {
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
  color: var(--home-text-primary);
  background: var(--home-surface-hover);
}

:deep(.theme-toggle) {
  color: var(--home-text-secondary);
}

:deep(.theme-toggle:hover) {
  color: var(--home-text-primary);
  background: var(--home-surface-hover);
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
  color: var(--home-text-primary);
  background: var(--home-control-bg);
  border: 1px solid var(--home-control-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default);
}

.user-trigger:hover {
  background: var(--home-surface-hover);
  border-color: var(--home-search-border);
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
  background: var(--avatar-grad);
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
  background: var(--home-surface-hover);
}

@media (max-width: 768px) {
  .topbar {
    height: auto;
    min-height: 58px;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 0;
  }

  .topbar__brand {
    gap: 10px;
    font-size: 18px;
  }

  .topbar__brand > span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .topbar__actions {
    gap: 4px;
  }

  .top-icon-btn,
  :deep(.top-icon-btn),
  :deep(.theme-toggle) {
    width: 38px;
    height: 38px;
  }

  .user-trigger {
    gap: 0;
    width: 38px;
    height: 38px;
    padding: 0;
    justify-content: center;
  }

  .user-trigger > svg {
    display: none;
  }

  .user-name {
    display: none;
  }

  .user-avatar {
    width: 30px;
    height: 30px;
  }
}

@media (max-width: 420px) {
  .topbar {
    min-height: 56px;
    padding: 8px 10px;
  }

  .topbar__brand {
    gap: 8px;
    font-size: 17px;
  }

  .brand-mark {
    width: 30px;
    height: 30px;
    border-radius: 9px;
  }

  .top-icon-btn,
  :deep(.top-icon-btn),
  :deep(.theme-toggle) {
    width: 34px;
    height: 34px;
  }

  .user-trigger {
    width: 34px;
    height: 34px;
  }

  .user-avatar {
    width: 28px;
    height: 28px;
  }
}
</style>
