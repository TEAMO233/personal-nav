/**
 * 灵感便签状态:首页展示置顶 / 最新便签列表,支持内联新增、编辑与删除。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as noteApi from '@/api/note'
import type { NoteInput, NoteItem } from '@/api/types'

const HOME_NOTE_LIMIT = 6

export const useNoteStore = defineStore('note', () => {
  const items = ref<NoteItem[]>([])
  const loaded = ref(false)

  // 首页首条便签:保留给旧调用兼容,列表排序由后端保证
  const activeNote = computed(() => items.value[0] ?? null)

  /**
   * 加载便签。
   *
   * @param limit 首页最多返回数量
   */
  async function load(limit = HOME_NOTE_LIMIT): Promise<void> {
    // 1. 首页需要便签列表,不再只取第一条
    items.value = await noteApi.listNotes(limit)
    loaded.value = true
  }

  /**
   * 新建首页便签。
   *
   * @param input 便签内容
   * @returns 新便签
   */
  async function create(input: NoteInput): Promise<NoteItem> {
    // 1. 新建后插入列表顶部,保持本地列表可连续新增
    const note = await noteApi.createNote({ ...input, pinned: input.pinned ?? true })
    items.value = [note, ...items.value.filter((item) => item.id !== note.id)].slice(0, HOME_NOTE_LIMIT)
    return note
  }

  /**
   * 更新指定便签。
   *
   * @param id    便签 id
   * @param input 便签内容
   * @returns 更新后便签
   */
  async function update(id: string, input: NoteInput): Promise<NoteItem> {
    // 1. 更新后替换列表中的同一条记录
    const note = await noteApi.updateNote(id, input)
    items.value = items.value.map((item) => (item.id === id ? note : item))
    return note
  }

  /**
   * 更新当前首页便签。
   *
   * @param input 便签内容
   */
  async function updateActive(input: NoteInput): Promise<void> {
    // 1. 没有活动便签时转为新建
    if (!activeNote.value) {
      await create(input)
      return
    }
    // 2. 有活动便签则只更新当前便签
    await update(activeNote.value.id, input)
  }

  /**
   * 保存首页便签:保持旧调用兼容,语义为更新当前便签。
   *
   * @param input 便签内容
   */
  async function save(input: NoteInput): Promise<void> {
    // 1. 兼容旧入口
    await updateActive(input)
  }

  /**
   * 删除当前便签。
   */
  async function removeActive(): Promise<void> {
    // 1. 没有便签则无需处理
    if (!activeNote.value) return
    // 2. 复用指定删除逻辑
    await remove(activeNote.value.id)
  }

  /**
   * 删除指定便签。
   *
   * @param id 便签 id
   */
  async function remove(id: string): Promise<void> {
    // 1. 删除后只移除对应列表项
    await noteApi.deleteNote(id)
    items.value = items.value.filter((item) => item.id !== id)
    // 2. 列表可能还有后续数据,重新补足首页数量
    if (items.value.length < HOME_NOTE_LIMIT) {
      await load()
    }
  }

  /**
   * 清空本地状态。
   */
  function reset(): void {
    items.value = []
    loaded.value = false
  }

  return { items, loaded, activeNote, load, create, update, updateActive, save, remove, removeActive, reset }
})
