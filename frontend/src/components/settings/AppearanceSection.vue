<script setup lang="ts">
/**
 * 外观设置区:配置亮暗模式与主页配色主题。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { PALETTE_OPTIONS, useThemeStore, type ThemeMode, type ThemePalette } from '@/stores/theme'
import AppIcon from '@/components/AppIcon.vue'

const theme = useThemeStore()
const router = useRouter()
const homeUrl = computed(() => new URL(router.resolve({ name: 'home' }).href, window.location.origin).toString())
const extensionDirectory = 'chrome-extension/personal-nav-new-tab'

const modeOptions: Array<{ value: ThemeMode; label: string; icon: 'sun' | 'moon' }> = [
  { value: 'light', label: '亮色', icon: 'sun' },
  { value: 'dark', label: '暗色', icon: 'moon' },
]

/**
 * 设置亮暗模式。
 *
 * @param mode 目标模式
 */
function chooseMode(mode: ThemeMode): void {
  // 1. 写入全局主题 store
  theme.setMode(mode)
}

/**
 * 设置配色主题。
 *
 * @param palette 目标配色
 */
function choosePalette(palette: ThemePalette): void {
  // 1. 写入全局主题 store
  theme.setPalette(palette)
}

/**
 * 复制导航首页地址。
 */
async function copyHomeUrl(): Promise<boolean> {
  // 1. 优先使用现代剪贴板 API
  try {
    await navigator.clipboard.writeText(homeUrl.value)
    ElMessage.success('首页地址已复制')
    return true
  } catch {
    // 2. 剪贴板不可用时提示手动复制
    ElMessage.error('复制失败,请手动选择地址复制')
    return false
  }
}

/**
 * 复制地址并打开 Chrome 启动页设置。
 */
async function openChromeStartupSettings(): Promise<void> {
  // 1. 先复制地址,再尝试打开 Chrome 设置页
  await copyHomeUrl()
  window.open('chrome://settings/onStartup', '_blank', 'noopener')
}

/**
 * 复制扩展目录。
 */
async function copyExtensionDirectory(): Promise<boolean> {
  // 1. 复制仓库内的扩展目录,方便在 Chrome 开发者模式中选择
  try {
    await navigator.clipboard.writeText(extensionDirectory)
    ElMessage.success('扩展目录已复制')
    return true
  } catch {
    // 2. 剪贴板不可用时提示手动复制
    ElMessage.error('复制失败,请手动选择目录复制')
    return false
  }
}

/**
 * 打开 Chrome 扩展管理页。
 */
async function openChromeExtensions(): Promise<void> {
  // 1. 先复制扩展目录,再打开扩展管理页
  await copyExtensionDirectory()
  window.open('chrome://extensions/', '_blank', 'noopener')
}
</script>

<template>
  <section class="appearance-section">
    <!-- 区头 -->
    <header class="section-head">
      <div>
        <h2 class="section-head__title">外观</h2>
        <p class="section-head__hint">调整明暗模式和主页配色,选择会自动保存在当前浏览器。</p>
      </div>
    </header>

    <!-- 明暗模式 -->
    <div class="setting-group">
      <div class="setting-group__head">
        <h3>显示模式</h3>
      </div>
      <div class="mode-segment" role="group" aria-label="显示模式">
        <button
          v-for="item in modeOptions"
          :key="item.value"
          type="button"
          class="mode-segment__item"
          :class="{ 'mode-segment__item--active': theme.mode === item.value }"
          :aria-pressed="theme.mode === item.value"
          @click="chooseMode(item.value)"
        >
          <AppIcon :name="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </div>

    <!-- 配色主题 -->
    <div class="setting-group">
      <div class="setting-group__head">
        <h3>配色主题</h3>
      </div>
      <div class="palette-list" role="radiogroup" aria-label="配色主题">
        <button
          v-for="item in PALETTE_OPTIONS"
          :key="item.value"
          type="button"
          class="palette-card"
          :class="{ 'palette-card--active': theme.palette === item.value }"
          role="radio"
          :aria-checked="theme.palette === item.value"
          @click="choosePalette(item.value)"
        >
          <span class="palette-card__swatches" aria-hidden="true">
            <span
              v-for="swatch in item.swatches"
              :key="swatch"
              class="palette-card__swatch"
              :style="{ backgroundColor: swatch }"
            ></span>
          </span>
          <span class="palette-card__body">
            <span class="palette-card__title">{{ item.label }}</span>
            <span class="palette-card__desc">{{ item.description }}</span>
          </span>
          <span class="palette-card__check" aria-hidden="true">
            <AppIcon v-if="theme.palette === item.value" name="check" :size="16" />
          </span>
        </button>
      </div>
    </div>

    <!-- Chrome 浏览器入口 -->
    <div class="setting-group">
      <div class="setting-group__head">
        <h3>浏览器入口</h3>
      </div>
      <div class="startup-card">
        <div class="startup-card__body">
          <span class="startup-card__eyebrow">Chrome</span>
          <strong>把个人导航放到 Chrome 常用入口</strong>
          <p>启动时打开可用 Chrome 原生设置;新建标签页需要加载本仓库里的本地扩展。</p>
          <code class="startup-card__url">{{ homeUrl }}</code>
          <div class="startup-card__options" aria-label="Chrome 设置方式">
            <div class="startup-option">
              <span class="startup-option__label">启动时打开</span>
              <span class="startup-option__text">复制地址后,在 Chrome “启动时”里选择打开特定网页。</span>
            </div>
            <div class="startup-option">
              <span class="startup-option__label">新建标签页</span>
              <span class="startup-option__text">打开扩展管理页,启用开发者模式并加载 <code>{{ extensionDirectory }}</code>。</span>
            </div>
          </div>
        </div>
        <div class="startup-card__actions">
          <button type="button" class="startup-card__primary" @click="openChromeStartupSettings">
            <AppIcon name="external-link" :size="16" />
            <span>启动页设置</span>
          </button>
          <button type="button" class="startup-card__secondary" @click="openChromeExtensions">
            <AppIcon name="external-link" :size="16" />
            <span>扩展管理</span>
          </button>
          <button type="button" class="startup-card__secondary" @click="copyExtensionDirectory">
            <AppIcon name="copy" :size="16" />
            <span>复制目录</span>
          </button>
          <button type="button" class="startup-card__secondary" @click="copyHomeUrl">
            <AppIcon name="copy" :size="16" />
            <span>复制地址</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.appearance-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-1);
}

