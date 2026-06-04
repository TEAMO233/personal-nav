<script setup lang="ts">
/**
 * 今日概览面板:用真实待办数据计算今日完成率、待办数、日程数和系统状态。
 */
import { computed } from 'vue'
import { useTodoStore } from '@/stores/todo'
import AppIcon from '@/components/AppIcon.vue'

const todoStore = useTodoStore()

const ringStyle = computed(() => ({
  background: `conic-gradient(#2f78ff 0 ${todoStore.todayCompletionRate}%, rgba(255,255,255,0.1) ${todoStore.todayCompletionRate}% 100%)`,
}))
</script>

<template>
  <section class="glass-panel overview panel">
    <header class="panel__head">
      <h2><AppIcon name="chart" :size="18" />今日概览</h2>
    </header>

    <div class="overview__main">
      <div class="ring" :style="ringStyle">
        <div class="ring__inner">{{ todoStore.todayCompletionRate }}%</div>
      </div>
      <div class="overview__text">
        <strong>今日完成率</strong>
        <span>持续推进，效率真棒！</span>
      </div>
    </div>

    <div class="overview__stats">
      <div class="mini-stat">
        <AppIcon name="calendar" :size="18" />
        <span>待办事项</span>
        <strong>{{ todoStore.pendingCount }}</strong>
      </div>
      <div class="mini-stat">
        <AppIcon name="calendar" :size="18" />
        <span>日程安排</span>
        <strong>{{ todoStore.scheduleCount }}</strong>
      </div>
      <div class="mini-stat mini-stat--ok">
        <AppIcon name="shield" :size="18" />
        <span>系统状态</span>
        <strong>正常</strong>
      </div>
    </div>

    <blockquote class="overview__quote">
      <span>专注和坚持是通往卓越的唯一路径。</span>
      <small>开启新的一天 ✦</small>
    </blockquote>
  </section>
</template>

<style scoped>
.panel {
  min-height: 226px;
  padding: 16px 20px;
}

.panel__head {
  margin-bottom: 16px;
}

.panel__head h2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  color: rgba(255, 255, 255, 0.94);
}

.panel__head svg {
  color: #5f9cff;
}

.overview__main {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-bottom: 16px;
}

.ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92px;
  height: 92px;
  border-radius: var(--radius-full);
  box-shadow: 0 12px 35px rgba(47, 120, 255, 0.26);
}

.ring__inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  font-size: 23px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  background: rgba(10, 25, 55, 0.9);
  border-radius: var(--radius-full);
}

.overview__text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview__text strong {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.95);
}

.overview__text span {
  font-size: 14px;
  color: rgba(220, 230, 255, 0.62);
}

.overview__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.075);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}

.mini-stat svg {
  color: #3b82f6;
}

.mini-stat span {
  font-size: 12px;
  color: rgba(220, 230, 255, 0.58);
}

.mini-stat strong {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.92);
}

.mini-stat--ok strong,
.mini-stat--ok svg {
  color: #34d399;
}

.overview__quote {
  margin: 14px 0 0;
  padding: 14px 20px;
  color: rgba(220, 230, 255, 0.7);
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}

.overview__quote span,
.overview__quote small {
  display: block;
}

.overview__quote small {
  margin-top: 8px;
  color: rgba(220, 230, 255, 0.5);
}
</style>
