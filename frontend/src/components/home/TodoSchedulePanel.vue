<script setup lang="ts">
/**
 * 待办 / 日程面板:首页内联新增、编辑、完成和删除。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useTodoStore } from '@/stores/todo'
import AppIcon from '@/components/AppIcon.vue'
import type { TodoItem } from '@/api/types'

const todoStore = useTodoStore()
const editorMode = ref<'create' | 'edit' | null>(null)
const editingId = ref<string | null>(null)
const title = ref('')
const tag = ref('')
const scheduledDate = ref('')
const scheduledTime = ref('')
const errorMessage = ref('')
const saving = ref(false)
const deletingId = ref<string | null>(null)
const now = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null

const visibleItems = computed(() => {
  // 1. 首页面板优先展示超时未完成项,其余保持后端原有排序
  return todoStore.items
    .map((item, index) => ({ item, index }))
    .sort((a, b) => {
      const aOverdue = isOverdue(a.item)
      const bOverdue = isOverdue(b.item)
      if (aOverdue !== bOverdue) return aOverdue ? -1 : 1
      if (aOverdue && bOverdue) return scheduleTime(a.item) - scheduleTime(b.item)
      return a.index - b.index
    })
    .slice(0, 3)
    .map(({ item }) => item)
})

onMounted(() => {
  // 1. 页面停留时让超时状态按分钟自动刷新
  clockTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 60_000)
})

onUnmounted(() => {
  // 1. 离开首页后清理定时器
  if (clockTimer) window.clearInterval(clockTimer)
})

/**
 * 打开新增编辑器。
 */
function startCreate(): void {
  // 1. 新增时清空临时表单
  title.value = ''
  tag.value = ''
  scheduledDate.value = ''
  scheduledTime.value = ''
  editingId.value = null
  errorMessage.value = ''
  editorMode.value = 'create'
}

/**
 * 打开编辑指定待办。
 *
 * @param item 需要编辑的待办
 */
function startEdit(item: TodoItem): void {
  // 1. 编辑时回填当前行数据
  title.value = item.title
  tag.value = item.tag ?? ''
  const localParts = parseLocalSchedule(item.scheduledAt)
  scheduledDate.value = localParts.date
  scheduledTime.value = localParts.time
  editingId.value = item.id
  errorMessage.value = ''
  editorMode.value = 'edit'
}

/**
 * 关闭编辑器。
 */
function cancelEdit(): void {
  // 1. 收起编辑器并清空临时状态
  editorMode.value = null
  editingId.value = null
  title.value = ''
  tag.value = ''
  scheduledDate.value = ''
  scheduledTime.value = ''
  errorMessage.value = ''
}

/**
 * 保存新增或编辑。
 */
async function save(): Promise<void> {
  // 1. 校验标题和日期时间成对填写
  const value = title.value.trim()
  if (!value) {
    errorMessage.value = '请填写待办内容'
    return
  }
  const scheduledAt = buildScheduledAt()
  if (scheduledAt === undefined) return

  // 2. 按模式新增或更新,保留编辑项的完成状态
  saving.value = true
  try {
    const normalizedTag = tag.value.trim() || null
    if (editorMode.value === 'create') {
      await todoStore.create({ title: value, tag: normalizedTag, scheduledAt })
    } else if (editingId.value) {
      const current = todoStore.items.find((item) => item.id === editingId.value)
      if (!current) {
        cancelEdit()
        return
      }
      await todoStore.update(editingId.value, {
        title: value,
        tag: normalizedTag,
        scheduledAt,
        done: current.done,
      })
    }
    cancelEdit()
  } finally {
    saving.value = false
  }
}

/**
 * 删除指定待办。
 *
 * @param id 待办 id
 */
async function removeTodo(id: string): Promise<void> {
  // 1. 防止重复点击同一条删除按钮
  if (deletingId.value) return
  deletingId.value = id
  try {
    // 2. 删除后同步列表,如果正在编辑该项则收起编辑器
    await todoStore.remove(id)
    if (editingId.value === id) cancelEdit()
  } finally {
    deletingId.value = null
  }
}

