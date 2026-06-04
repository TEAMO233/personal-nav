<script setup lang="ts">
/**
 * 首页底部 dashboard widget 总编排。
 */
import HomeBookmarkPanel from './HomeBookmarkPanel.vue'
import CategoryPanel from './CategoryPanel.vue'
import OverviewPanel from './OverviewPanel.vue'
import TodoSchedulePanel from './TodoSchedulePanel.vue'
import NotePanel from './NotePanel.vue'
</script>

<template>
  <section class="dashboard-grid">
    <HomeBookmarkPanel class="dashboard-grid__bookmarks" />
    <CategoryPanel class="dashboard-grid__category" />
    <OverviewPanel class="dashboard-grid__overview" />
    <div class="dashboard-grid__right">
      <TodoSchedulePanel />
      <NotePanel />
    </div>
  </section>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.85fr 0.9fr 1.05fr;
  gap: 18px;
  align-items: stretch;
  width: 100%;
  min-width: 0;
  min-height: 0;
}

.dashboard-grid__right {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  min-height: 0;
}

.dashboard-grid :deep(.panel) {
  min-width: 0;
  height: 100%;
}

.dashboard-grid__right :deep(.todo) {
  flex: 1 1 58%;
}

.dashboard-grid__right :deep(.note) {
  flex: 1 1 42%;
}

@media (max-width: 1199px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid :deep(.panel) {
    height: auto;
  }
}

@media (max-width: 760px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
