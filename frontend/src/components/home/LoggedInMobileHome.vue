<script setup lang="ts">
/**
 * 登录态移动首页:极简单列入口,只保留搜索和首页书签。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { useSearchHistoryStore } from '@/stores/searchHistory'
import { useTodoStore } from '@/stores/todo'
import { useNoteStore } from '@/stores/note'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import { useRecentVisitStore } from '@/stores/recentVisit'
import { useNotificationStore } from '@/stores/notification'
import DynamicBackground from './DynamicBackground.vue'
import SearchBar from '@/components/SearchBar.vue'
import HomeBookmarkPanel from './HomeBookmarkPanel.vue'
import AppIcon from '@/components/AppIcon.vue'

defineProps<{
  loading: boolean
  error: string
  isReady: boolean
}>()

defineEmits<{
  retry: []
}>()

const router = useRouter()
const auth = useAuthStore()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()
const searchHistoryStore = useSearchHistoryStore()
const todoStore = useTodoStore()
const noteStore = useNoteStore()
const homeBookmarkStore = useHomeBookmarkStore()
const recentVisitStore = useRecentVisitStore()
const notificationStore = useNotificationStore()

const userMenuOpen = ref(false)

/**
 * 退出登录:销毁会话、清空本地数据、回公开首页。
 */
async function onLogout(): Promise<void> {
  // 1. 收起菜单并登出
  userMenuOpen.value = false
  await auth.logout()
  // 2. 清空本地缓存的数据,避免账号切换后残留
  engineStore.reset()
  shortcutStore.reset()
  searchHistoryStore.reset()
  todoStore.reset()
  noteStore.reset()
  homeBookmarkStore.reset()
  recentVisitStore.reset()
  notificationStore.reset()
  // 3. 回到公开首页
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
 * 打开管理后台。
 */
function onOpenAdmin(): void {
  // 1. 收起菜单并跳转管理后台
  userMenuOpen.value = false
  router.push({ name: 'admin' })
}
</script>

<template>
  <div class="mobile-home">
    <DynamicBackground />
    <div class="mobile-home__shell">
      <div class="mobile-home__account">
        <button
          type="button"
          class="mobile-home__user-trigger"
          aria-label="用户菜单"
          :aria-expanded="userMenuOpen"
          @click="userMenuOpen = !userMenuOpen"
        >
          <span class="mobile-home__avatar">{{ auth.user?.username?.charAt(0).toUpperCase() || 'U' }}</span>
        </button>

        <template v-if="userMenuOpen">
          <div class="mobile-home__dropdown-backdrop" @click="userMenuOpen = false"></div>
          <div class="mobile-home__user-dropdown glass-panel">
            <div class="mobile-home__user-info">
              <div class="mobile-home__user-name">{{ auth.user?.username }}</div>
              <div class="mobile-home__user-role">{{ auth.isAdmin ? '管理员' : '用户' }}</div>
            </div>
            <div class="mobile-home__user-divider"></div>
            <button type="button" class="mobile-home__user-item" @click="onOpenSettings">
              <AppIcon name="settings" :size="18" />
              <span>设置</span>
            </button>
            <button v-if="auth.isAdmin" type="button" class="mobile-home__user-item" @click="onOpenAdmin">
              <AppIcon name="shield" :size="18" />
              <span>管理后台</span>
            </button>
            <button type="button" class="mobile-home__user-item" @click="onLogout">
              <AppIcon name="logout" :size="18" />
              <span>退出登录</span>
            </button>
          </div>
        </template>
      </div>

      <main class="mobile-home__main">
        <section class="mobile-home__search" aria-label="首页搜索">
          <p class="mobile-home__hello">欢迎回来，{{ auth.user?.username }}</p>
          <SearchBar />
        </section>

        <div v-if="loading" class="mobile-home__state" aria-live="polite">加载中...</div>
        <div v-else-if="error" class="mobile-home__state" role="alert">
          <p>{{ error }}</p>
          <button type="button" class="mobile-home__retry" @click="$emit('retry')">重试</button>
        </div>
        <template v-else-if="isReady">
          <HomeBookmarkPanel class="mobile-home__bookmarks" :limit="0" />
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.mobile-home {
  position: relative;
  min-height: 100svh;
  overflow-x: hidden;
  background: var(--home-shell-bg);
}

.mobile-home__shell {
  position: relative;
  z-index: 1;
  min-height: 100svh;
  background: color-mix(in srgb, var(--home-shell-bg) 94%, #0f172a);
}

.mobile-home__main {
  display: flex;
  position: relative;
  flex-direction: column;
  gap: 20px;
  box-sizing: border-box;
  max-width: 520px;
  min-height: 100svh;
  margin: 0 auto;
  padding: 18px 14px max(28px, env(safe-area-inset-bottom));
}

.mobile-home__account {
  position: fixed;
  top: max(14px, env(safe-area-inset-top));
  right: 14px;
  z-index: 12;
}

.mobile-home__user-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-full);
  box-shadow: var(--home-search-shadow);
  cursor: pointer;
  touch-action: manipulation;
  transition: background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.mobile-home__user-trigger:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: var(--home-search-border);
}

.mobile-home__user-trigger:active {
  transform: scale(0.96);
}

.mobile-home__user-trigger:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--system-blue) 34%, transparent);
  outline-offset: 3px;
}

.mobile-home__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  font-size: 16px;
  font-weight: 750;
  color: #ffffff;
  background: var(--avatar-grad);
  border-radius: var(--radius-full);
}

.mobile-home__dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 13;
}

.mobile-home__user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 14;
  width: min(230px, calc(100vw - 28px));
  padding: 8px;
}

.mobile-home__user-info {
  padding: 8px 10px;
}

.mobile-home__user-name {
  overflow: hidden;
  font-size: var(--text-subhead);
  font-weight: 750;
  color: var(--home-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-home__user-role {
  margin-top: 2px;
  font-size: var(--text-caption1);
  color: var(--home-text-secondary);
}

.mobile-home__user-divider {
  height: 1px;
  margin: 4px 0;
  background: var(--home-panel-border);
}

.mobile-home__user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 0 10px;
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--home-text-primary);
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  touch-action: manipulation;
}

.mobile-home__user-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.mobile-home__search {
  display: flex;
  position: relative;
  z-index: 10;
  flex-direction: column;
  gap: 14px;
  padding-top: max(18px, env(safe-area-inset-top));
}

.mobile-home__hello {
  margin: 0;
  padding-right: 58px;
  overflow-wrap: anywhere;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--home-text-primary);
}

.mobile-home__search :deep(.search-bar) {
  min-height: 54px;
}

.mobile-home :deep(.glass-panel) {
  color: var(--home-text-primary);
  background: var(--home-panel-bg);
  border: 1px solid var(--home-panel-border);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), var(--home-panel-shadow);
  backdrop-filter: blur(18px) saturate(135%);
  -webkit-backdrop-filter: blur(18px) saturate(135%);
}

.mobile-home__bookmarks {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
}

.mobile-home__bookmarks :deep(.bookmark__item) {
  min-height: 52px;
}

.mobile-home__state {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: var(--home-text-secondary);
}

.mobile-home__retry {
  min-width: 88px;
  min-height: 44px;
  padding: 0 var(--space-5);
  font-size: var(--text-subhead);
  font-weight: 650;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #18181b);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
}

@media (max-width: 380px) {
  .mobile-home__main {
    padding-inline: 10px;
  }

  .mobile-home__hello {
    font-size: 22px;
  }
}
</style>
