<script setup lang="ts">
/**
 * 登录页:HIG 风格居中卡片 + 用户名/密码表单;底部提供邀请码注册入口。
 * 卡片视觉走 style.css 的 .auth-* 共用类(与注册页共享)。
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
  <div class="auth-page">
    <!-- 右上角:主题切换 -->
    <div class="auth-toolbar">
      <ThemeToggle />
    </div>

    <!-- 居中登录卡片 -->
    <main class="auth-main">
      <form class="auth-card" novalidate @submit.prevent="onSubmit">
        <!-- 标题区 -->
        <header class="auth-header">
          <h1 class="auth-title">个人导航</h1>
          <p class="auth-subtitle">登录以继续</p>
        </header>

        <!-- 输入区 -->
        <div class="auth-fields">
          <label class="auth-field">
            <span class="auth-field-label">用户名</span>
            <input
              v-model.trim="username"
              class="auth-field-input"
              type="text"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              :disabled="loading"
            />
          </label>
          <label class="auth-field">
            <span class="auth-field-label">密码</span>
            <input
              v-model="password"
              class="auth-field-input"
              type="password"
              name="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              :disabled="loading"
            />
          </label>
        </div>

        <!-- 错误提示 -->
        <p v-if="errorMsg" class="auth-error" role="alert">{{ errorMsg }}</p>

        <!-- 提交按钮 -->
        <button class="auth-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>

        <!-- 注册入口 -->
        <p class="auth-alt">有邀请码?<router-link to="/register">去注册</router-link></p>
      </form>
    </main>
  </div>
</template>
