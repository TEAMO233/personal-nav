/**
 * 引擎状态:当前用户的引擎列表与默认引擎。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listEngines } from '@/api/engine'
import type { Engine } from '@/api/types'

export const useEngineStore = defineStore('engine', () => {
  const engines = ref<Engine[]>([])
  const loaded = ref(false)

  // 默认引擎:取标记为默认者,兜底取第一个
  const defaultEngine = computed<Engine | null>(
    () => engines.value.find((e) => e.isDefault) ?? engines.value[0] ?? null,
  )

  /**
   * 加载本人引擎列表。
   */
  async function load(): Promise<void> {
    // 1. 拉取并标记已加载
    engines.value = await listEngines()
    loaded.value = true
  }

  /**
   * 清空(切换用户/登出时用)。
   */
  function reset(): void {
    engines.value = []
    loaded.value = false
  }

  return { engines, loaded, defaultEngine, load, reset }
})
