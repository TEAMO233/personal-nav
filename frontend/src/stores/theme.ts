/**
 * 主题状态:亮/暗模式,持久化到 localStorage,首次按系统偏好初始化。
 * 切换通过给 <html> 加/去 dark class 实现(同时驱动本项目变量与 Element Plus 暗色)。
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'nav-theme'

/**
 * 读初始主题:localStorage 优先,否则跟随系统偏好。
 *
 * @returns 初始模式
 */
function readInitialMode(): ThemeMode {
  // 1. 用过的主题优先
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  // 2. 否则看系统是否暗色
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readInitialMode())

  /**
   * 在亮/暗之间切换。
   */
  function toggle(): void {
    // 1. 翻转模式(watch 会负责落地)
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  // 模式变化即写 <html> class 并持久化;immediate 保证启动就应用
  watch(
    mode,
    (m) => {
      document.documentElement.classList.toggle('dark', m === 'dark')
      localStorage.setItem(STORAGE_KEY, m)
    },
    { immediate: true },
  )

  return { mode, toggle }
})
