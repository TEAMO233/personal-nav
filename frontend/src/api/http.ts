/**
 * axios 实例与全局拦截器。
 * - 携带 Cookie(服务端会话)并自动附带 CSRF 双提交头
 * - 业务请求遇 401(会话失效)时清登录态并跳登录页;探测/登录请求用标志跳过
 * - 把后端 {code,message} 统一规范成 ApiClientError 抛出
 */
import axios from 'axios'
import type { AxiosError } from 'axios'
import type { ApiError } from './types'

// 给请求配置加一个自定义标志:遇 401 不触发全局"跳登录"(用于登录态探测与登录接口本身)
declare module 'axios' {
  interface AxiosRequestConfig {
    skipAuthInterceptor?: boolean
  }
}

/**
 * 规范化的接口错误:组件/store 捕获后可直接读 code/message/status。
 */
export class ApiClientError extends Error {
  code: string
  status?: number

  constructor(code: string, message: string, status?: number) {
    super(message)
    this.name = 'ApiClientError'
    this.code = code
    this.status = status
  }
}

// axios 实例:统一 /api 前缀、带 Cookie、CSRF 头名与后端一致
const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  // 始终把 XSRF-TOKEN cookie 的值放进 X-XSRF-TOKEN 头(同源部署,确保 CSRF 双提交成立)
  withXSRFToken: true,
})

/**
 * 按 HTTP 状态码给一个兜底中文文案(后端未返回 message 时用)。
 *
 * @param status HTTP 状态码
 * @returns 文案
 */
function defaultMessage(status: number): string {
  // 1. 常见状态的友好提示
  if (status === 401) return '请先登录'
  if (status === 403) return '没有权限'
  if (status === 404) return '资源不存在'
  if (status === 429) return '操作过于频繁,请稍后再试'
  if (status >= 500) return '服务器开小差了,请稍后重试'
  return '请求失败'
}

/**
 * 把 AxiosError 转成统一的 ApiClientError。
 *
 * @param error axios 抛出的错误
 * @returns 规范化错误
 */
function toApiClientError(error: AxiosError<ApiError>): ApiClientError {
  // 1. 有响应:优先用后端的 code/message
  const res = error.response
  if (res) {
    const data = res.data
    const code = data?.code ?? `HTTP_${res.status}`
    const message = data?.message ?? defaultMessage(res.status)
    return new ApiClientError(code, message, res.status)
  }
  // 2. 无响应:网络层异常
  return new ApiClientError('NETWORK_ERROR', '网络异常,请检查连接后重试')
}

// 响应拦截:会话失效跳登录 + 错误规范化
http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    // 1. 业务请求遇 401(会话过期):清登录态并跳登录页
    const status = error.response?.status
    if (status === 401 && !error.config?.skipAuthInterceptor) {
      // 动态引入打破 http→router→store→api 的循环依赖
      const [{ useAuthStore }, { default: router }] = await Promise.all([
        import('@/stores/auth'),
        import('@/router'),
      ])
      useAuthStore().clear()
      // 2. 不在登录页才跳,并带上原地址便于登录后回跳
      if (router.currentRoute.value.name !== 'login') {
        await router.push({
          name: 'login',
          query: { redirect: router.currentRoute.value.fullPath },
        })
      }
    }
    // 3. 统一抛规范化错误
    return Promise.reject(toApiClientError(error))
  },
)

export default http
