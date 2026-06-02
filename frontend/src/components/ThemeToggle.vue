<script setup lang="ts">
/**
 * 主题切换按钮:亮色时显示月亮(点击转暗),暗色时显示太阳(点击转亮)。
 */
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import AppIcon from './AppIcon.vue'

const theme = useThemeStore()

// 暗色显示太阳(可转亮),亮色显示月亮(可转暗)
const iconName = computed(() => (theme.mode === 'dark' ? 'sun' : 'moon'))
const label = computed(() => (theme.mode === 'dark' ? '切换到亮色' : '切换到暗色'))
</script>

<template>
  <!-- 圆形图标按钮,满足 44pt 可点区域 -->
  <button class="theme-toggle" type="button" :aria-label="label" :title="label" @click="theme.toggle()">
    <AppIcon :name="iconName" :size="20" />
  </button>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: var(--label-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.theme-toggle:hover {
  color: var(--label-primary);
  background: var(--bg-secondary);
}

.theme-toggle:active {
  transform: scale(0.94);
}
</style>
