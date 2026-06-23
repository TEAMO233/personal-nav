<script setup lang="ts">
/**
 * 灵感便签面板:展示置顶 / 最新便签列表,并支持内联新增、编辑和删除。
 */
import { ref } from 'vue'
import { useNoteStore } from '@/stores/note'
import AppIcon from '@/components/AppIcon.vue'
import type { NoteItem } from '@/api/types'

const noteStore = useNoteStore()
const editorMode = ref<'create' | 'edit' | null>(null)
const editingId = ref<string | null>(null)
const content = ref('')
const deletingId = ref<string | null>(null)

/**
 * 打开新增编辑器。
 */
function startCreate(): void {
  // 1. 新增时清空输入
  content.value = ''
  editingId.value = null
  editorMode.value = 'create'
}

/**
 * 打开编辑指定便签。
 *
 * @param item 需要编辑的便签
 */
function startEdit(item: NoteItem): void {
  // 1. 编辑时回填当前行内容
  content.value = item.content
  editingId.value = item.id
  editorMode.value = 'edit'
}

/**
 * 关闭编辑器。
 */
function cancelEdit(): void {
  // 1. 收起编辑器并清空临时状态
  editorMode.value = null
  editingId.value = null
  content.value = ''
}

/**
 * 保存便签。
 */
async function save(): Promise<void> {
  // 1. 空内容不提交
  const value = content.value.trim()
  if (!value) return
  // 2. 按模式新增或更新
  if (editorMode.value === 'create') {
    await noteStore.create({ content: value, pinned: true })
  } else if (editingId.value) {
    await noteStore.update(editingId.value, { content: value, pinned: true })
  }
  // 3. 收起编辑器并清空输入
  cancelEdit()
}

/**
 * 删除指定便签。
 *
 * @param id 便签 id
 */
async function removeNote(id: string): Promise<void> {
  // 1. 防止重复点击同一条删除按钮
  if (deletingId.value) return
  deletingId.value = id
  try {
    // 2. 删除后同步本地列表
    await noteStore.remove(id)
    if (editingId.value === id) cancelEdit()
  } finally {
    deletingId.value = null
  }
}

/**
 * 格式化便签时间。
 *
 * @param value ISO 时间字符串
 */
function displayDate(value: string): string {
  // 1. 后端创建瞬间可能尚未返回创建时间,先显示同步中
  if (!value) return '同步中'
  // 2. 用本地日期展示创建时间
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<template>
  <section class="glass-panel note panel">
    <header class="panel__head">
      <h2><AppIcon name="edit" :size="18" />灵感便签</h2>
      <div class="panel__actions">
        <button type="button" aria-label="新增便签" title="新增便签" @click="startCreate">
          <AppIcon name="plus" :size="17" />
        </button>
      </div>
    </header>

    <form v-if="editorMode" class="note__editor" @submit.prevent="save">
      <textarea
        v-model="content"
        rows="3"
        :placeholder="editorMode === 'create' ? '写下一条新灵感' : '编辑这条灵感'"
      ></textarea>
      <div class="note__editor-actions">
        <button type="button" class="note__cancel" @click="cancelEdit">取消</button>
        <button type="submit">{{ editorMode === 'create' ? '新增' : '保存' }}</button>
      </div>
    </form>

    <div class="note__list" :class="{ 'note__list--empty': !noteStore.items.length }">
      <article v-for="item in noteStore.items" :key="item.id" class="note__card">
        <div class="note__meta">
          <AppIcon v-if="item.pinned" name="star" :size="14" class="note__pin" />
          <time>{{ displayDate(item.createdAt) }}</time>
        </div>
        <p>{{ item.content }}</p>
        <div class="note__row-actions">
          <button type="button" aria-label="编辑便签" title="编辑便签" @click="startEdit(item)">
            <AppIcon name="edit" :size="14" />
          </button>
          <button
            type="button"
            aria-label="删除便签"
            title="删除便签"
            :disabled="deletingId === item.id"
            @click="removeNote(item.id)"
          >
            <AppIcon name="trash" :size="14" />
          </button>
        </div>
      </article>

      <button v-if="!noteStore.items.length" type="button" class="note__empty" @click="startCreate">创建第一条便签</button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  min-height: 146px;
  padding: 16px 20px;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel__head h2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  color: var(--home-text-primary);
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

.note__list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.note__list::-webkit-scrollbar {
  width: 4px;
}

.note__list::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--home-text-secondary) 24%, transparent);
  border-radius: var(--radius-full);
}

.note__list--empty {
  justify-content: center;
}

.note__card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 10px;
  align-items: start;
  padding: 10px 12px;
  background: var(--home-surface-bg);
  border: 1px solid var(--home-surface-border-soft);
  border-radius: 12px;
}

.note__meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  color: color-mix(in srgb, var(--home-text-secondary) 66%, transparent);
}

.note__pin {
  flex: 0 0 auto;
  color: #fbbf24;
}

.note__card p {
  grid-column: 1 / -1;
  margin: 0;
  font-size: 14px;
  line-height: 1.45;
  color: var(--home-text-primary);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.note__row-actions {
  display: inline-flex;
  grid-column: 2;
  grid-row: 1;
  gap: 4px;
  justify-self: end;
}

.note__row-actions button {
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

.note__row-actions button:hover:not(:disabled) {
  color: var(--home-text-primary);
  background: var(--home-surface-hover);
}

.note__row-actions button:disabled {
  cursor: wait;
  opacity: 0.5;
}

.note__empty {
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

.note__editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.note__editor textarea {
  resize: vertical;
  min-height: 72px;
  padding: 12px;
  font-family: inherit;
  color: var(--home-text-primary);
  background: var(--home-surface-bg-strong);
  border: 1px solid var(--home-surface-border);
  border-radius: 12px;
}

.note__editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.note__editor-actions button {
  align-self: flex-end;
  height: 34px;
  padding: 0 16px;
  color: #ffffff;
  background: var(--system-blue);
  border: none;
  border-radius: var(--radius-full);
}

.note__editor-actions .note__cancel {
  color: var(--home-text-secondary);
  background: var(--home-control-bg);
}
</style>
