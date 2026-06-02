/**
 * 快捷方式状态:分组与快捷方式列表,并按分组分桶供首页展示。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listGroups } from '@/api/group'
import { listShortcuts } from '@/api/shortcut'
import type { Group, Shortcut } from '@/api/types'

/** 一个分组及其组内快捷方式 */
export interface GroupWithShortcuts {
  group: Group
  shortcuts: Shortcut[]
}

export const useShortcutStore = defineStore('shortcut', () => {
  const groups = ref<Group[]>([])
  const shortcuts = ref<Shortcut[]>([])
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
    // 1. 两个列表一起拉
    const [g, s] = await Promise.all([listGroups(), listShortcuts()])
    groups.value = g
    shortcuts.value = s
    loaded.value = true
  }

  /**
   * 清空(切换用户/登出时用)。
   */
  function reset(): void {
    groups.value = []
    shortcuts.value = []
    loaded.value = false
  }

  return { groups, shortcuts, loaded, grouped, load, reset }
})
