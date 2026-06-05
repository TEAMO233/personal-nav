<script setup lang="ts">
/**
 * 引擎图标:内置图标优先,其次自定义媒体图标,都没有时用名称首字母占位。
 */
import { computed } from 'vue'
import type { Engine } from '@/api/types'
import { mediaUrl } from '@/api/media'
import googleIcon from '@/assets/engine-icons/google.ico'
import baiduIcon from '@/assets/engine-icons/baidu.svg'
import bingIcon from '@/assets/engine-icons/bing.svg'
import duckduckgoIcon from '@/assets/engine-icons/duckduckgo.svg'

const props = withDefaults(defineProps<{ engine: Engine; size?: number }>(), { size: 22 })

// 内置图标 key → 打包后的图标地址
const builtinIconMap: Record<string, string> = {
  google: googleIcon,
  baidu: baiduIcon,
  bing: bingIcon,
  duckduckgo: duckduckgoIcon,
}

// 选用的图标地址:内置优先,其次自定义媒体,都没有为 null
const iconSrc = computed<string | null>(() => {
  const { iconBuiltin, iconAssetId } = props.engine
  if (iconBuiltin && builtinIconMap[iconBuiltin]) return builtinIconMap[iconBuiltin]
  if (iconAssetId) return mediaUrl(iconAssetId)
  return null
})

// 无图标时的占位首字母
const initial = computed(() => props.engine.name.trim().charAt(0).toUpperCase() || '?')
</script>

<template>
  <!-- 有图标:直接显示 -->
  <img
    v-if="iconSrc"
    class="engine-icon"
    :src="iconSrc"
    :alt="engine.name"
    :style="{ width: size + 'px', height: size + 'px' }"
  />
  <!-- 无图标:首字母占位 -->
  <span
    v-else
    class="engine-icon engine-icon--text"
    :style="{ width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.5) + 'px' }"
    aria-hidden="true"
    >{{ initial }}</span
  >
</template>

<style scoped>
.engine-icon {
  display: inline-block;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.engine-icon--text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #ffffff;
  background: var(--system-gray);
}
</style>
