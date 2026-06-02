/**
 * 管理后台接口(仅 ADMIN 可访问,鉴权由后端 /api/admin/** 规则保证):开户、重置密码、签发邀请码。
 */
import http from './http'
import type { CreateUserInput, InviteCode, UserInfo } from './types'

/**
 * 管理员开户。
 *
 * @param input 用户名、初始密码、可选角色
 * @returns 新建用户信息
 */
export function createUser(input: CreateUserInput): Promise<UserInfo> {
  // 1. 提交开户
  return http.post<UserInfo>('/admin/users', input).then((r) => r.data)
}

/**
 * 重置指定用户的密码。
 *
 * @param id          用户 id
 * @param newPassword 新密码
 */
export function resetPassword(id: string, newPassword: string): Promise<void> {
  // 1. 提交新密码,后端返回 204
  return http.post(`/admin/users/${id}/reset-password`, { newPassword }).then(() => undefined)
}

/**
 * 签发邀请码。
 *
 * @param expiresInDays 有效天数,留空用后端默认
 * @returns 邀请码与过期时间
 */
export function createInviteCode(expiresInDays?: number): Promise<InviteCode> {
  // 1. 提交签发请求(天数可空)
  return http.post<InviteCode>('/admin/invite-codes', { expiresInDays }).then((r) => r.data)
}