/**
 * 根据表单日期时间生成后端计划时间。
 */
function buildScheduledAt(): string | null | undefined {
  // 1. 日期和时间都为空表示清空排期
  const date = scheduledDate.value
  const time = scheduledTime.value
  if (!date && !time) {
    errorMessage.value = ''
    return null
  }
  // 2. 只填一项时阻止提交
  if (!date || !time) {
    errorMessage.value = '日期和时间需要一起填写'
    return undefined
  }
  // 3. 用浏览器本地时间生成 ISO 字符串
  const localDate = new Date(`${date}T${time}`)
  if (Number.isNaN(localDate.getTime())) {
    errorMessage.value = '请选择有效的日期和时间'
    return undefined
  }
  errorMessage.value = ''
  return localDate.toISOString()
}

/**
 * 将后端 ISO 时间拆成本地日期和时间输入值。
 *
 * @param value ISO 时间字符串
 */
function parseLocalSchedule(value: string | null): { date: string; time: string } {
  // 1. 无计划时间时保持输入为空
  if (!value) return { date: '', time: '' }
  // 2. 按本地时区回填 input[type=date/time] 需要的格式
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return { date: '', time: '' }
  const year = d.getFullYear()
  const month = pad2(d.getMonth() + 1)
  const day = pad2(d.getDate())
  const hour = pad2(d.getHours())
  const minute = pad2(d.getMinutes())
  return {
    date: `${year}-${month}-${day}`,
    time: `${hour}:${minute}`,
  }
}

/**
 * 补齐两位数字。
 *
 * @param value 数字
 */
function pad2(value: number): string {
  // 1. 原生 padStart 足够覆盖日期时间输入格式
  return String(value).padStart(2, '0')
}

/**
 * 格式化时间。
 *
 * @param item 待办项
 */
function displayTime(item: TodoItem): string {
  // 1. 无计划时间显示真实未排期状态
  if (!item.scheduledAt) return '未排期'
  // 2. 用本地时间格式化,避免把非今日日期误写成明天
  const d = new Date(item.scheduledAt)
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(today.getDate() + 1)
  const time = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  if (d.toDateString() === today.toDateString()) return `今天 ${time}`
  if (d.toDateString() === tomorrow.toDateString()) return `明天 ${time}`
  return `${d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })} ${time}`
}

/**
 * 判断待办是否已经超时。
 *
 * @param item 待办项
 */
function isOverdue(item: TodoItem): boolean {
  // 1. 只有未完成且有计划时间的待办才会进入超时态
  if (item.done || !item.scheduledAt) return false
  const time = scheduleTime(item)
  return Number.isFinite(time) && time < now.value
}

/**
 * 读取待办计划时间戳。
 *
 * @param item 待办项
 */
function scheduleTime(item: TodoItem): number {
  // 1. 无效时间排到普通项之后,避免 NaN 影响排序
  if (!item.scheduledAt) return Number.POSITIVE_INFINITY
  const time = new Date(item.scheduledAt).getTime()
  return Number.isNaN(time) ? Number.POSITIVE_INFINITY : time
}

/**
 * 判断待办是否有计划时间。
 *
 * @param item 待办项
 */
function isScheduled(item: TodoItem): boolean {
  // 1. 使用真实 scheduledAt 判断,不做前端推断
  return Boolean(item.scheduledAt)
}
</script>

