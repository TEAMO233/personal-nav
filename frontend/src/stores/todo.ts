/**
 * 待办 / 日程状态:首页内联展示、添加、更新完成状态与删除。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as todoApi from '@/api/todo'
import type { TodoInput, TodoItem } from '@/api/types'

export const useTodoStore = defineStore('todo', () => {
  const items = ref<TodoItem[]>([])
  const loaded = ref(false)

  // 今天相关待办:计划时间在今天,或无计划但创建于今天
  const todayItems = computed(() => {
    const today = new Date()
    return items.value.filter((item) => isSameDay(item.scheduledAt ?? item.createdAt, today))
  })

  // 今日完成率:今天相关待办中已完成数量 / 总数
  const todayCompletionRate = computed(() => {
    if (!todayItems.value.length) return 0
    const doneCount = todayItems.value.filter((item) => item.done).length
    return Math.round((doneCount / todayItems.value.length) * 100)
  })

  // 未完成待办数
  const pendingCount = computed(() => items.value.filter((item) => !item.done).length)

  // 有计划时间且未完成的日程数
  const scheduleCount = computed(() => items.value.filter((item) => !item.done && item.scheduledAt).length)

  /**
   * 加载待办列表。
   */
  async function load(): Promise<void> {
    // 1. 首页需要全部待办做完成率和统计;后端默认返回全部
    items.value = await todoApi.listTodos()
    loaded.value = true
  }

  /**
   * 新建待办。
   *
   * @param input 待办内容
   */
  async function create(input: TodoInput): Promise<void> {
    // 1. 建好后重新加载,保证排序与统计一致
    await todoApi.createTodo(input)
    await load()
  }

  /**
   * 更新待办。
   *
   * @param id    待办 id
   * @param input 更新内容
   */
  async function update(id: string, input: TodoInput): Promise<void> {
    // 1. 更新后替换本地项
    const updated = await todoApi.updateTodo(id, input)
    const i = items.value.findIndex((item) => item.id === id)
    if (i >= 0) items.value[i] = updated
  }

  /**
   * 切换完成状态。
   *
   * @param item 待办项
   */
  async function toggleDone(item: TodoItem): Promise<void> {
    // 1. 保留原字段,只切换 done
    await update(item.id, {
      title: item.title,
      tag: item.tag,
      scheduledAt: item.scheduledAt,
      done: !item.done,
    })
  }

  /**
   * 删除待办。
   *
   * @param id 待办 id
   */
  async function remove(id: string): Promise<void> {
    // 1. 删除后本地同步移除
    await todoApi.deleteTodo(id)
    items.value = items.value.filter((item) => item.id !== id)
  }

  /**
   * 清空本地状态。
   */
  function reset(): void {
    items.value = []
    loaded.value = false
  }

  return {
    items,
    loaded,
    todayItems,
    todayCompletionRate,
    pendingCount,
    scheduleCount,
    load,
    create,
    update,
    toggleDone,
    remove,
    reset,
  }
})

/**
 * 判断 ISO 时间是否与指定日期同天。
 */
function isSameDay(value: string | null, date: Date): boolean {
  // 1. 空值不是今天
  if (!value) return false
  // 2. 按浏览器本地时区比较年月日
  const d = new Date(value)
  return d.getFullYear() === date.getFullYear()
    && d.getMonth() === date.getMonth()
    && d.getDate() === date.getDate()
}
