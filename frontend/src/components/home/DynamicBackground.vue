<script setup lang="ts">
/**
 * 首页动态背景:深蓝渐变底色 + macOS 风弧形流线光效。
 */
</script>

<template>
  <div class="dyn-bg" aria-hidden="true">
    <span class="dyn-bg__arc dyn-bg__arc--1"></span>
    <span class="dyn-bg__arc dyn-bg__arc--2"></span>
    <span class="dyn-bg__glow dyn-bg__glow--1"></span>
    <span class="dyn-bg__glow dyn-bg__glow--2"></span>
    <span class="dyn-bg__glow dyn-bg__glow--3"></span>
  </div>
</template>

<style scoped>
.dyn-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(circle at 50% 0%, rgba(70, 130, 255, 0.35), transparent 38%),
    radial-gradient(circle at 16% 26%, rgba(56, 189, 248, 0.18), transparent 30%),
    linear-gradient(135deg, #020617 0%, #061b3a 48%, #020617 100%);
}

.dyn-bg__arc {
  position: absolute;
  left: 50%;
  width: 1900px;
  height: 520px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  border-color: rgba(96, 165, 250, 0.24) transparent transparent transparent;
  border-radius: 50%;
  filter: blur(3px);
  transform: translateX(-50%) rotate(-5deg);
}

.dyn-bg__arc--1 {
  top: 185px;
  box-shadow: 0 -22px 75px rgba(59, 130, 246, 0.16);
}

.dyn-bg__arc--2 {
  top: 250px;
  width: 2100px;
  border-color: rgba(125, 180, 255, 0.16) transparent transparent transparent;
  transform: translateX(-50%) rotate(4deg);
}

.dyn-bg__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  will-change: transform;
}

.dyn-bg__glow--1 {
  top: -120px;
  left: 33%;
  width: 430px;
  height: 430px;
  background: rgba(56, 130, 255, 0.42);
  animation: dyn-float-1 26s var(--ease-default) infinite alternate;
}

.dyn-bg__glow--2 {
  top: 30px;
  right: 2%;
  width: 360px;
  height: 360px;
  background: rgba(56, 189, 248, 0.22);
  animation: dyn-float-2 32s var(--ease-default) infinite alternate;
}

.dyn-bg__glow--3 {
  bottom: -220px;
  left: 18%;
  width: 560px;
  height: 560px;
  background: rgba(37, 99, 235, 0.18);
  animation: dyn-float-3 30s var(--ease-default) infinite alternate;
}

@keyframes dyn-float-1 {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(70px, 36px) scale(1.08); }
}

@keyframes dyn-float-2 {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(-50px, 58px) scale(1.05); }
}

@keyframes dyn-float-3 {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(42px, -52px) scale(1.12); }
}

@media (prefers-reduced-motion: reduce) {
  .dyn-bg__glow {
    animation: none;
  }
}
</style>