.section-head__title {
  font-size: var(--text-title2);
  font-weight: 700;
}

.section-head__hint {
  margin-top: var(--space-1);
  font-size: var(--text-footnote);
  color: var(--label-secondary);
}

.setting-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.setting-group__head h3 {
  font-size: var(--text-subhead);
  font-weight: 650;
}

.mode-segment {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-1);
  padding: var(--space-1);
  background: var(--bg-tertiary);
  border: 1px solid var(--separator);
  border-radius: var(--radius-lg);
}

.mode-segment__item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  min-height: 44px;
  font-family: inherit;
  font-size: var(--text-subhead);
  font-weight: 650;
  color: var(--label-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
}

.mode-segment__item:hover {
  color: var(--label-primary);
}

.mode-segment__item--active {
  color: var(--label-primary);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
}

.palette-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.palette-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 24px;
  gap: var(--space-3);
  align-items: center;
  min-height: 88px;
  padding: var(--space-4);
  font-family: inherit;
  text-align: left;
  background: var(--bg-tertiary);
  border: 1px solid var(--separator);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.palette-card:hover {
  border-color: color-mix(in srgb, var(--system-blue) 45%, var(--separator));
}

.palette-card:active {
  transform: scale(0.99);
}

.palette-card--active {
  border-color: var(--system-blue);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--system-blue) 16%, transparent);
}

.palette-card__swatches {
  display: inline-flex;
  overflow: hidden;
  width: 54px;
  height: 36px;
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
}

.palette-card__swatch {
  flex: 1;
}

.palette-card__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.palette-card__title {
  font-size: var(--text-subhead);
  font-weight: 700;
  color: var(--label-primary);
}

.palette-card__desc {
  font-size: var(--text-caption1);
  line-height: 1.45;
  color: var(--label-secondary);
}

.palette-card__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--system-blue);
}

.startup-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-4);
  background: var(--bg-tertiary);
  border: 1px solid var(--separator);
  border-radius: var(--radius-lg);
}

.startup-card__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: var(--space-2);
}

.startup-card__eyebrow {
  font-size: var(--text-caption1);
  font-weight: 700;
  color: var(--system-blue);
}

.startup-card__body strong {
  font-size: var(--text-subhead);
  color: var(--label-primary);
}

.startup-card__body p {
  margin: 0;
  font-size: var(--text-footnote);
  line-height: 1.45;
  color: var(--label-secondary);
}

.startup-card__url {
  overflow: hidden;
  max-width: 100%;
  padding: 7px 10px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
  background: var(--bg-elevated);
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
}

.startup-option__text code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: var(--text-caption1);
  color: var(--label-primary);
}

.startup-card__options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}

.startup-option {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  padding: var(--space-3);
  background: var(--bg-elevated);
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
}

.startup-option__label {
  font-size: var(--text-footnote);
  font-weight: 700;
  color: var(--label-primary);
}

.startup-option__text {
  font-size: var(--text-caption1);
  line-height: 1.45;
  color: var(--label-secondary);
}

.startup-card__actions {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.startup-card__actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  min-height: 36px;
  padding: 0 var(--space-3);
  font-family: inherit;
  font-size: var(--text-footnote);
  font-weight: 650;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: opacity var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.startup-card__actions button:active {
  transform: scale(0.98);
}

.startup-card__primary {
  color: #ffffff;
  background: var(--system-blue);
  border: 1px solid var(--system-blue);
}

.startup-card__secondary {
  color: var(--system-blue);
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--system-blue) 45%, var(--separator));
}

@media (max-width: 640px) {
  .palette-list {
    grid-template-columns: 1fr;
  }

  .startup-card {
    grid-template-columns: 1fr;
  }

  .startup-card__options {
    grid-template-columns: 1fr;
  }

  .startup-card__actions {
    flex-wrap: wrap;
  }
}
</style>
