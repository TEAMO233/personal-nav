/**
 * 引擎状态:当前用户的引擎列表、默认引擎,以及增删改、排序、设默认。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as engineApi from '@/api/engine'
import type { Engine, EngineInput } from '@/api/types'

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
    engines.value = await engineApi.listEngines()
    loaded.value = true
  }

  /**
   * 新建引擎,追加到列表末尾(不影响其他引擎)。
   *
   * @param input 名称、URL 模板、可选图标
   * @returns 新引擎
   */
  async function create(input: EngineInput): Promise<Engine> {
    // 1. 建好后直接追加
    const e = await engineApi.createEngine(input)
    engines.value.push(e)
    return e
  }

  /**
   * 更新引擎并替换本地对应项。
   *
   * @param id    引擎 id
   * @param input 更新内容
   */
  async function update(id: string, input: EngineInput): Promise<void> {
    // 1. 更新后替换本地项
    const e = await engineApi.updateEngine(id, input)
    const i = engines.value.findIndex((x) => x.id === id)
    if (i >= 0) engines.value[i] = e
  }

  /**
   * 删除引擎;删默认会触发后端补设新默认,故重新加载保证一致。
   *
   * @param id 引擎 id
   */
  async function remove(id: string): Promise<void> {
    // 1. 删除后重新加载(同步可能变化的默认标记)
    await engineApi.deleteEngine(id)
    await load()
  }

  /**
   * 按给定顺序重排,用后端返回的有序列表覆盖本地。
   *
   * @param orderedIds 有序引擎 id 列表
   */
  async function reorder(orderedIds: string[]): Promise<void> {
    // 1. 提交并用返回的有序列表覆盖
    engines.value = await engineApi.reorderEngines(orderedIds)
  }

  /**
   * 设某引擎为默认,本地同步只让其为默认。
   *
   * @param id 引擎 id
   */
  async function setDefault(id: string): Promise<void> {
    // 1. 设默认后本地把其余取消默认
    await engineApi.setDefaultEngine(id)
    engines.value.forEach((e) => {
      e.isDefault = e.id === id
    })
  }

  /**
   * 清空(切换用户/登出时用)。
   */
  function reset(): void {
    engines.value = []
    loaded.value = false
  }

  return { engines, loaded, defaultEngine, load, create, update, remove, reorder, setDefault, reset }
})