<template>
  <section class="glass-panel todo panel">
    <header class="panel__head">
      <h2><AppIcon name="calendar" :size="18" />待办 / 日程</h2>
      <div class="panel__actions">
        <button type="button" aria-label="新增待办" title="新增待办" @click="startCreate">
          <AppIcon name="plus" :size="17" />
        </button>
      </div>
    </header>

    <form v-if="editorMode" class="todo__editor" @submit.prevent="save">
      <input v-model="title" type="text" :placeholder="editorMode === 'create' ? '输入待办或日程' : '编辑待办或日程'" />
      <div class="todo__editor-grid">
        <input v-model="tag" type="text" placeholder="标签" />
        <input v-model="scheduledDate" type="date" aria-label="计划日期" />
        <input v-model="scheduledTime" type="time" aria-label="计划时间" />
      </div>
      <p v-if="errorMessage" class="todo__error">{{ errorMessage }}</p>
      <div class="todo__editor-actions">
        <button type="button" class="todo__cancel" :disabled="saving" @click="cancelEdit">取消</button>
        <button type="submit" :disabled="saving">{{ editorMode === 'create' ? '新增' : '保存' }}</button>
      </div>
    </form>

    <div class="todo__list" :class="{ 'todo__list--empty': !todoStore.items.length }">
      <div
        v-for="item in visibleItems"
        :key="item.id"
        class="todo__row"
        :class="{ 'todo__row--done': item.done, 'todo__row--overdue': isOverdue(item) }"
      >
        <button type="button" class="todo__check" :class="{ 'todo__check--done': item.done }" @click="todoStore.toggleDone(item)">
          <AppIcon v-if="item.done" name="check" :size="14" />
        </button>
        <div class="todo__main">
          <span class="todo__title">{{ item.title }}</span>
          <div class="todo__meta">
            <span class="todo__tag">{{ item.tag || '待办' }}</span>
            <span v-if="isOverdue(item)" class="todo__overdue-badge">超时</span>
            <time class="todo__time" :class="{ 'todo__time--scheduled': isScheduled(item), 'todo__time--overdue': isOverdue(item) }">
              <AppIcon :name="isScheduled(item) ? 'clock' : 'calendar'" :size="13" />
              {{ displayTime(item) }}
            </time>
          </div>
        </div>
        <div class="todo__row-actions">
          <button type="button" aria-label="编辑待办" title="编辑待办" @click="startEdit(item)">
            <AppIcon name="edit" :size="14" />
          </button>
          <button
            type="button"
            aria-label="删除待办"
            title="删除待办"
            :disabled="deletingId === item.id"
            @click="removeTodo(item.id)"
          >
            <AppIcon name="trash" :size="14" />
          </button>
        </div>
      </div>
      <button v-if="!todoStore.items.length" type="button" class="todo__empty" @click="startCreate">创建第一条待办</button>
    </div>

    <button v-if="!editorMode && todoStore.items.length" type="button" class="todo__add" @click="startCreate">
      <AppIcon name="plus" :size="18" />
      <span>添加待办</span>
    </button>
  </section>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  min-height: 210px;
  padding: 16px 20px;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel__head h2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  color: var(--home-text-primary);
}

.panel__head svg {
  color: var(--system-blue);
}

.panel__actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.panel__actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--home-text-secondary);
  background: var(--home-control-bg);
  border: 1px solid var(--home-control-border);
  border-radius: 9px;
  cursor: pointer;
}

.panel__actions button:hover {
  color: var(--home-text-primary);
  background: var(--home-surface-hover);
}

.todo__list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.todo__list--empty {
  justify-content: center;
}

.todo__row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 50px;
  padding: 8px 10px;
  background: var(--home-surface-bg);
  border: 1px solid var(--home-surface-border-soft);
  border-radius: 12px;
}

.todo__row--done {
  opacity: 0.62;
}

.todo__row--overdue {
  background:
    linear-gradient(145deg, color-mix(in srgb, var(--system-red) 23%, transparent), var(--home-surface-bg)),
    var(--home-surface-bg);
  border-color: color-mix(in srgb, var(--system-red) 48%, transparent);
  box-shadow: 0 10px 26px color-mix(in srgb, var(--system-red) 22%, transparent);
}

.todo__row--overdue .todo__check {
  border-color: color-mix(in srgb, var(--system-red) 72%, #ffffff);
}

.todo__row--overdue .todo__title {
  color: var(--home-text-primary);
  font-weight: 600;
}

.todo__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #ffffff;
  background: transparent;
  border: 1px solid var(--home-text-secondary);
  border-radius: 5px;
  cursor: pointer;
}

.todo__check--done {
  background: var(--system-blue);
  border-color: var(--system-blue);
}

