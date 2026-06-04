<script setup lang="ts">
/**
 * 资源概览(玻璃面板):分组数 / 网站数 / 引擎数的真实统计。
 */
import { computed } from 'vue'
import { useShortcutStore } from '@/stores/shortcut'
import { useEngineStore } from '@/stores/engine'

const shortcutStore = useShortcutStore()
const engineStore = useEngineStore()

// 真实计数:分组 / 网站(快捷方式) / 引擎
const stats = computed(() => [
  { label: '分组', value: shortcutStore.groups.length },
  { label: '网站', value: shortcutStore.shortcuts.length },
  { label: '引擎', value: engineStore.engines.length },
])
</script>

<template>
  <section class="glass-panel stats-panel">
    <h2 class="stats-panel__title">资源概览</h2>
    <div class="stats-panel__grid">
      <div v-for="s in stats" :key="s.label" class="stat">
        <div class="stat__value">{{ s.value }}</div>
        <div class="stat__label">{{ s.label }}</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.stats-panel {
  padding: var(--space-5);
}

.stats-panel__title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--label-primary);
}

.stats-panel__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  align-items: center;
  padding: var(--space-3) var(--space-2);
  background: color-mix(in srgb, var(--label-primary) 5%, transparent);
  border-radius: var(--radius-lg);
}

.stat__value {
  font-size: var(--text-title1);
  font-weight: 700;
  color: var(--label-primary);
}

.stat__label {
  font-size: var(--text-caption1);
  color: var(--label-secondary);
}
</style>
