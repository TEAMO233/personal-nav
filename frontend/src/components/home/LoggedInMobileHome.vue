<script setup lang="ts">
/**
 * 登录态移动首页:独立单列工作台,优先搜索与常用入口。
 */
import { useAuthStore } from '@/stores/auth'
import DynamicBackground from './DynamicBackground.vue'
import HomeTopbar from './HomeTopbar.vue'
import SearchBar from '@/components/SearchBar.vue'
import FeaturedShortcutGrid from './FeaturedShortcutGrid.vue'
import DashboardGrid from './DashboardGrid.vue'

defineProps<{
  loading: boolean
  error: string
  isReady: boolean
}>()

defineEmits<{
  retry: []
}>()

const auth = useAuthStore()
</script>

<template>
  <div class="mobile-home">
    <DynamicBackground />
    <div class="mobile-home__shell">
      <HomeTopbar />

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
          <section class="mobile-home__section" aria-label="快捷入口">
            <h2>快捷入口</h2>
            <FeaturedShortcutGrid />
          </section>
          <section class="mobile-home__section" aria-label="工作台">
            <h2>工作台</h2>
            <DashboardGrid />
          </section>
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
  flex-direction: column;
  gap: 22px;
  box-sizing: border-box;
  padding: 18px 14px max(28px, env(safe-area-inset-bottom));
}

.mobile-home__search {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mobile-home__hello {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--home-text-primary);
}

.mobile-home__search :deep(.search-bar) {
  min-height: 54px;
}

.mobile-home__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.mobile-home__section h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 750;
  color: var(--home-text-primary);
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

.mobile-home :deep(.dashboard-grid) {
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
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
