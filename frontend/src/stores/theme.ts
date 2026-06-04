/**
 * 主题状态:亮/暗模式 + 配色主题,持久化到 localStorage。
 * 亮暗通过 <html>.dark 驱动,配色通过 <html data-color-theme> 驱动。
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'
export type ThemePalette = 'warm' | 'classic' | 'obsidian' | 'ocean' | 'violet' | 'mist'

const MODE_STORAGE_KEY = 'nav-theme'
const PALETTE_STORAGE_KEY = 'nav-color-theme'

export const PALETTE_OPTIONS: Array<{
  value: ThemePalette
  label: string
  description: string
  swatches: string[]
}> = [
  {
    value: 'warm',
    label: '暖砂琥珀',
    description: '更温和的主页光效,弱化蓝色存在感。',
    swatches: ['#92400E', '#D97706', '#F7EFE3'],
  },
  {
    value: 'classic',
    label: '经典蓝',
    description: '保留原来的清冷蓝色玻璃风格。',
    swatches: ['#2563EB', '#60A5FA', '#EEF3FB'],
  },
  {
    value: 'obsidian',
    label: '曜石绿',
    description: '深色高对比工作台,用绿色突出状态与行动。',
    swatches: ['#020617', '#0E1223', '#22C55E'],
  },
  {
    value: 'ocean',
    label: '深海蓝',
    description: '更沉稳的冷色夜间主题,适合长时间浏览。',
    swatches: ['#06111F', '#0B1B2E', '#38BDF8'],
  },
  {
    value: 'violet',
    label: '紫夜金',
    description: '暗紫底色搭配金色点缀,更具个性和高级感。',
    swatches: ['#0F0F23', '#312E81', '#CA8A04'],
  },
  {
    value: 'mist',
    label: '雾白青',
    description: '清爽明亮的日间主题,用青绿色保持专注感。',
    swatches: ['#F0FDFA', '#0D9488', '#EA580C'],
  },
]

/**
 * 读初始主题:localStorage 优先,否则跟随系统偏好。
 *
 * @returns 初始模式
 */
function readInitialMode(): ThemeMode {
  // 1. 用过的主题优先
  const saved = localStorage.getItem(MODE_STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  // 2. 否则看系统是否暗色
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * 读初始配色:默认使用暖色,保留经典蓝可切回。
 *
 * @returns 初始配色
 */
function readInitialPalette(): ThemePalette {
  // 1. 只接受已知配色 key
  const saved = localStorage.getItem(PALETTE_STORAGE_KEY)
  if (
    saved === 'warm' ||
    saved === 'classic' ||
    saved === 'obsidian' ||
    saved === 'ocean' ||
    saved === 'violet' ||
    saved === 'mist'
  ) {
    return saved
  }
  // 2. 新用户默认暖色,符合当前主页改色目标
  return 'warm'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readInitialMode())
  const palette = ref<ThemePalette>(readInitialPalette())

  /**
   * 在亮/暗之间切换。
   */
  function toggle(): void {
    // 1. 翻转模式(watch 会负责落地)
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  /**
   * 设置亮/暗模式。
   *
   * @param nextMode 目标模式
   */
  function setMode(nextMode: ThemeMode): void {
    // 1. 直接写入目标模式
    mode.value = nextMode
  }

  /**
   * 设置配色主题。
   *
   * @param nextPalette 目标配色
   */
  function setPalette(nextPalette: ThemePalette): void {
    // 1. 直接写入目标配色
    palette.value = nextPalette
  }

  // 模式变化即写 <html> class 并持久化;immediate 保证启动就应用
  watch(
    mode,
    (m) => {
      document.documentElement.classList.toggle('dark', m === 'dark')
      localStorage.setItem(MODE_STORAGE_KEY, m)
    },
    { immediate: true },
  )

  // 配色变化即写 <html> dataset 并持久化;CSS token 由该属性分支
  watch(
    palette,
    (p) => {
      document.documentElement.dataset.colorTheme = p
      localStorage.setItem(PALETTE_STORAGE_KEY, p)
    },
    { immediate: true },
  )

  return { mode, palette, toggle, setMode, setPalette }
})
