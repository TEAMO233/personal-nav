<script setup lang="ts">
/**
 * 外观设置区:配置亮暗模式与主页配色主题。
 */
import { PALETTE_OPTIONS, useThemeStore, type ThemeMode, type ThemePalette } from '@/stores/theme'
import AppIcon from '@/components/AppIcon.vue'

const theme = useThemeStore()

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

@media (max-width: 640px) {
  .palette-list {
    grid-template-columns: 1fr;
  }
}
</style>