.todo__main {
  min-width: 0;
}

.todo__title {
  display: block;
  overflow: hidden;
  font-size: 14px;
  color: var(--home-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo__row--done .todo__title {
  text-decoration: line-through;
  text-decoration-color: color-mix(in srgb, var(--home-text-secondary) 58%, transparent);
}

.todo__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-top: 5px;
}

.todo__tag {
  padding: 2px 8px;
  font-size: 12px;
  color: var(--home-tag-text);
  background: var(--home-tag-bg);
  border-radius: var(--radius-full);
}

.todo__overdue-badge {
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  background: color-mix(in srgb, var(--system-red) 72%, transparent);
  border: 1px solid color-mix(in srgb, var(--system-red) 70%, #ffffff);
  border-radius: var(--radius-full);
  box-shadow: 0 6px 16px color-mix(in srgb, var(--system-red) 24%, transparent);
}

.todo__time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--home-text-tertiary);
  background: var(--home-control-bg);
  border: 1px solid var(--home-control-border);
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.todo__time--scheduled {
  color: var(--system-blue);
  background: color-mix(in srgb, var(--system-blue) 20%, transparent);
  border-color: color-mix(in srgb, var(--system-blue) 28%, transparent);
}

.todo__time--overdue {
  color: #ffffff;
  background: color-mix(in srgb, var(--system-red) 34%, transparent);
  border-color: color-mix(in srgb, var(--system-red) 46%, transparent);
}

.todo__row-actions {
  display: inline-flex;
  gap: 4px;
}

.todo__row-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--home-text-tertiary);
  background: var(--home-control-bg);
  border: 1px solid var(--home-control-border);
  border-radius: 8px;
  cursor: pointer;
}

.todo__row-actions button:hover:not(:disabled) {
  color: var(--home-text-primary);
  background: var(--home-surface-hover);
}

.todo__row-actions button:disabled {
  cursor: wait;
  opacity: 0.5;
}

.todo__add {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--home-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
}

.todo__add:hover {
  color: var(--home-text-primary);
}

.todo__empty:hover {
  color: var(--home-primary-on-accent);
  filter: brightness(1.03);
}

.todo__editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.todo__editor input {
  height: 34px;
  padding: 0 12px;
  font-family: inherit;
  color: var(--home-text-primary);
  background: var(--home-surface-bg-strong);
  border: 1px solid var(--home-surface-border);
  border-radius: 12px;
}

.todo__editor input:focus {
  outline: 2px solid color-mix(in srgb, var(--system-blue) 38%, transparent);
  outline-offset: 1px;
}

.todo__editor input::placeholder {
  color: var(--home-text-tertiary);
}

.todo__editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 8px;
}

.todo__error {
  margin: -2px 0 0;
  font-size: 12px;
  color: #fecaca;
}

.todo__editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.todo__editor-actions button {
  align-self: flex-end;
  height: 34px;
  padding: 0 16px;
  color: #ffffff;
  background: var(--system-blue);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
}

.todo__editor-actions button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.todo__editor-actions .todo__cancel {
  color: var(--home-text-secondary);
  background: var(--home-control-bg);
}

.todo__empty {
  align-self: flex-start;
  min-height: 38px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 700;
  color: var(--home-primary-on-accent);
  background: var(--accent-grad);
  border: none;
  border-radius: var(--radius-full);
  box-shadow: 0 8px 18px var(--accent-shadow);
  cursor: pointer;
}

@media (max-width: 480px) {
  .panel {
    min-height: auto;
    padding: 14px;
  }

  .panel__head h2 {
    font-size: 16px;
  }

  .todo__row {
    grid-template-columns: 24px minmax(0, 1fr) auto;
    gap: 8px;
    min-height: 54px;
    padding: 8px;
  }

  .todo__editor-grid {
    grid-template-columns: 1fr;
  }

  .todo__meta {
    gap: 5px;
  }

  .todo__tag {
    max-width: 88px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .todo__row-actions {
    flex-direction: column;
  }
}
</style>
