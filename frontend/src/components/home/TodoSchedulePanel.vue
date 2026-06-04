<script setup lang="ts">
/**
 * 待办 / 日程面板:首页内联添加、完成和删除。
 */
import { ref } from 'vue'
import { useTodoStore } from '@/stores/todo'
import AppIcon from '@/components/AppIcon.vue'
import type { TodoItem } from '@/api/types'

const todoStore = useTodoStore()
const adding = ref(false)
const title = ref('')

/**
 * 添加待办。
 */
async function addTodo(): Promise<void> {
  // 1. 空标题不提交
  const value = title.value.trim()
  if (!value) return
  // 2. 新建后清空输入
  await todoStore.create({ title: value, tag: '工作' })
  title.value = ''
  adding.value = false
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
</script>

<template>
  <section class="glass-panel todo panel">
    <header class="panel__head">
      <h2><AppIcon name="calendar" :size="18" />待办 / 日程</h2>
      <button type="button" @click="adding = true">添加</button>
    </header>

    <div class="todo__list">
      <div v-for="item in todoStore.items.slice(0, 3)" :key="item.id" class="todo__row">
        <button type="button" class="todo__check" :class="{ 'todo__check--done': item.done }" @click="todoStore.toggleDone(item)">
          <AppIcon v-if="item.done" name="check" :size="14" />
        </button>
        <span class="todo__title">{{ item.title }}</span>
        <span class="todo__tag">{{ item.tag || '待办' }}</span>
        <time>{{ displayTime(item) }}</time>
        <button type="button" class="todo__delete" aria-label="删除待办" @click="todoStore.remove(item.id)">
          <AppIcon name="close" :size="14" />
        </button>
      </div>
      <p v-if="!todoStore.items.length" class="panel__empty">暂无待办</p>
    </div>

    <form v-if="adding" class="todo__add-form" @submit.prevent="addTodo">
      <input v-model="title" type="text" placeholder="输入待办事项" />
      <button type="submit">添加</button>
    </form>
    <button v-else type="button" class="todo__add" @click="adding = true">
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
  color: rgba(255, 255, 255, 0.94);
}

.panel__head svg {
  color: #73a7ff;
}

.panel__head button {
  font-size: 13px;
  color: #3b82f6;
  background: transparent;
  border: none;
}

.todo__list {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.todo__row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto auto 24px;
  gap: 10px;
  align-items: center;
  min-height: 36px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.todo__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #ffffff;
  background: transparent;
  border: 1px solid rgba(220, 230, 255, 0.65);
  border-radius: 5px;
  cursor: pointer;
}

.todo__check--done {
  background: #3b82f6;
  border-color: #3b82f6;
}

.todo__title {
  overflow: hidden;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo__tag {
  padding: 2px 8px;
  font-size: 12px;
  color: #77d89a;
  background: rgba(52, 211, 153, 0.12);
  border-radius: var(--radius-full);
}

.todo__row time {
  font-size: 13px;
  color: rgba(220, 230, 255, 0.64);
}

.todo__delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(220, 230, 255, 0.46);
  background: transparent;
  border: none;
  cursor: pointer;
}

.todo__add,
.todo__add-form {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: rgba(220, 230, 255, 0.68);
}

.todo__add {
  background: transparent;
  border: none;
  cursor: pointer;
}

.todo__add-form input {
  flex: 1;
  height: 34px;
  padding: 0 12px;
  color: var(--label-primary);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-full);
}

.todo__add-form button {
  height: 34px;
  padding: 0 14px;
  color: #ffffff;
  background: #2f78ff;
  border: none;
  border-radius: var(--radius-full);
}

.panel__empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  margin: 0;
  font-size: 14px;
  color: rgba(220, 230, 255, 0.54);
  text-align: center;
}
</style>
