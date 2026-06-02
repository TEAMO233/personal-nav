<script setup lang="ts">
/**
 * 登录页:HIG 风格居中卡片 + 用户名/密码表单。仅登录,邀请码注册留待 M7。
 */
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiClientError } from '@/api/http'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)

/**
 * 提交登录:校验非空 → 调登录 → 成功回跳原地址(无则首页)。
 */
async function onSubmit(): Promise<void> {
  // 1. 基础非空校验
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  // 2. 进入提交态
  errorMsg.value = ''
  loading.value = true
  try {
    // 3. 登录并回跳(登录前被拦下来的地址,否则首页)
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    // 4. 直接展示后端文案(如"用户名或密码错误"/限流提示)
    errorMsg.value = e instanceof ApiClientError ? e.message : '登录失败,请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 右上角:主题切换 -->
    <div class="login-toolbar">
      <ThemeToggle />
    </div>

    <!-- 居中登录卡片 -->
    <main class="login-main">
      <form class="login-card" novalidate @submit.prevent="onSubmit">
        <!-- 标题区 -->
        <header class="login-header">
          <h1 class="login-title">个人导航</h1>
          <p class="login-subtitle">登录以继续</p>
        </header>

        <!-- 输入区 -->
        <div class="login-fields">
          <label class="field">
            <span class="field-label">用户名</span>
            <input
              v-model.trim="username"
              class="field-input"
              type="text"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              :disabled="loading"
            />
          </label>
          <label class="field">
            <span class="field-label">密码</span>
            <input
              v-model="password"
              class="field-input"
              type="password"
              name="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              :disabled="loading"
            />
          </label>
        </div>

        <!-- 错误提示 -->
        <p v-if="errorMsg" class="login-error" role="alert">{{ errorMsg }}</p>

        <!-- 提交按钮 -->
        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-secondary);
}

.login-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-4);
}

.login-main {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  /* 视觉重心略偏上 */
  padding: var(--space-4) var(--space-4) 12vh;
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  box-sizing: border-box;
  width: 100%;
  max-width: 360px;
  padding: var(--space-8);
  background: var(--bg-elevated);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-elevated);
}

.login-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  text-align: center;
}

.login-title {
  font-size: var(--text-title1);
  font-weight: 700;
  letter-spacing: -0.5px;
}

.login-subtitle {
  font-size: var(--text-subhead);
  color: var(--label-secondary);
}

.login-fields {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.field-label {
  padding-left: var(--space-1);
  font-size: var(--text-footnote);
  color: var(--label-secondary);
}

.field-input {
  box-sizing: border-box;
  width: 100%;
  height: 44px;
  padding: 0 var(--space-4);
  font-family: inherit;
  font-size: var(--text-body);
  color: var(--label-primary);
  background: var(--bg-secondary);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
}

.field-input::placeholder {
  color: var(--label-tertiary);
}

.field-input:focus {
  border-color: var(--system-blue);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--system-blue) 25%, transparent);
}

.login-error {
  margin: 0;
  font-size: var(--text-subhead);
  color: var(--system-red);
  text-align: center;
}

.login-submit {
  height: 50px;
  font-size: var(--text-body);
  font-weight: 600;
  color: #ffffff;
  background: var(--system-blue);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: opacity var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.login-submit:hover:not(:disabled) {
  opacity: 0.9;
}

.login-submit:active:not(:disabled) {
  transform: scale(0.98);
}

.login-submit:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
