<script setup lang="ts">
/**
 * 登录态桌面首页:高对比石墨玻璃工作台,保留搜索、精选入口和仪表盘。
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
  <div class="desktop-home">
    <DynamicBackground />
    <div class="desktop-home__shell">
      <HomeTopbar />

      <main class="desktop-home__main">
        <section class="desktop-home__hero" aria-label="首页搜索">
          <div class="desktop-home__copy">
            <p class="desktop-home__eyebrow">Personal workspace</p>
            <h1>欢迎回来，{{ auth.user?.username }}</h1>
            <p>从一个搜索入口进入今天的工具、资源与待办。</p>
          </div>
          <SearchBar />
        </section>

        <div v-if="loading" class="desktop-home__state" aria-live="polite">加载中...</div>
        <div v-else-if="error" class="desktop-home__state" role="alert">
          <p>{{ error }}</p>
          <button type="button" class="desktop-home__retry" @click="$emit('retry')">重试</button>
        </div>
        <template v-else-if="isReady">
          <FeaturedShortcutGrid class="desktop-home__features" />
          <DashboardGrid />
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.desktop-home {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 8px;
  overflow-x: hidden;
  background: var(--home-shell-bg);
}

.desktop-home__shell {
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  width: 100%;
  height: calc(100vh - 16px);
  min-height: 880px;
  overflow: hidden;
  background: color-mix(in srgb, var(--home-shell-bg) 92%, #0f172a);
  border: 1px solid var(--home-shell-border);
  border-radius: 24px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.09), 0 28px 90px rgba(0, 0, 0, 0.24);
}

.desktop-home__main {
  display: grid;
  flex: 1;
  grid-template-rows: auto auto minmax(0, 1fr);
  position: relative;
  box-sizing: border-box;
  min-width: 0;
  padding: 34px 52px 52px;
  overflow: visible;
}

.desktop-home__hero {
  display: flex;
  position: relative;
  z-index: 20;
  flex-direction: column;
  gap: 18px;
  align-items: center;
  margin-bottom: 28px;
  text-align: center;
}

.desktop-home__copy {
  min-width: 0;
  max-width: 720px;
}

.desktop-home__eyebrow {
  margin: 0 0 10px;
  font-size: var(--text-caption1);
  font-weight: 750;
  color: var(--feature-blue);
  letter-spacing: 0;
  text-transform: uppercase;
}

.desktop-home__copy h1 {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: clamp(34px, 3vw, 46px);
  font-weight: 800;
  line-height: 1.08;
  color: var(--home-text-primary);
}

.desktop-home__copy p:last-child {
  max-width: 520px;
  margin: 12px auto 0;
  font-size: var(--text-body);
  line-height: 1.55;
  color: var(--home-text-secondary);
}

.desktop-home__hero :deep(.search-bar) {
  width: min(100%, 800px);
  max-width: 800px;
}

.desktop-home__features {
  position: relative;
  z-index: 1;
  margin-bottom: 20px;
}

.desktop-home :deep(.dashboard-grid) {
  position: relative;
  z-index: 0;
  min-height: 0;
}

.desktop-home :deep(.glass-panel) {
  color: var(--home-text-primary);
  background: var(--home-panel-bg);
  border: 1px solid var(--home-panel-border);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), var(--home-panel-shadow);
  backdrop-filter: blur(18px) saturate(135%);
  -webkit-backdrop-filter: blur(18px) saturate(135%);
}

.desktop-home__state {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: var(--home-text-secondary);
}

.desktop-home__retry {
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

</style>
