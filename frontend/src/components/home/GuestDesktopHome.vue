<script setup lang="ts">
/**
 * 未登录桌面首页:Google 式极简搜索入口,不展示登录/注册/营销内容。
 */
import SearchBar from '@/components/SearchBar.vue'
</script>

<template>
  <main class="guest-desktop" aria-label="公开搜索首页">
    <RouterLink class="guest-desktop__login" :to="{ name: 'login', query: { redirect: '/' } }">登录</RouterLink>
    <h1 class="guest-desktop__sr-only">个人导航公开搜索</h1>
    <section class="guest-desktop__search" aria-label="搜索">
      <SearchBar />
    </section>
  </main>
</template>

<style scoped>
.guest-desktop {
  display: grid;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 0 clamp(32px, 8vw, 128px);
  background: var(--guest-search-page-bg);
  color: var(--guest-search-text);
  place-items: center;
}

.guest-desktop__search {
  width: min(100%, 640px);
  transform: translateY(-6vh);
}

.guest-desktop__login {
  position: fixed;
  top: 28px;
  right: 36px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  min-height: 44px;
  padding: 0 16px;
  font-size: var(--text-subhead);
  font-weight: 650;
  color: var(--guest-search-text);
  text-decoration: none;
  background: color-mix(in srgb, var(--guest-search-surface) 76%, transparent);
  border: 1px solid var(--guest-search-border);
  border-radius: var(--radius-full);
  box-shadow: var(--guest-search-shadow);
  transition: border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.guest-desktop__login:hover {
  border-color: var(--guest-search-border-focus);
  box-shadow: var(--guest-search-shadow-focus);
}

.guest-desktop__login:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--system-blue) 34%, transparent);
  outline-offset: 3px;
}

.guest-desktop__login:active {
  transform: scale(0.97);
}

.guest-desktop__search :deep(.search-bar) {
  height: 54px;
  max-width: 640px;
  padding: 0 6px 0 18px;
  background: var(--guest-search-surface);
  border: 1px solid var(--guest-search-border);
  box-shadow: var(--guest-search-shadow);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.guest-desktop__search :deep(.search-bar:focus-within) {
  border-color: var(--guest-search-border-focus);
  box-shadow: var(--guest-search-shadow-focus);
}

.guest-desktop__search :deep(.engine-trigger),
.guest-desktop__search :deep(.search-input) {
  color: var(--guest-search-text);
}

.guest-desktop__search :deep(.search-input) {
  font-weight: 450;
}

.guest-desktop__search :deep(.search-input::placeholder) {
  color: var(--guest-search-muted);
}

.guest-desktop__search :deep(.search-kbd) {
  color: var(--guest-search-muted);
  background: var(--guest-search-chip);
}

.guest-desktop__search :deep(.search-go) {
  color: var(--guest-search-surface);
  background: var(--guest-search-action);
  box-shadow: var(--guest-search-action-shadow);
}

.guest-desktop__search :deep(.engine-menu) {
  background: color-mix(in srgb, var(--guest-search-surface) 96%, transparent);
  border-color: var(--guest-search-border);
  box-shadow: var(--guest-search-shadow-focus);
}

.guest-desktop__search :deep(.engine-option) {
  color: var(--guest-search-text);
}

.guest-desktop__search :deep(.engine-option:hover) {
  background: var(--guest-search-chip);
}

.guest-desktop__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

</style>
