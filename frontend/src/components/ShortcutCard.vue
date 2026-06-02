<script setup lang="ts">
/**
 * 快捷方式卡片:图标 + 名称,点击在新标签打开目标网址。
 */
import { computed } from 'vue'
import type { Shortcut } from '@/api/types'
import { mediaUrl } from '@/api/media'

const props = defineProps<{ shortcut: Shortcut }>()

// 自定义图标地址(无则用首字母占位)
const iconSrc = computed(() =>
  props.shortcut.iconAssetId ? mediaUrl(props.shortcut.iconAssetId) : null,
)

// 占位首字母
const initial = computed(() => props.shortcut.name.trim().charAt(0).toUpperCase() || '?')

// 无图标时按名称取一个稳定的 HIG 系统色做底色(类似联系人头像)
const palette = [
  '#ff3b30',
  '#ff9500',
  '#ffcc00',
  '#34c759',
  '#5ac8fa',
  '#007aff',
  '#5856d6',
  '#af52de',
  '#ff2d55',
]
const monoColor = computed(() => {
  // 1. 简单字符串 hash,保证同名稳定取同色
  let h = 0
  for (const ch of props.shortcut.name) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  // 2. 落到调色板
  return palette[h % palette.length] ?? '#8e8e93'
})
</script>

<template>
  <a
    class="shortcut-card"
    :href="shortcut.url"
    target="_blank"
    rel="noopener noreferrer"
    :title="shortcut.name"
  >
    <!-- 图标 -->
    <span class="shortcut-icon">
      <img v-if="iconSrc" class="shortcut-icon__img" :src="iconSrc" :alt="shortcut.name" />
      <span v-else class="shortcut-icon__mono" :style="{ background: monoColor }" aria-hidden="true">{{
        initial
      }}</span>
    </span>
    <!-- 名称 -->
    <span class="shortcut-name">{{ shortcut.name }}</span>
  </a>
</template>

<style scoped>
.shortcut-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-2);
  color: var(--label-primary);
  text-decoration: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default),
    transform var(--duration-fast) var(--ease-default);
}

.shortcut-card:hover {
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.shortcut-card:active {
  transform: translateY(0) scale(0.98);
}

.shortcut-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
}

.shortcut-icon__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: var(--bg-elevated);
  border-radius: var(--radius-md);
}

.shortcut-icon__mono {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  border-radius: var(--radius-md);
}

.shortcut-name {
  max-width: 100%;
  overflow: hidden;
  font-size: var(--text-footnote);
  color: var(--label-secondary);
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--duration-fast) var(--ease-default);
}

.shortcut-card:hover .shortcut-name {
  color: var(--label-primary);
}
</style>
