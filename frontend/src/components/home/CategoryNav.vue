<script setup lang="ts">
/**
 * 分类导航(玻璃面板):列出真实分组胶囊,点击平滑滚动到对应分组锚点。
 */
import { useShortcutStore } from '@/stores/shortcut'

const shortcutStore = useShortcutStore()

/**
 * 平滑滚动到指定分组锚点。
 *
 * @param groupId 分组 id
 */
function scrollToGroup(groupId: string): void {
  // 1. 找到分组容器并平滑滚动到顶部(scroll-margin 已为吸顶栏留白)
  document
    .getElementById(`group-${groupId}`)
    ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <!-- 有分组才渲染 -->
  <section v-if="shortcutStore.grouped.length" class="glass-panel category-nav">
    <h2 class="category-nav__title">常用分类</h2>
    <div class="category-nav__chips">
      <button
        v-for="g in shortcutStore.grouped"
        :key="g.group.id"
        type="button"
        class="category-chip"
        @click="scrollToGroup(g.group.id)"
      >
        <span class="category-chip__name">{{ g.group.name }}</span>
        <span class="category-chip__count">{{ g.shortcuts.length }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.category-nav {
  padding: var(--space-5);
}

.category-nav__title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--label-primary);
}

.category-nav__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-footnote);
  color: var(--label-primary);
  background: color-mix(in srgb, var(--label-primary) 6%, transparent);
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default);
}

.category-chip:hover {
  border-color: color-mix(in srgb, var(--system-blue) 40%, transparent);
  background: color-mix(in srgb, var(--system-blue) 12%, transparent);
}

.category-chip__count {
  min-width: 20px;
  padding: 0 6px;
  font-size: var(--text-caption2);
  font-weight: 600;
  color: var(--label-secondary);
  text-align: center;
  background: color-mix(in srgb, var(--label-primary) 10%, transparent);
  border-radius: var(--radius-full);
}
</style>
