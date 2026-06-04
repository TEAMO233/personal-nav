<script setup lang="ts">
/**
 * 设置页:顶栏(返回首页 + 主题切换) + 选项卡切换外观、搜索引擎、快捷方式等管理区。
 * 进入时确保引擎、分组、快捷方式已加载(直接访问/刷新时需要)。
 */
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import { ApiClientError } from '@/api/http'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AppIcon from '@/components/AppIcon.vue'
import EngineSection from '@/components/settings/EngineSection.vue'
import ShortcutSection from '@/components/settings/ShortcutSection.vue'
import HomeBookmarkSection from '@/components/settings/HomeBookmarkSection.vue'
import AppearanceSection from '@/components/settings/AppearanceSection.vue'

const route = useRoute()
const router = useRouter()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()
const homeBookmarkStore = useHomeBookmarkStore()

type SettingsTab = 'appearance' | 'engines' | 'shortcuts' | 'homeBookmarks'

const activeTab = ref<SettingsTab>('appearance')
const error = ref('')

/**
 * 确保引擎与快捷方式数据已加载。
 */
async function ensureLoaded(): Promise<void> {
  // 1. 已加载的跳过,未加载的并发拉取
  error.value = ''
  try {
    await Promise.all([
      engineStore.loaded ? Promise.resolve() : engineStore.load(),
      shortcutStore.loaded ? Promise.resolve() : shortcutStore.load(),
      homeBookmarkStore.allLoaded ? Promise.resolve() : homeBookmarkStore.loadAll(),
    ])
  } catch (e) {
    // 2. 失败展示错误(401 会被拦截器跳登录)
    error.value = e instanceof ApiClientError ? e.message : '加载失败,请稍后重试'
  }
}

/**
 * 返回首页。
 */
function goHome(): void {
  // 1. 回首页
  router.push({ name: 'home' })
}

/**
 * 从 query 读取默认标签。
 */
function syncTabFromQuery(): void {
  // 1. 只接受已知标签
  const tab = route.query.tab
  if (tab === 'appearance' || tab === 'engines' || tab === 'shortcuts' || tab === 'homeBookmarks') {
    activeTab.value = tab
  }
}

onMounted(() => {
  syncTabFromQuery()
  void ensureLoaded()
})
watch(() => route.query.tab, syncTabFromQuery)
</script>

<template>
  <div class="settings">
    <!-- 顶栏:返回 + 标题 + 主题切换 -->
    <header class="settings__topbar">
      <button type="button" class="back-btn" @click="goHome">
        <AppIcon name="arrow-left" :size="20" />
        <span class="back-btn__text">首页</span>
      </button>
      <h1 class="settings__title">设置</h1>
      <ThemeToggle />
    </header>

    <!-- 主体 -->
    <main class="settings__main">
      <!-- 选项卡:外观不依赖接口,资源管理区失败时在标签内提示 -->
      <el-tabs v-model="activeTab" class="settings__tabs">
        <el-tab-pane label="外观" name="appearance">
          <AppearanceSection />
        </el-tab-pane>
        <el-tab-pane label="搜索引擎" name="engines">
          <div v-if="error" class="settings__state">
            <p>{{ error }}</p>
            <el-button type="primary" round @click="ensureLoaded">重试</el-button>
          </div>
          <EngineSection v-else />
        </el-tab-pane>
        <el-tab-pane label="快捷方式" name="shortcuts">
          <div v-if="error" class="settings__state">
            <p>{{ error }}</p>
            <el-button type="primary" round @click="ensureLoaded">重试</el-button>
          </div>
          <ShortcutSection v-else />
        </el-tab-pane>
        <el-tab-pane label="首页书签" name="homeBookmarks">
          <div v-if="error" class="settings__state">
            <p>{{ error }}</p>
            <el-button type="primary" round @click="ensureLoaded">重试</el-button>
          </div>
          <HomeBookmarkSection v-else />
        </el-tab-pane>
      </el-tabs>
    </main>
  </div>
</template>

<style scoped>
.settings {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* 顶栏:毛玻璃吸顶 */
.settings__topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 56px;
  padding: 0 var(--space-4);
  background: var(--material-bar);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 0.5px solid var(--separator);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 40px;
  padding: 0 var(--space-2);
  color: var(--system-blue);
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.back-btn:hover {
  background: var(--bg-secondary);
}

.settings__title {
  flex: 1;
  font-size: var(--text-body);
  font-weight: 600;
  text-align: center;
}

/* 主体容器 */
.settings__main {
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5) var(--space-12);
}

.settings__state {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-12) 0;
  color: var(--label-secondary);
}

@media (max-width: 640px) {
  .settings__topbar {
    padding: 0 var(--space-3);
  }

  .back-btn__text {
    display: none;
  }

  .settings__main {
    padding: var(--space-4) var(--space-3) var(--space-10);
  }
}
</style>
