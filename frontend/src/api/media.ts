/**
 * 媒体(自定义图标)接口:上传 / 从图片外链保存 / 抓站点 favicon,以及拼读取地址。
 * 图标读取通过 <img src> 直接走后端接口,请求自动带同源 Cookie 完成归属校验。
 */
import http from './http'
import type { MediaAsset } from './types'

/**
 * 拼出某个媒体资源的读取地址。
 *
 * @param assetId 媒体 id
 * @returns 可直接用于 img src 的 URL
 */
export function mediaUrl(assetId: string): string {
  // 1. 统一 /api/media/{id} 前缀(开发期经 Vite proxy 转发到后端)
  return `/api/media/${assetId}`
}

/**
 * 上传本地图片作图标。
 *
 * @param file 图片文件
 * @returns 媒体信息
 */
export function uploadImage(file: File): Promise<MediaAsset> {
  // 1. 包成 multipart 表单,字段名与后端一致(file)
  const form = new FormData()
  form.append('file', file)
  // 2. 提交上传(axios 传 FormData 时自动设 multipart 头)
  return http.post<MediaAsset>('/media/upload', form).then((r) => r.data)
}

/**
 * 从图片外链下载并保存作图标。
 *
 * @param url 图片地址
 * @returns 媒体信息
 */
export function saveImageFromUrl(url: string): Promise<MediaAsset> {
  // 1. 后端下载外链图片并落库
  return http.post<MediaAsset>('/media/from-url', { url }).then((r) => r.data)
}

/**
 * 抓取站点 favicon 并保存作图标。
 *
 * @param url 站点地址
 * @returns 媒体信息
 */
export function fetchFavicon(url: string): Promise<MediaAsset> {
  // 1. 后端抓取站点图标并落库
  return http.post<MediaAsset>('/media/fetch-favicon', { url }).then((r) => r.data)
}
