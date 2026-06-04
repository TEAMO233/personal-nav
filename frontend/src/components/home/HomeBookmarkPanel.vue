<script setup lang="ts">
/**
 * 首页书签面板:展示用户在 /settings 配置的启用书签。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { mediaUrl } from '@/api/media'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import { useRecentVisitStore } from '@/stores/recentVisit'
import type { HomeBookmark } from '@/api/types'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const bookmarkStore = useHomeBookmarkStore()
const recentVisitStore = useRecentVisitStore()

const bookmarks = computed(() => bookmarkStore.enabledItems.slice(0, 6))

/**
 * 取 URL 域名。
 *
 * @param url 目标 URL
 * @returns 域名或原 URL
 */
function domainOf(url: string): string {
  // 1. 尝试解析 URL
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

/**
 * 取书签首字母。
 *
 * @param name 名称
 * @returns 首字符
 */
function initialOf(name: string): string {
  // 1. 空名兜底为星标
  return name.trim().charAt(0).toUpperCase() || '★'
}

/**
 * 点击书签时记录访问。
 *
 * @param bookmark 目标书签
 */
function recordVisit(bookmark: HomeBookmark): void {
  // 1. 访问记录失败不影响跳转
  void recentVisitStore.record({ name: bookmark.name, url: bookmark.url })
}

/**
 * 前往设置页的首页书签标签。
 */
function goSettings(): void {
  // 1. 跳转到设置页并带上 tab 参数
  router.push({ name: 'settings', query: { tab: 'homeBookmarks' } })
}
</script>

<template>
  <section class="glass-panel bookmark panel">
    <header class="panel__head">
      <h2><AppIcon name="star" :size="18" />首页书签</h2>
      <button type="button" class="panel__action" aria-label="管理首页书签" @click="goSettings">
        <AppIcon name="settings" :size="15" />
      </button>
    </header>

    <div v-if="bookmarks.length" class="bookmark__list">
      <a
        v-for="item in bookmarks"
        :key="item.id"
        class="bookmark__item"
        :href="item.url"
        target="_blank"
        rel="noopener noreferrer"
        @click="recordVisit(item)"
      >
        <img v-if="item.iconAssetId" :src="mediaUrl(item.iconAssetId)" :alt="item.name" class="bookmark__icon" />
        <span v-else class="bookmark__icon bookmark__icon--text">{{ initialOf(item.name) }}</span>
        <span class="bookmark__text">
          <strong>{{ item.name }}</strong>
          <small>{{ item.description || domainOf(item.url) }}</small>
        </span>
        <AppIcon name="external-link" :size="15" class="bookmark__open" />
      </a>
    </div>

    <div v-else class="panel__empty">
      <p>还没有首页书签</p>
      <button type="button" class="empty-action" @click="goSettings">去设置</button>
    </div>
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

.panel__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: rgba(220, 230, 255, 0.72);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.panel__action:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

.bookmark__list {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.bookmark__item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 20px;
  gap: 12px;
  align-items: center;
  min-height: 42px;
  color: inherit;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
}

.bookmark__item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.bookmark__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  object-fit: cover;
  font-size: 13px;
  font-weight: 800;
  color: #ffffff;
  background: linear-gradient(135deg, #111827, #3b82f6);
  border-radius: 8px;
}

.bookmark__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bookmark__text strong,
.bookmark__text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bookmark__text strong {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}

.bookmark__text small {
  font-size: 12px;
  color: rgba(220, 230, 255, 0.55);
}

.bookmark__open {
  color: rgba(220, 230, 255, 0.48);
}

.panel__empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  margin: 0;
  font-size: 14px;
  color: rgba(220, 230, 255, 0.54);
  text-align: center;
}

.panel__empty p {
  margin: 0;
}

.empty-action {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 650;
  color: #ffffff;
  background: rgba(74, 144, 255, 0.8);
  border: none;
  border-radius: 999px;
  cursor: pointer;
}
</style>
