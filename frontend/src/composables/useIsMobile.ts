/**
 * 响应式判断是否窄屏(移动端):用于把拖拽排序降级为上移/下移按钮(对应 prd D10)。
 */
import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 监听媒体查询,返回是否命中(窄屏)的响应式标记。
 *
 * @param query 媒体查询条件,默认窄屏阈值 640px
 * @returns isMobile 响应式布尔
 */
export function useIsMobile(query = '(max-width: 640px)') {
  const isMobile = ref(false)
  let mq: MediaQueryList | undefined

  /**
   * 同步当前是否命中。
   */
  function update(): void {
    // 1. 取媒体查询当前匹配状态
    isMobile.value = mq?.matches ?? false
  }

  // 挂载时建立监听,卸载时移除
  onMounted(() => {
    mq = window.matchMedia(query)
    update()
    mq.addEventListener('change', update)
  })
  onUnmounted(() => {
    mq?.removeEventListener('change', update)
  })

  return { isMobile }
}
