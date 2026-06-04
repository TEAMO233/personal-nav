<script setup lang="ts">
/**
 * 未登录移动首页:只保留移动友好的公开搜索框。
 */
import SearchBar from '@/components/SearchBar.vue'
</script>

<template>
  <main class="guest-mobile" aria-label="公开搜索首页">
    <RouterLink class="guest-mobile__login" :to="{ name: 'login', query: { redirect: '/' } }">登录</RouterLink>
    <h1 class="guest-mobile__sr-only">个人导航公开搜索</h1>
    <section class="guest-mobile__search" aria-label="搜索">
      <SearchBar />
    </section>
  </main>
</template>

<style scoped>
.guest-mobile {
  display: grid;
  box-sizing: border-box;
  min-height: 100svh;
  padding: max(20px, env(safe-area-inset-top)) 16px max(24px, env(safe-area-inset-bottom));
  background: var(--guest-search-page-bg);
  place-items: center;
}

.guest-mobile__search {
  width: 100%;
  transform: translateY(-8vh);
}

.guest-mobile__login {
  position: fixed;
  top: max(14px, env(safe-area-inset-top));
  right: 14px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  min-height: 44px;
  padding: 0 14px;
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

.guest-mobile__login:hover {
  border-color: var(--guest-search-border-focus);
  box-shadow: var(--guest-search-shadow-focus);
}

.guest-mobile__login:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--system-blue) 34%, transparent);
  outline-offset: 3px;
}

.guest-mobile__login:active {
  transform: scale(0.97);
}

.guest-mobile__search :deep(.search-bar) {
  height: 52px;
  max-width: 100%;
  padding: 0 6px 0 12px;
  background: var(--guest-search-surface);
  border: 1px solid var(--guest-search-border);
  box-shadow: var(--guest-search-shadow);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.guest-mobile__search :deep(.search-bar:focus-within) {
  border-color: var(--guest-search-border-focus);
  box-shadow: var(--guest-search-shadow-focus);
}

.guest-mobile__search :deep(.engine-trigger),
.guest-mobile__search :deep(.search-go) {
  width: 44px;
  height: 44px;
}

.guest-mobile__search :deep(.engine-trigger),
.guest-mobile__search :deep(.search-input) {
  color: var(--guest-search-text);
}

.guest-mobile__search :deep(.search-input) {
  font-size: 16px;
  font-weight: 450;
}

.guest-mobile__search :deep(.search-input::placeholder) {
  color: var(--guest-search-muted);
}

.guest-mobile__search :deep(.search-go) {
  color: var(--guest-search-surface);
  background: var(--guest-search-action);
  box-shadow: var(--guest-search-action-shadow);
}

.guest-mobile__search :deep(.engine-menu) {
  min-width: min(220px, calc(100vw - 32px));
  background: color-mix(in srgb, var(--guest-search-surface) 96%, transparent);
  border-color: var(--guest-search-border);
}

.guest-mobile__search :deep(.engine-option) {
  min-height: 44px;
  color: var(--guest-search-text);
}

.guest-mobile__sr-only {
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
