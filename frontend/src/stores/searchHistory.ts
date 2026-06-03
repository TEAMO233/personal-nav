/**
 * 搜索历史状态:当前用户最近的搜索关键词,以及记录、删除、清空。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as searchHistoryApi from '@/api/searchHistory'
import type { SearchHistoryItem } from '@/api/types'

export const useSearchHistoryStore = defineStore('searchHistory', () => {
  const items = ref<SearchHistoryItem[]>([])
  const loaded = ref(false)

  /**
   * 加载本人搜索历史。
   */
  async function load(): Promise<void> {
    // 1. 拉取并标记已加载
    items.value = await searchHistoryApi.listSearchHistory()
    loaded.value = true
  }

  /**
   * 记录一次搜索;用后端返回的最新列表覆盖本地(已去重置顶)。
   * 失败静默,不打断用户的搜索跳转。
   *
   * @param keyword 搜索关键词
   */
  async function record(keyword: string): Promise<void> {
    // 1. 记录并用返回列表覆盖;失败忽略
    try {
      items.value = await searchHistoryApi.recordSearch(keyword)
    } catch {
      // 记录历史失败不影响用户搜索,静默处理
    }
  }

  /**
   * 删除一条历史,并从本地移除。
   *
   * @param id 记录 id
   */
  async function remove(id: string): Promise<void> {
    // 1. 删除后本地同步移除
    await searchHistoryApi.removeSearch(id)
    items.value = items.value.filter((it) => it.id !== id)
  }

  /**
   * 清空全部历史。
   */
  async function clear(): Promise<void> {
    // 1. 清空后本地置空
    await searchHistoryApi.clearSearchHistory()
    items.value = []
  }

  /**
   * 清空本地状态(切换用户/登出时用)。
   */
  function reset(): void {
    items.value = []
    loaded.value = false
  }

  return { items, loaded, load, record, remove, clear, reset }
})
