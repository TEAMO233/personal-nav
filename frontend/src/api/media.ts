/**
 * 媒体(自定义图标)相关辅助。
 * 图标通过 <img src> 直接走后端接口读取,请求会自动带上同源 Cookie 完成归属校验。
 */

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
