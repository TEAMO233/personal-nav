<script setup lang="ts">
/**
 * 首页精选入口网格:展示现有 Shortcut 的 featured 视图,点击记录最近访问。
 */
import { computed } from 'vue'
import { useShortcutStore } from '@/stores/shortcut'
import { useRecentVisitStore } from '@/stores/recentVisit'
import AppIcon, { type IconName } from '@/components/AppIcon.vue'
import type { Shortcut } from '@/api/types'

const shortcutStore = useShortcutStore()
const recentVisitStore = useRecentVisitStore()

const featured = computed(() => shortcutStore.featuredShortcuts.slice(0, 8))

/**
 * 点击快捷入口。
 *
 * @param shortcut 快捷方式
 */
function openShortcut(shortcut: Shortcut): void {
  // 1. 先记录最近访问,不阻塞跳转
  recentVisitStore.record({ shortcutId: shortcut.id })
  // 2. 内部链接同页跳转,外部链接新标签打开
  if (shortcut.url.startsWith('/')) {
    window.location.href = shortcut.url
  } else {
    window.open(shortcut.url, '_blank', 'noopener')
  }
}

/**
 * 图标 key 兜底。
 */
function iconName(shortcut: Shortcut): IconName {
  // 1. 后端给的 iconKey 命中则使用,否则用外链图标
  const name = shortcut.iconKey as IconName | null
  return name ?? 'external-link'
}
</script>

<template>
  <section class="shortcut-grid" aria-label="快捷入口">
    <button
      v-for="item in featured"
      :key="item.id"
      type="button"
      class="feature-card"
      :class="`feature-card--${item.accent || 'blue'}`"
      @click="openShortcut(item)"
    >
      <span class="feature-card__icon">
        <AppIcon :name="iconName(item)" :size="34" />
      </span>
      <span class="feature-card__title">{{ item.name }}</span>
      <span class="feature-card__desc">{{ item.description || item.url }}</span>
      <AppIcon name="arrow-right" :size="20" class="feature-card__arrow" />
    </button>
  </section>
</template>

<style scoped>
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 132px), 1fr));
  gap: 18px;
  width: 100%;
}

.feature-card {
  display: flex;
  position: relative;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  height: 146px;
  min-height: 146px;
  padding: 18px 24px 42px;
  overflow: hidden;
  color: var(--label-primary);
  text-align: left;
  background: var(--home-surface-bg);
  border: 1px solid var(--home-surface-border);
  border-radius: 16px;
  cursor: pointer;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18), var(--home-surface-shadow);
  transition: transform var(--duration-fast) var(--ease-default),
    background-color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
}

.feature-card:hover {
  background: var(--home-surface-bg-strong);
  border-color: var(--home-search-border);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24), var(--home-surface-shadow);
  transform: translateY(-4px);
}

.feature-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  color: var(--feature-color);
}

.feature-card__title {
  width: 100%;
  margin-bottom: 8px;
  overflow: hidden;
  font-size: 17px;
  font-weight: 700;
  color: var(--home-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feature-card__desc {
  display: -webkit-box;
  max-width: 100%;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.35;
  color: var(--home-text-tertiary);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.feature-card__arrow {
  position: absolute;
  bottom: 14px;
  left: 24px;
  color: var(--home-text-secondary);
}

.feature-card--blue { --feature-color: var(--feature-blue); }
.feature-card--green { --feature-color: #64d9ad; }
.feature-card--purple { --feature-color: #8066f4; }
.feature-card--orange { --feature-color: #ff9f3f; }
.feature-card--cyan { --feature-color: #3fd6d5; }
.feature-card--violet { --feature-color: #7c5cff; }
.feature-card--yellow { --feature-color: #ffc857; }

@media (max-width: 900px) {
  .shortcut-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 156px), 1fr));
    gap: 14px;
  }

  .feature-card {
    height: 140px;
    min-height: 140px;
    padding: 14px 18px 36px;
  }

  .feature-card__icon {
    width: 40px;
    height: 40px;
    margin-bottom: 8px;
  }

  .feature-card__title {
    margin-bottom: 6px;
  }

  .feature-card__arrow {
    bottom: 12px;
  }
}

@media (max-width: 560px) {
  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .feature-card {
    height: 112px;
    min-height: 112px;
    padding: 12px 12px 30px;
    border-radius: 14px;
  }

  .feature-card__icon {
    width: 34px;
    height: 34px;
    margin-bottom: 6px;
  }

  .feature-card__title {
    margin-bottom: 4px;
    font-size: 15px;
  }

  .feature-card__desc {
    font-size: 12px;
    line-height: 1.3;
  }

  .feature-card__arrow {
    bottom: 8px;
    left: 12px;
  }
}

@media (max-width: 360px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
