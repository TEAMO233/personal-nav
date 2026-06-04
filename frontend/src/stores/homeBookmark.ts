/**
 * 首页书签状态:首页显示启用项,设置页管理全部项。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as homeBookmarkApi from '@/api/homeBookmark'
import type { HomeBookmark, HomeBookmarkInput } from '@/api/types'

export const useHomeBookmarkStore = defineStore('homeBookmark', () => {
  const items = ref<HomeBookmark[]>([])
  const loaded = ref(false)
  const allLoaded = ref(false)

  const enabledItems = computed(() => items.value.filter((item) => item.enabled))

  /**
   * 加载首页启用书签。
   */
  async function load(): Promise<void> {
    // 1. 只拉启用项供首页展示
    items.value = await homeBookmarkApi.listHomeBookmarks(true)
    loaded.value = true
    allLoaded.value = false
  }

  /**
   * 加载全部书签供设置页管理。
   */
  async function loadAll(): Promise<void> {
    // 1. 拉全部项并更新两个加载标记
    items.value = await homeBookmarkApi.listHomeBookmarks(false)
    loaded.value = true
    allLoaded.value = true
  }

  /**
   * 新建首页书签。
   *
   * @param input 书签内容
   * @returns 新书签
   */
  async function create(input: HomeBookmarkInput): Promise<HomeBookmark> {
    // 1. 新建后追加到本地列表
    const bookmark = await homeBookmarkApi.createHomeBookmark(input)
    items.value = [...items.value, bookmark].sort(bySortOrder)
    return bookmark
  }

  /**
   * 更新首页书签。
   *
   * @param id    书签 id
   * @param input 书签内容
   */
  async function update(id: string, input: HomeBookmarkInput): Promise<void> {
    // 1. 更新后替换本地项
    const bookmark = await homeBookmarkApi.updateHomeBookmark(id, input)
    const index = items.value.findIndex((item) => item.id === id)
    if (index >= 0) items.value[index] = bookmark
    else items.value.push(bookmark)
    items.value = [...items.value].sort(bySortOrder)
  }

  /**
   * 删除首页书签。
   *
   * @param id 书签 id
   */
  async function remove(id: string): Promise<void> {
    // 1. 删除后本地移除
    await homeBookmarkApi.deleteHomeBookmark(id)
    items.value = items.value.filter((item) => item.id !== id)
  }

  /**
   * 重排首页书签。
   *
   * @param orderedIds 有序 id 列表
   */
  async function reorder(orderedIds: string[]): Promise<void> {
    // 1. 后端返回新顺序,直接覆盖
    items.value = await homeBookmarkApi.reorderHomeBookmarks(orderedIds)
  }

  /**
   * 清空本地状态。
   */
  function reset(): void {
    items.value = []
    loaded.value = false
    allLoaded.value = false
  }

  /**
   * 按排序值排序。
   */
  function bySortOrder(a: HomeBookmark, b: HomeBookmark): number {
    return a.sortOrder - b.sortOrder
  }

  return { items, enabledItems, loaded, allLoaded, load, loadAll, create, update, remove, reorder, reset }
})
