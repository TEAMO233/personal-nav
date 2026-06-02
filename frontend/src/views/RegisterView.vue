<script setup lang="ts">
/**
 * 注册页:邀请码 + 用户名 + 密码自助注册。注册成功后不自动登录,引导去登录页。
 * 卡片视觉走 style.css 的 .auth-* 共用类(与登录页共享)。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import * as authApi from '@/api/auth'
import { ApiClientError } from '@/api/http'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()

const code = ref('')
const username = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)

/**
 * 提交注册:基础校验 → 调注册 → 成功提示并跳登录页。
 */
async function onSubmit(): Promise<void> {
  // 1. 基础非空校验
  if (!code.value || !username.value || !password.value) {
    errorMsg.value = '请填写邀请码、用户名和密码'
    return
  }
  // 2. 密码长度校验(对齐后端 @Size(min=8))
  if (password.value.length < 8) {
    errorMsg.value = '密码至少 8 位'
    return
  }
  // 3. 进入提交态
  errorMsg.value = ''
  loading.value = true
  try {
    // 4. 注册(后端不自动登录),成功后引导去登录
    await authApi.register(code.value.trim(), username.value.trim(), password.value)
    ElMessage.success('注册成功,请登录')
    await router.replace({ name: 'login' })
  } catch (e) {
    // 5. 展示后端文案(邀请码无效/过期、用户名占用、限流等)
    errorMsg.value = e instanceof ApiClientError ? e.message : '注册失败,请稍后重试'
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

    <!-- 居中注册卡片 -->
    <main class="auth-main">
      <form class="auth-card" novalidate @submit.prevent="onSubmit">
        <!-- 标题区 -->
        <header class="auth-header">
          <h1 class="auth-title">注册账号</h1>
          <p class="auth-subtitle">用邀请码创建账号</p>
        </header>

        <!-- 输入区 -->
        <div class="auth-fields">
          <label class="auth-field">
            <span class="auth-field-label">邀请码</span>
            <input
              v-model.trim="code"
              class="auth-field-input"
              type="text"
              autocomplete="off"
              placeholder="请输入邀请码"
              :disabled="loading"
            />
          </label>
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
              name="new-password"
              autocomplete="new-password"
              placeholder="至少 8 位"
              :disabled="loading"
            />
          </label>
        </div>

        <!-- 错误提示 -->
        <p v-if="errorMsg" class="auth-error" role="alert">{{ errorMsg }}</p>

        <!-- 提交按钮 -->
        <button class="auth-submit" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注册' }}
        </button>

        <!-- 登录入口 -->
        <p class="auth-alt">已有账号?<router-link to="/login">去登录</router-link></p>
      </form>
    </main>
  </div>
</template>
