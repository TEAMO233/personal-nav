<script setup lang="ts">
/**
 * 常用分类面板:展示真实分组和组内快捷方式数量。
 */
import { useShortcutStore } from '@/stores/shortcut'
import AppIcon, { type IconName } from '@/components/AppIcon.vue'

const shortcutStore = useShortcutStore()
const icons: IconName[] = ['code', 'chart', 'server', 'star', 'globe', 'document', 'folder']
</script>

<template>
  <section class="glass-panel category panel">
    <header class="panel__head">
      <h2><AppIcon name="grid" :size="18" />常用分类</h2>
      <AppIcon name="more" :size="18" />
    </header>

    <div v-if="shortcutStore.grouped.length" class="category__list">
      <div v-for="(g, index) in shortcutStore.grouped" :key="g.group.id" class="category__row">
        <AppIcon :name="icons[index % icons.length]" :size="18" class="category__icon" />
        <span>{{ g.group.name }}</span>
        <b>{{ g.shortcuts.length }}</b>
      </div>
    </div>
    <p v-else class="panel__empty">暂无分类</p>
  </section>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  min-height: 226px;
  padding: 16px 20px;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: rgba(220, 230, 255, 0.72);
}

.panel__head h2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  color: rgba(255, 255, 255, 0.94);
}

.category__list {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.category__row {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  min-height: 36px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.category__icon {
  color: #3bb6ff;
}

.category__row span {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.86);
}

.category__row b {
  min-width: 34px;
  padding: 3px 10px;
  font-size: 13px;
  color: rgba(220, 230, 255, 0.8);
  text-align: center;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-full);
}

.panel__empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  margin: 0;
  font-size: 14px;
  color: rgba(220, 230, 255, 0.54);
  text-align: center;
}
</style>
