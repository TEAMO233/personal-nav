/**
 * 最近访问状态:展示用户点击过的导航资源,点击记录失败不打断跳转。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as recentVisitApi from '@/api/recentVisit'
import type { RecentVisit, RecordVisitInput } from '@/api/types'

export const useRecentVisitStore = defineStore('recentVisit', () => {
  const items = ref<RecentVisit[]>([])
  const loaded = ref(false)

  /**
   * 加载最近访问。
   */
  async function load(): Promise<void> {
    // 1. 首页展示 6 条
    items.value = await recentVisitApi.listRecentVisits(6)
    loaded.value = true
  }

  /**
   * 记录一次访问;失败静默,避免阻塞用户跳转。
   *
   * @param input 访问目标
   */
  async function record(input: RecordVisitInput): Promise<void> {
    // 1. 记录访问,成功后置顶到本地
    try {
      const visit = await recentVisitApi.recordVisit(input)
      items.value = [visit, ...items.value.filter((item) => item.id !== visit.id)].slice(0, 6)
    } catch {
      // 记录访问失败不影响导航跳转
    }
  }

  /**
   * 清空本地状态。
   */
  function reset(): void {
    items.value = []
    loaded.value = false
  }

  return { items, loaded, load, record, reset }
})
