<script setup lang="ts">
/**
 * 首页动态背景:主题渐变底色 + 现代细网格与低饱和光束。
 */
</script>

<template>
  <div class="dyn-bg" aria-hidden="true">
    <span class="dyn-bg__grid"></span>
    <span class="dyn-bg__arc dyn-bg__arc--1"></span>
    <span class="dyn-bg__arc dyn-bg__arc--2"></span>
    <span class="dyn-bg__beam dyn-bg__beam--1"></span>
    <span class="dyn-bg__beam dyn-bg__beam--2"></span>
    <span class="dyn-bg__beam dyn-bg__beam--3"></span>
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

.dyn-bg__arc {
  position: absolute;
  width: 58%;
  height: 1px;
  border: none;
  border-radius: var(--radius-full);
  opacity: 0.78;
  background: linear-gradient(90deg, transparent, var(--home-line-highlight), transparent);
}

.dyn-bg__arc--1 {
  top: 218px;
  left: 14%;
  box-shadow: 0 0 44px var(--home-arc-shadow);
  transform: rotate(-7deg);
}

.dyn-bg__arc--2 {
  top: 286px;
  right: 10%;
  width: 42%;
  opacity: 0.55;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--system-blue) 20%, transparent), transparent);
  transform: rotate(9deg);
}

.dyn-bg__beam {
  position: absolute;
  height: 1px;
  border-radius: var(--radius-full);
  filter: blur(0.4px);
  opacity: 0.9;
  will-change: transform;
}

.dyn-bg__beam--1 {
  top: 118px;
  left: 22%;
  width: 56%;
  background: var(--home-glow-1);
  animation: dyn-drift-1 18s var(--ease-default) infinite alternate;
}

.dyn-bg__beam--2 {
  top: 335px;
  right: 6%;
  width: 34%;
  background: var(--home-glow-2);
  animation: dyn-drift-2 24s var(--ease-default) infinite alternate;
}

.dyn-bg__beam--3 {
  bottom: 22%;
  left: 8%;
  width: 42%;
  background: var(--home-glow-3);
  animation: dyn-drift-3 22s var(--ease-default) infinite alternate;
}

@keyframes dyn-drift-1 {
  from { transform: translateX(-22px); }
  to { transform: translateX(42px); }
}

@keyframes dyn-drift-2 {
  from { transform: translateX(36px); }
  to { transform: translateX(-46px); }
}

@keyframes dyn-drift-3 {
  from { transform: translateX(-28px); }
  to { transform: translateX(34px); }
}

@media (prefers-reduced-motion: reduce) {
  .dyn-bg__beam {
    animation: none;
  }
}
</style>
