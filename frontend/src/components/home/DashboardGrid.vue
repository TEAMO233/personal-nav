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
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
  gap: 18px;
  align-items: start;
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
  height: auto;
}

.dashboard-grid__right :deep(.todo) {
  flex: 1 1 58%;
}

.dashboard-grid__right :deep(.note) {
  flex: 1 1 42%;
}

@media (max-width: 1199px) {
  .dashboard-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  }

  .dashboard-grid :deep(.panel) {
    height: auto;
  }
}

@media (max-width: 760px) {
  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
  }
}
</style>
