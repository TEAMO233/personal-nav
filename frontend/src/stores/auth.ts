/**
 * 认证状态:当前用户、登录态探测、登录/登出。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  // 当前登录用户,未登录为 null
  const user = ref<UserInfo | null>(null)
  // 是否已完成首次登录态探测(避免重复探测)
  const initialized = ref(false)

  const isLoggedIn = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  /**
   * 应用启动时探测登录态,顺带拿到后端下发的 CSRF cookie。
   */
  async function init(): Promise<void> {
    // 1. 只探测一次
    if (initialized.value) return
    // 2. 拉当前用户;探测失败(未登录/网络异常)都视为未登录,不阻断进入登录页
    try {
      user.value = await authApi.fetchMe()
    } catch {
      user.value = null
    }
    initialized.value = true
  }

  /**
   * 登录成功后保存用户信息。
   *
   * @param username 用户名
   * @param password 密码
   */
  async function login(username: string, password: string): Promise<void> {
    // 1. 调登录接口并记录用户
    user.value = await authApi.login(username, password)
    initialized.value = true
  }

  /**
   * 登出并清空登录态(无论后端是否成功都清本地)。
   */
  async function logout(): Promise<void> {
    // 1. 通知后端销毁会话
    try {
      await authApi.logout()
    } finally {
      // 2. 本地清空
      user.value = null
    }
  }

  /**
   * 仅清空本地登录态(供 401 拦截器调用)。
   */
  function clear(): void {
    user.value = null
  }

  return { user, initialized, isLoggedIn, isAdmin, init, login, logout, clear }
})
