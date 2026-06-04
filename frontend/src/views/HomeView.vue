<script setup lang="ts">
/**
 * 首页路由容器:集中加载登录态数据,再按登录态和设备类型渲染四套首页体验。
 */
import { computed, onMounted, watch, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useEngineStore } from '@/stores/engine'
import { useShortcutStore } from '@/stores/shortcut'
import { useSearchHistoryStore } from '@/stores/searchHistory'
import { useTodoStore } from '@/stores/todo'
import { useNoteStore } from '@/stores/note'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import { useNotificationStore } from '@/stores/notification'
import { useIsMobile } from '@/composables/useIsMobile'
import { ApiClientError } from '@/api/http'
import GuestDesktopHome from '@/components/home/GuestDesktopHome.vue'
import GuestMobileHome from '@/components/home/GuestMobileHome.vue'
import LoggedInDesktopHome from '@/components/home/LoggedInDesktopHome.vue'
import LoggedInMobileHome from '@/components/home/LoggedInMobileHome.vue'

const auth = useAuthStore()
const engineStore = useEngineStore()
const shortcutStore = useShortcutStore()
const searchHistoryStore = useSearchHistoryStore()
const todoStore = useTodoStore()
const noteStore = useNoteStore()
const homeBookmarkStore = useHomeBookmarkStore()
const notificationStore = useNotificationStore()
const { isMobile } = useIsMobile('(max-width: 760px)')

const loading = ref(true)
const error = ref('')
const isReady = computed(() => !auth.isLoggedIn || (!loading.value && !error.value))

/**
 * 并发加载首页所有真实数据。
 */
async function loadAll(): Promise<void> {
  // 1. 访客首页只需要公开搜索,不加载私有数据
  if (!auth.isLoggedIn) {
    loading.value = false
    error.value = ''
    return
  }

  // 2. 登录态进入加载态
  loading.value = true
  error.value = ''
  try {
    // 3. 并发加载首页需要的数据
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
    // 4. 展示错误
    error.value = e instanceof ApiClientError ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// 登录态切换时同步首页数据,退出后回到公开搜索页
watch(
  () => auth.isLoggedIn,
  () => {
    void loadAll()
  },
)
</script>

<template>
  <GuestMobileHome v-if="!auth.isLoggedIn && isMobile" />
  <GuestDesktopHome v-else-if="!auth.isLoggedIn" />
  <LoggedInMobileHome
    v-else-if="isMobile"
    :loading="loading"
    :error="error"
    :is-ready="isReady"
    @retry="loadAll"
  />
  <LoggedInDesktopHome v-else :loading="loading" :error="error" :is-ready="isReady" @retry="loadAll" />
</template>
