<script setup lang="ts">
/**
 * 快捷方式卡片(玻璃):图标 + 名称 + 域名,点击在新标签打开目标网址。
 */
import { computed } from 'vue'
import type { Shortcut } from '@/api/types'
import { mediaUrl } from '@/api/media'
import AppIcon from './AppIcon.vue'

const props = defineProps<{ shortcut: Shortcut }>()

// 自定义图标地址(无则用首字母占位)
const iconSrc = computed(() =>
  props.shortcut.iconAssetId ? mediaUrl(props.shortcut.iconAssetId) : null,
)

// 占位首字母
const initial = computed(() => props.shortcut.name.trim().charAt(0).toUpperCase() || '?')

// 从网址提取域名做副文本;解析失败就回退显示原始串
const domain = computed(() => {
  // 1. 正常网址:取主机名并去掉 www. 前缀
  try {
    return new URL(props.shortcut.url).hostname.replace(/^www\./, '')
  } catch {
    // 2. 不是合法网址就直接显示原串
    return props.shortcut.url
  }
})

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
    <!-- 名称 + 域名 -->
    <span class="shortcut-text">
      <span class="shortcut-name">{{ shortcut.name }}</span>
      <span class="shortcut-domain">{{ domain }}</span>
    </span>
    <!-- hover 时显现的外链箭头 -->
    <AppIcon name="external-link" :size="16" class="shortcut-arrow" />
  </a>
</template>

<style scoped>
.shortcut-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  color: var(--label-primary);
  text-decoration: none;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default),
    transform var(--duration-fast) var(--ease-default);
}

.shortcut-card:hover {
  background: var(--glass-highlight);
  border-color: color-mix(in srgb, var(--system-blue) 35%, transparent);
  box-shadow: var(--glass-shadow);
  transform: translateY(-3px);
}

.shortcut-card:active {
  transform: translateY(-1px) scale(0.99);
}

.shortcut-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
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
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  border-radius: var(--radius-md);
}

.shortcut-text {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.shortcut-name {
  overflow: hidden;
  font-size: var(--text-subhead);
  font-weight: 500;
  color: var(--label-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-domain {
  overflow: hidden;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-arrow {
  flex-shrink: 0;
  color: var(--label-tertiary);
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-default);
}

.shortcut-card:hover .shortcut-arrow {
  opacity: 1;
}
</style>
