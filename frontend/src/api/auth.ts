/**
 * 认证相关接口:登录、登出、获取当前用户。
 */
import http, { ApiClientError } from './http'
import type { UserInfo } from './types'

/**
 * 用户名密码登录。
 *
 * @param username 用户名
 * @param password 密码
 * @returns 当前用户信息
 */
export function login(username: string, password: string): Promise<UserInfo> {
  // 1. 登录失败的 401 由登录页自行提示,跳过全局跳转拦截
  return http
    .post<UserInfo>('/auth/login', { username, password }, { skipAuthInterceptor: true })
    .then((r) => r.data)
}

/**
 * 登出,清除服务端会话。
 */
export function logout(): Promise<void> {
  // 1. 后端成功返回 204
  return http.post('/auth/logout').then(() => undefined)
}

/**
 * 探测当前登录态。
 *
 * @returns 已登录返回用户信息,未登录(401)返回 null
 */
export function fetchMe(): Promise<UserInfo | null> {
  // 1. 探测请求:401 属正常"未登录"分支,跳过全局跳转并转为 null
  return http
    .get<UserInfo>('/auth/me', { skipAuthInterceptor: true })
    .then((r) => r.data)
    .catch((e: unknown) => {
      // 2. 仅 401 视为未登录;其他错误继续抛出
      if (e instanceof ApiClientError && e.status === 401) return null
      throw e
    })
}
