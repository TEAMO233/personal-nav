<script setup lang="ts">
/**
 * 最近访问面板:展示用户真实点击过的导航资源。
 */
import { useRecentVisitStore } from '@/stores/recentVisit'
import AppIcon from '@/components/AppIcon.vue'

const recentVisitStore = useRecentVisitStore()

/**
 * 格式化相对时间。
 *
 * @param value ISO 时间
 */
function relativeTime(value: string): string {
  // 1. 计算分钟差
  const diff = Date.now() - new Date(value).getTime()
  const minutes = Math.max(1, Math.round(diff / 60000))
  // 2. 按常用粒度展示
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.round(hours / 24)} 天前`
}
</script>

<template>
  <section class="glass-panel recent panel">
    <header class="panel__head">
      <h2><AppIcon name="clock" :size="18" />最近访问</h2>
    </header>

    <div v-if="recentVisitStore.items.length" class="recent__list">
      <a
        v-for="item in recentVisitStore.items"
        :key="item.id"
        class="recent__item"
        :href="item.url"
        target="_blank"
        rel="noopener noreferrer"
      >
        <span class="recent__favicon">{{ item.name.charAt(0).toUpperCase() }}</span>
        <span class="recent__text">
          <strong>{{ item.name }}</strong>
          <small>{{ item.domain }}</small>
        </span>
        <span class="recent__time">{{ relativeTime(item.visitedAt) }}</span>
      </a>
    </div>
    <p v-else class="panel__empty">点击快捷入口后会出现在这里</p>
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
  margin-bottom: 12px;
}

.panel__head h2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  color: var(--home-text-primary);
}

.panel__head svg {
  color: var(--system-blue);
}

.recent__list {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.recent__item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 42px;
  color: inherit;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
}

.recent__item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.recent__favicon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 13px;
  font-weight: 800;
  color: #ffffff;
  background: var(--avatar-grad);
  border-radius: 8px;
}

.recent__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.recent__text strong,
.recent__text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent__text strong {
  font-size: 14px;
  color: var(--home-text-primary);
}

.recent__text small,
.recent__time {
  font-size: 12px;
  color: var(--home-text-tertiary);
}

.panel__empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  margin: 0;
  font-size: 14px;
  color: var(--home-text-tertiary);
  text-align: center;
}
</style>
