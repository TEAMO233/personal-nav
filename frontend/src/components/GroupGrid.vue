<script setup lang="ts">
/**
 * 快捷方式网格:按分组分桶展示,响应式多列;含空分组与整体空状态提示。
 */
import { useShortcutStore } from '@/stores/shortcut'
import ShortcutCard from './ShortcutCard.vue'

const shortcutStore = useShortcutStore()
</script>

<template>
  <section class="group-grid">
    <!-- 有分组:逐组渲染标题 + 卡片网格 -->
    <template v-if="shortcutStore.grouped.length">
      <div v-for="g in shortcutStore.grouped" :key="g.group.id" class="group">
        <h2 class="group__title">{{ g.group.name }}</h2>
        <div v-if="g.shortcuts.length" class="group__items">
          <ShortcutCard v-for="s in g.shortcuts" :key="s.id" :shortcut="s" />
        </div>
        <!-- 空分组提示 -->
        <p v-else class="group__empty">暂无快捷方式</p>
      </div>
    </template>

    <!-- 无任何分组:整体空状态 -->
    <div v-else class="grid-empty">
      <p class="grid-empty__title">还没有快捷方式</p>
      <p class="grid-empty__hint">在设置中添加分组与快捷方式(M7 上线)</p>
    </div>
  </section>
</template>

<style scoped>
.group-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
  width: 100%;
}

.group__title {
  margin: 0 0 var(--space-4);
  padding-left: var(--space-1);
  font-size: var(--text-title3);
  font-weight: 600;
  color: var(--label-primary);
}

.group__items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: var(--space-3);
}

.group__empty {
  margin: 0;
  padding: var(--space-2) var(--space-1);
  font-size: var(--text-subhead);
  color: var(--label-tertiary);
}

.grid-empty {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-12) 0;
  text-align: center;
}

.grid-empty__title {
  font-size: var(--text-title3);
  color: var(--label-primary);
}

.grid-empty__hint {
  font-size: var(--text-subhead);
  color: var(--label-tertiary);
}

/* 移动端:卡片更密 */
@media (max-width: 640px) {
  .group__items {
    grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
    gap: var(--space-2);
  }
}
</style>
