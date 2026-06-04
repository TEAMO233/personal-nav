/**
 * 快捷方式状态:分组与快捷方式列表,按分组分桶供展示,并提供增删改、排序、跨组移动。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as groupApi from '@/api/group'
import * as shortcutApi from '@/api/shortcut'
import type { Group, GroupInput, Shortcut, ShortcutInput, ShortcutOrderItem } from '@/api/types'

/** 一个分组及其组内快捷方式 */
export interface GroupWithShortcuts {
  group: Group
  shortcuts: Shortcut[]
}

export const useShortcutStore = defineStore('shortcut', () => {
  const groups = ref<Group[]>([])
  const shortcuts = ref<Shortcut[]>([])
  const featuredShortcuts = ref<Shortcut[]>([])
  const loaded = ref(false)

  // 按分组分桶:分组按 sortOrder,组内也按 sortOrder(用副本排序,不动原数组)
  const grouped = computed<GroupWithShortcuts[]>(() => {
    const sortedGroups = [...groups.value].sort((a, b) => a.sortOrder - b.sortOrder)
    return sortedGroups.map((g) => ({
      group: g,
      shortcuts: shortcuts.value
        .filter((s) => s.groupId === g.id)
        .sort((a, b) => a.sortOrder - b.sortOrder),
    }))
  })

  /**
   * 并发加载分组与快捷方式。
   */
  async function load(): Promise<void> {
    // 1. 分组、全部快捷方式与首页精选一起拉
    const [g, s, f] = await Promise.all([
      groupApi.listGroups(),
      shortcutApi.listShortcuts(),
      shortcutApi.listFeaturedShortcuts(),
    ])
    groups.value = g
    shortcuts.value = s
    featuredShortcuts.value = f
    loaded.value = true
  }

  // ===== 分组增删改 =====

  /**
   * 新建分组,追加到末尾。
   *
   * @param input 分组名称
   * @returns 新分组
   */
  async function createGroup(input: GroupInput): Promise<Group> {
    // 1. 建好后追加
    const g = await groupApi.createGroup(input)
    groups.value.push(g)
    return g
  }

  /**
   * 更新分组并替换本地项。
   *
   * @param id    分组 id
   * @param input 新名称
   */
  async function updateGroup(id: string, input: GroupInput): Promise<void> {
    // 1. 更新后替换本地项
    const g = await groupApi.updateGroup(id, input)
    const i = groups.value.findIndex((x) => x.id === id)
    if (i >= 0) groups.value[i] = g
  }

  /**
   * 删除分组(后端级联删组内快捷方式),本地同步移除分组与其下快捷方式。
   *
   * @param id 分组 id
   */
  async function removeGroup(id: string): Promise<void> {
    // 1. 删除后本地同步清掉该组及其快捷方式
    await groupApi.deleteGroup(id)
    groups.value = groups.value.filter((g) => g.id !== id)
    shortcuts.value = shortcuts.value.filter((s) => s.groupId !== id)
  }

  /**
   * 重排分组,用后端返回列表覆盖。
   *
   * @param orderedIds 有序分组 id 列表
   */
  async function reorderGroups(orderedIds: string[]): Promise<void> {
    // 1. 提交并用返回的有序列表覆盖
    groups.value = await groupApi.reorderGroups(orderedIds)
  }

  // ===== 快捷方式增删改 =====

  /**
   * 新建快捷方式,追加到本地列表。
   *
   * @param input 分组、名称、URL、可选图标
   * @returns 新快捷方式
   */
  async function createShortcut(input: ShortcutInput): Promise<Shortcut> {
    // 1. 建好后追加
    const s = await shortcutApi.createShortcut(input)
    shortcuts.value.push(s)
    if (s.featured) {
      featuredShortcuts.value = [...featuredShortcuts.value, s].sort(
        (a, b) => a.featuredOrder - b.featuredOrder || a.sortOrder - b.sortOrder,
      )
    }
    return s
  }

  /**
   * 更新快捷方式并替换本地项。
   *
   * @param id    快捷方式 id
   * @param input 更新内容
   */
  async function updateShortcut(id: string, input: ShortcutInput): Promise<void> {
    // 1. 更新后替换本地项
    const s = await shortcutApi.updateShortcut(id, input)
    const i = shortcuts.value.findIndex((x) => x.id === id)
    if (i >= 0) shortcuts.value[i] = s
    // 2. 同步首页精选列表
    featuredShortcuts.value = s.featured
      ? [...featuredShortcuts.value.filter((x) => x.id !== id), s].sort(
          (a, b) => a.featuredOrder - b.featuredOrder || a.sortOrder - b.sortOrder,
        )
      : featuredShortcuts.value.filter((x) => x.id !== id)
  }

  /**
   * 删除快捷方式,本地移除。
   *
   * @param id 快捷方式 id
   */
  async function removeShortcut(id: string): Promise<void> {
    // 1. 删除后本地移除
    await shortcutApi.deleteShortcut(id)
    shortcuts.value = shortcuts.value.filter((s) => s.id !== id)
    featuredShortcuts.value = featuredShortcuts.value.filter((s) => s.id !== id)
  }

  /**
   * 重排 + 跨组移动,用后端返回列表覆盖。
   *
   * @param items 全部快捷方式的新位置
   */
  async function reorderShortcuts(items: ShortcutOrderItem[]): Promise<void> {
    // 1. 提交全量新位置并用返回列表覆盖
    shortcuts.value = await shortcutApi.reorderShortcuts(items)
    featuredShortcuts.value = featuredShortcuts.value
      .map((s) => shortcuts.value.find((x) => x.id === s.id) ?? s)
      .sort((a, b) => a.featuredOrder - b.featuredOrder || a.sortOrder - b.sortOrder)
  }

  /**
   * 清空(切换用户/登出时用)。
   */
  function reset(): void {
    groups.value = []
    shortcuts.value = []
    featuredShortcuts.value = []
    loaded.value = false
  }

  return {
    groups,
    shortcuts,
    featuredShortcuts,
    loaded,
    grouped,
    load,
    createGroup,
    updateGroup,
    removeGroup,
    reorderGroups,
    createShortcut,
    updateShortcut,
    removeShortcut,
    reorderShortcuts,
    reset,
  }
})
