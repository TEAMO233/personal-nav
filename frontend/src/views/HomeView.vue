<script setup lang="ts">
/**
 * 首页:参考图像素级深色玻璃拟态工作台。
 * 进入时按登录态加载真实数据;未登录仅保留公开搜索入口。
 */
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { useSearchHistoryStore } from '@/stores/searchHistory'
import { useTodoStore } from '@/stores/todo'
import { useNoteStore } from '@/stores/note'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import { useNotificationStore } from '@/stores/notification'
import { ApiClientError } from '@/api/http'
import DynamicBackground from '@/components/home/DynamicBackground.vue'
import HomeTopbar from '@/components/home/HomeTopbar.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import FeaturedShortcutGrid from '@/components/home/FeaturedShortcutGrid.vue'
import DashboardGrid from '@/components/home/DashboardGrid.vue'

const auth = useAuthStore()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()
const searchHistoryStore = useSearchHistoryStore()
const todoStore = useTodoStore()
const noteStore = useNoteStore()
const homeBookmarkStore = useHomeBookmarkStore()
const notificationStore = useNotificationStore()

const loading = ref(true)
const error = ref('')

/**
 * 并发加载首页所有真实数据。
 */
async function loadAll(): Promise<void> {
  // 1. 进入加载态
  loading.value = true
  error.value = ''
  if (!auth.isLoggedIn) {
    loading.value = false
    return
  }
  try {
    // 2. 并发加载首页需要的数据
    await Promise.all([
      engineStore.load(),
      shortcutStore.load(),
      searchHistoryStore.load(),
      todoStore.load(),
      noteStore.load(),
      homeBookmarkStore.load(),
      notificationStore.load(),
    ])
  } catch (e) {
    // 3. 展示错误
    error.value = e instanceof ApiClientError ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="home">
    <DynamicBackground />

    <div class="home__shell">
      <HomeTopbar />

      <main class="home__main">
        <HomeHero class="home__hero" />

        <div v-if="auth.isLoggedIn && loading" class="home__state">加载中…</div>
        <div v-else-if="auth.isLoggedIn && error" class="home__state">
          <p>{{ error }}</p>
          <button type="button" class="retry-btn" @click="loadAll">重试</button>
        </div>
        <template v-else-if="auth.isLoggedIn">
          <FeaturedShortcutGrid class="home__features" />
          <DashboardGrid />
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.home {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 8px;
  overflow-x: hidden;
  background: var(--home-shell-bg);
}

.home__shell {
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 100%;
  height: calc(100vh - 16px);
  min-height: 880px;
  overflow: hidden;
  border: 1px solid var(--home-shell-border);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.018);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.09), 0 28px 90px rgba(0, 0, 0, 0.22);
}

.home__main {
  display: grid;
  flex: 1;
  grid-template-rows: auto auto minmax(0, 1fr);
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: 38px 52px 52px;
}

.home__hero {
  position: relative;
  z-index: 4;
  margin-bottom: 30px;
}

.home__features {
  position: relative;
  z-index: 2;
  margin-bottom: 20px;
}

.home :deep(.dashboard-grid) {
  position: relative;
  z-index: 1;
  min-height: 0;
}

.home :deep(.glass-panel) {
  color: var(--home-text-primary);
  background: var(--home-panel-bg);
  border: 1px solid var(--home-panel-border);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), var(--home-panel-shadow);
  backdrop-filter: blur(28px) saturate(135%);
  -webkit-backdrop-filter: blur(28px) saturate(135%);
}

.home__state {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-12) 0;
  color: var(--home-text-secondary);
}

.retry-btn {
  height: 40px;
  padding: 0 var(--space-5);
  font-size: var(--text-subhead);
  font-weight: 650;
  color: #ffffff;
  background: var(--accent-grad);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
}

@media (max-width: 1024px) {
  .home {
    padding: 0;
  }

  .home__shell {
    height: auto;
    min-height: 100svh;
    border: none;
    border-radius: 0;
  }

  .home__main {
    display: block;
    padding: 28px 16px 32px;
    overflow: hidden;
  }

  .home__hero {
    margin-bottom: 22px;
  }
}

@media (max-width: 430px) {
  .home__main {
    padding: 22px 12px 28px;
  }
}
</style>
