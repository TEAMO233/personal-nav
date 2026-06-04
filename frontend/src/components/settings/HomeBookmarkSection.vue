<script setup lang="ts">
/**
 * 首页书签设置区:配置首页底部书签面板的名称、链接、说明、图标、启用状态和顺序。
 */
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { mediaUrl } from '@/api/media'
import { ApiClientError } from '@/api/http'
import { useHomeBookmarkStore } from '@/stores/homeBookmark'
import type { HomeBookmark } from '@/api/types'
import IconPicker from '@/components/IconPicker.vue'
import AppIcon from '@/components/AppIcon.vue'

const bookmarkStore = useHomeBookmarkStore()

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const submitting = ref(false)
const form = reactive<{
  name: string
  url: string
  description: string
  iconAssetId: string | null
  enabled: boolean
}>({
  name: '',
  url: '',
  description: '',
  iconAssetId: null,
  enabled: true,
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入书签名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入书签链接', trigger: 'blur' }],
}

/**
 * 打开新增书签对话框。
 */
function openCreate(): void {
  // 1. 清空表单并标记新增
  editingId.value = null
  form.name = ''
  form.url = ''
  form.description = ''
  form.iconAssetId = null
  form.enabled = true
  dialogVisible.value = true
}

/**
 * 打开编辑书签对话框。
 *
 * @param item 目标书签
 */
function openEdit(item: HomeBookmark): void {
  // 1. 回填表单
  editingId.value = item.id
  form.name = item.name
  form.url = item.url
  form.description = item.description ?? ''
  form.iconAssetId = item.iconAssetId
  form.enabled = item.enabled
  dialogVisible.value = true
}

/**
 * 对话框打开后清理校验状态。
 */
function onDialogOpen(): void {
  // 1. 清掉上次校验红字
  formRef.value?.clearValidate()
}

/**
 * 提交新增或更新。
 */
async function submit(): Promise<void> {
  // 1. 表单校验
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 2. 调 store 保存
  submitting.value = true
  try {
    const input = {
      name: form.name.trim(),
      url: form.url.trim(),
      description: form.description.trim() || null,
      iconAssetId: form.iconAssetId,
      enabled: form.enabled,
    }
    if (editingId.value) await bookmarkStore.update(editingId.value, input)
    else await bookmarkStore.create(input)
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

/**
 * 切换启用状态。
 *
 * @param item 目标书签
 * @param enabled 是否启用
 */
async function toggleEnabled(item: HomeBookmark, enabled: boolean): Promise<void> {
  // 1. 复用更新接口
  try {
    await bookmarkStore.update(item.id, {
      name: item.name,
      url: item.url,
      description: item.description,
      iconAssetId: item.iconAssetId,
      enabled,
    })
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '更新失败')
  }
}

/**
 * 上移或下移书签。
 *
 * @param index 当前下标
 * @param dir   -1 上移,1 下移
 */
async function moveBookmark(index: number, dir: -1 | 1): Promise<void> {
  // 1. 越界不处理
  const target = index + dir
  if (target < 0 || target >= bookmarkStore.items.length) return
  // 2. 交换顺序并提交完整 id 列表
  const next = [...bookmarkStore.items]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  try {
    await bookmarkStore.reorder(next.map((bookmark) => bookmark.id))
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '排序失败')
  }
}

/**
 * 二次确认后删除书签。
 *
 * @param item 目标书签
 */
async function confirmDelete(item: HomeBookmark): Promise<void> {
  // 1. 确认删除
  try {
    await ElMessageBox.confirm(`确定删除「${item.name}」吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  // 2. 执行删除
  try {
    await bookmarkStore.remove(item.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '删除失败')
  }
}

/**
 * 提取域名用于列表副标题。
 *
 * @param url 目标 URL
 * @returns 域名或原 URL
 */
function domainOf(url: string): string {
  // 1. 尝试解析 URL
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

/**
 * 取首字母占位图标。
 *
 * @param name 名称
 * @returns 首字符
 */
function initialOf(name: string): string {
  // 1. 空名兜底问号
  return name.trim().charAt(0).toUpperCase() || '?'
}
</script>

<template>
  <section class="bookmark-section">
    <!-- 区头:标题 + 新增书签 -->
    <header class="section-head">
      <div>
        <h2 class="section-head__title">首页书签</h2>
        <p class="section-head__hint">配置首页左下角书签面板;可隐藏、排序和自定义图标。</p>
      </div>
      <el-button type="primary" round @click="openCreate">
        <AppIcon name="plus" :size="16" />
        <span class="section-head__btn-text">新增书签</span>
      </el-button>
    </header>

    <!-- 加载 / 空态 -->
    <div v-if="!bookmarkStore.allLoaded" class="section-state">加载中…</div>
    <div v-else-if="bookmarkStore.items.length === 0" class="section-state">
      还没有首页书签,点"新增书签"添加常用链接。
    </div>

    <!-- 书签列表 -->
    <div v-else class="bookmark-list">
      <div v-for="(item, index) in bookmarkStore.items" :key="item.id" class="bookmark-row">
        <div class="move-btns">
          <button :disabled="index === 0" aria-label="上移书签" @click="moveBookmark(index, -1)">
            <AppIcon name="arrow-up" :size="14" />
          </button>
          <button
            :disabled="index === bookmarkStore.items.length - 1"
            aria-label="下移书签"
            @click="moveBookmark(index, 1)"
          >
            <AppIcon name="arrow-down" :size="14" />
          </button>
        </div>

        <img v-if="item.iconAssetId" :src="mediaUrl(item.iconAssetId)" :alt="item.name" class="bookmark-icon" />
        <span v-else class="bookmark-icon bookmark-icon--text">{{ initialOf(item.name) }}</span>

        <div class="bookmark-row__info">
          <span class="bookmark-row__name">{{ item.name }}</span>
          <span class="bookmark-row__meta">{{ item.description || domainOf(item.url) }}</span>
        </div>

        <el-switch
          :model-value="item.enabled"
          size="small"
          aria-label="启用首页书签"
          @change="(value: string | number | boolean) => toggleEnabled(item, Boolean(value))"
        />

        <button type="button" class="s-icon-btn" aria-label="编辑" @click="openEdit(item)">
          <AppIcon name="edit" :size="16" />
        </button>
        <button
          type="button"
          class="s-icon-btn s-icon-btn--danger"
          aria-label="删除"
          @click="confirmDelete(item)"
        >
          <AppIcon name="trash" :size="16" />
        </button>
      </div>
    </div>

    <!-- 书签对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑首页书签' : '新增首页书签'"
      width="460px"
      align-center
      @open="onDialogOpen"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 Figma" maxlength="96" />
        </el-form-item>
        <el-form-item label="链接" prop="url">
          <el-input v-model="form.url" placeholder="https://www.figma.com" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="160"
            show-word-limit
            placeholder="可选,显示在首页书签副标题"
          />
        </el-form-item>
        <el-form-item label="图标">
          <IconPicker v-model="form.iconAssetId" :site-url="form.url" />
        </el-form-item>
        <el-form-item label="显示在首页">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.bookmark-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.section-head__title {
  font-size: var(--text-title3);
}

.section-head__hint {
  margin-top: 2px;
  font-size: var(--text-footnote);
  color: var(--label-secondary);
}

.section-head__btn-text {
  margin-left: 4px;
}

.section-state {
  padding: var(--space-8) 0;
  color: var(--label-secondary);
  text-align: center;
}

.bookmark-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.bookmark-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--bg-elevated);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-lg);
}

.move-btns {
  display: inline-flex;
  flex-shrink: 0;
  flex-direction: column;
  gap: 2px;
}

.move-btns button,
.s-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--label-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.move-btns button:hover:not(:disabled),
.s-icon-btn:hover {
  color: var(--label-primary);
  background: var(--bg-secondary);
}

.move-btns button:disabled {
  color: var(--label-quaternary);
  cursor: not-allowed;
}

.s-icon-btn--danger:hover {
  color: var(--system-red);
}

.bookmark-icon {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.bookmark-icon--text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-footnote);
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--system-blue), var(--system-purple));
}

.bookmark-row__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.bookmark-row__name,
.bookmark-row__meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bookmark-row__name {
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--label-primary);
}

.bookmark-row__meta {
  font-size: var(--text-caption1);
  color: var(--label-secondary);
}

@media (max-width: 640px) {
  .section-head {
    align-items: stretch;
    flex-direction: column;
  }

  .bookmark-row {
    gap: var(--space-1);
  }

  .bookmark-row__meta {
    display: none;
  }
}
</style>
