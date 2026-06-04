<script setup lang="ts">
/**
 * 首页动态背景:主题渐变底色 + 现代细网格与低饱和光束。
 */
</script>

<template>
  <div class="dyn-bg" aria-hidden="true">
    <span class="dyn-bg__grid"></span>
  </div>
</template>

<style scoped>
.dyn-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--home-bg);
}

.dyn-bg::after {
  position: absolute;
  inset: 0;
  content: '';
  background:
    radial-gradient(ellipse at 50% 0%, var(--home-grid-fade), transparent 34%),
    linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.16) 100%);
}

.dyn-bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--home-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--home-grid-line) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse at 50% 18%, black 0%, transparent 64%);
  opacity: 0.7;
}

</style>
