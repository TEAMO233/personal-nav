<script setup lang="ts">
/**
 * 首页欢迎区:大号欢迎语 + 副标题 + 核心搜索栏。
 */
import { useAuthStore } from '@/stores/auth'
import SearchBar from '@/components/SearchBar.vue'

const auth = useAuthStore()
</script>

<template>
  <section class="hero">
    <h1 v-if="auth.isLoggedIn" class="hero__title">欢迎回来，{{ auth.user?.username }} 👋</h1>
    <p v-if="auth.isLoggedIn" class="hero__subtitle">高效连接你的工具、资源与灵感，开启专注的一天。</p>
    <div class="hero__search" :class="{ 'hero__search--solo': !auth.isLoggedIn }">
      <SearchBar />
    </div>
  </section>
</template>

<style scoped>
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  min-width: 0;
  text-align: center;
}

.hero__title {
  width: 100%;
  max-width: 100%;
  overflow-wrap: anywhere;
  font-size: clamp(34px, 3.2vw, 46px);
  font-weight: 800;
  line-height: 1.1;
  color: var(--home-text-primary);
}

.hero__subtitle {
  width: 100%;
  max-width: 680px;
  margin-top: 12px;
  overflow-wrap: anywhere;
  font-size: 17px;
  color: var(--home-text-secondary);
}

.hero__search {
  display: flex;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  margin-top: 28px;
}

.hero__search--solo {
  max-width: min(680px, 100%);
  margin-top: 0;
}

.hero__search--solo :deep(.search-bar) {
  height: 58px;
  max-width: 100%;
  background: color-mix(in srgb, var(--home-search-bg) 76%, transparent);
  border-color: color-mix(in srgb, var(--home-search-border) 82%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 18px 46px rgba(0, 0, 0, 0.2);
}

@media (max-width: 420px) {
  .hero__title {
    font-size: 30px;
    line-height: 1.18;
  }

  .hero__subtitle {
    max-width: 100%;
    font-size: 16px;
    line-height: 1.55;
  }

  .hero__search--solo :deep(.search-bar) {
    height: 52px;
  }
}

@media (max-width: 360px) {
  .hero__title {
    font-size: 27px;
  }
}
</style>
