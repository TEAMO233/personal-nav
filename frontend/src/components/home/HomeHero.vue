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
    <h1 class="hero__title">欢迎回来，{{ auth.user?.username }} 👋</h1>
    <p class="hero__subtitle">高效连接你的工具、资源与灵感，开启专注的一天。</p>
    <div class="hero__search">
      <SearchBar />
    </div>
  </section>
</template>

<style scoped>
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.hero__title {
  font-size: clamp(34px, 3.2vw, 46px);
  font-weight: 800;
  line-height: 1.1;
  color: rgba(255, 255, 255, 0.96);
}

.hero__subtitle {
  margin-top: 12px;
  font-size: 17px;
  color: rgba(220, 230, 255, 0.72);
}

.hero__search {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 28px;
}

@media (max-width: 420px) {
  .hero__title {
    font-size: 31px;
  }

  .hero__subtitle {
    max-width: 320px;
    font-size: 16px;
    line-height: 1.55;
  }
}
</style>
