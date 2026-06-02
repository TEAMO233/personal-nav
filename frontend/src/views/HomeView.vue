<script setup lang="ts">
/**
 * 首页:顶栏(品牌 + 主题切换 + 用户菜单) + 中央搜索栏 + 下方快捷方式网格。
 * 进入时并发加载引擎、分组、快捷方式。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { ApiClientError } from '@/api/http'
import SearchBar from '@/components/SearchBar.vue'
import GroupGrid from '@/components/GroupGrid.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()

const loading = ref(true)
const error = ref('')
const userMenuOpen = ref(false)

/**
 * 并发加载首页所需的引擎、分组与快捷方式。
 */
async function loadAll(): Promise<void> {
  // 1. 进入加载态
  loading.value = true
  error.value = ''
  try {
    // 2. 三类数据一起拉(分组与快捷方式在 shortcutStore 内已并发)
    await Promise.all([engineStore.load(), shortcutStore.load()])
  } catch (e) {
    // 3. 展示错误(401 会被拦截器自动跳登录,这里多为网络/服务异常)
    error.value = e instanceof ApiClientError ? e.message : '加载失败,请稍后重试'
  } finally {
    loading.value = false
  }
}

/**
 * 退出登录:销毁会话、清空本地数据、回登录页。
 */
async function onLogout(): Promise<void> {
  // 1. 收起菜单并登出
  userMenuOpen.value = false
  await auth.logout()
  // 2. 清空本地缓存的引擎与快捷方式,避免残留
  engineStore.reset()
  shortcutStore.reset()
  // 3. 回登录页
  router.replace({ name: 'login' })
}

onMounted(loadAll)
</script>

<template>
  <div class="home">
    <!-- 顶栏:毛玻璃材质,随滚动吸顶 -->
    <header class="home__topbar">
      <div class="home__brand">个人导航</div>
      <div class="home__actions">
        <ThemeToggle />
        <!-- 用户菜单 -->
        <div class="user-menu">
          <button
            type="button"
            class="user-trigger"
            aria-label="用户菜单"
            @click="userMenuOpen = !userMenuOpen"
          >
            <AppIcon name="user" :size="20" />
            <span class="user-name">{{ auth.user?.username }}</span>
            <AppIcon name="chevron-down" :size="14" />
          </button>

          <template v-if="userMenuOpen">
            <div class="dropdown-backdrop" @click="userMenuOpen = false"></div>
            <div class="user-dropdown">
              <!-- 当前用户信息 -->
              <div class="user-info">
                <div class="user-info__name">{{ auth.user?.username }}</div>
                <div class="user-info__role">{{ auth.isAdmin ? '管理员' : '用户' }}</div>
              </div>
              <div class="user-divider"></div>
              <!-- 设置入口(M7 上线前禁用) -->
              <button type="button" class="user-item user-item--disabled" disabled>
                <AppIcon name="settings" :size="18" />
                <span>设置</span>
                <span class="user-item__soon">即将上线</span>
              </button>
              <!-- 退出登录 -->
              <button type="button" class="user-item" @click="onLogout">
                <AppIcon name="logout" :size="18" />
                <span>退出登录</span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </header>

    <!-- 主体 -->
    <main class="home__main">
      <!-- 搜索栏 -->
      <div class="home__search">
        <SearchBar />
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="home__state">加载中…</div>
      <!-- 错误态 -->
      <div v-else-if="error" class="home__state">
        <p class="home__state-text">{{ error }}</p>
        <button type="button" class="retry-btn" @click="loadAll">重试</button>
      </div>
      <!-- 内容:快捷方式网格 -->
      <GroupGrid v-else class="home__grid" />
    </main>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* 顶栏 */
.home__topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 var(--space-5);
  background: var(--material-bar);
  /* 毛玻璃材质;不支持时退化为半透明底色仍可读 */
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 0.5px solid var(--separator);
}

.home__brand {
  font-size: var(--text-headline);
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--label-primary);
}

.home__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* 用户菜单 */
.user-menu {
  position: relative;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 40px;
  padding: 0 var(--space-2);
  color: var(--label-primary);
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.user-trigger:hover {
  background: var(--bg-secondary);
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  font-size: var(--text-subhead);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: 0;
  z-index: 11;
  min-width: 200px;
  padding: var(--space-1);
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
}

.user-info {
  padding: var(--space-2) var(--space-3);
}

.user-info__name {
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--label-primary);
}

.user-info__role {
  font-size: var(--text-caption1);
  color: var(--label-secondary);
}

.user-divider {
  height: 0.5px;
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
  transition: background-color var(--duration-fast) var(--ease-default);
}

.user-item:hover:not(:disabled) {
  background: var(--bg-secondary);
}

.user-item--disabled {
  color: var(--label-tertiary);
  cursor: default;
}

.user-item__soon {
  margin-left: auto;
  font-size: var(--text-caption2);
  color: var(--label-tertiary);
}

/* 下拉遮罩:铺满视口,点击收起 */
.dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10;
}

/* 主体 */
.home__main {
  display: flex;
  flex-direction: column;
  flex: 1;
  box-sizing: border-box;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5) var(--space-12);
}

.home__search {
  display: flex;
  justify-content: center;
  padding: 8vh 0 var(--space-12);
}

.home__grid {
  width: 100%;
}

.home__state {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-12) 0;
  color: var(--label-secondary);
}

.home__state-text {
  margin: 0;
}

.retry-btn {
  height: 40px;
  padding: 0 var(--space-5);
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--system-blue);
  background: color-mix(in srgb, var(--system-blue) 12%, transparent);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
}

/* 移动端 */
@media (max-width: 640px) {
  .home__topbar {
    padding: 0 var(--space-3);
  }

  .user-name {
    display: none;
  }

  .home__main {
    padding: var(--space-4) var(--space-3) var(--space-10);
  }

  .home__search {
    padding: 6vh 0 var(--space-8);
  }
}
</style>
