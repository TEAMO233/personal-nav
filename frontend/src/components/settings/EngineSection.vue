<script setup lang="ts">
/**
 * 引擎管理区:展示当前用户的搜索引擎,支持拖拽排序、设为默认、新增/编辑/删除,
 * 并通过 IconPicker 配置自定义图标。窄屏把拖拽降级为上移/下移按钮。
 */
import { ref, reactive, computed, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useEngineStore } from '@/stores/engine'
import type { Engine } from '@/api/types'
import { ApiClientError } from '@/api/http'
import { useIsMobile } from '@/composables/useIsMobile'
import EngineIcon from '@/components/EngineIcon.vue'
import IconPicker from '@/components/IconPicker.vue'
import AppIcon from '@/components/AppIcon.vue'

const engineStore = useEngineStore()
const { isMobile } = useIsMobile()

// 拖拽用的本地副本:跟随 store 同步,拖拽/移动后回写排序接口
const localEngines = ref<Engine[]>([])
watch(
  () => engineStore.engines,
  (list) => {
    localEngines.value = [...list]
  },
  { immediate: true, deep: true },
)

// ===== 排序 =====

/**
 * 提交当前顺序到后端;失败则回滚为 store 现状。
 */
async function commitOrder(): Promise<void> {
  // 1. 按本地顺序提交全部引擎 id
  try {
    await engineStore.reorder(localEngines.value.map((e) => e.id))
  } catch (e) {
    // 2. 失败提示并回滚
    ElMessage.error(e instanceof ApiClientError ? e.message : '排序失败')
    localEngines.value = [...engineStore.engines]
  }
}

/**
 * 窄屏上移/下移一个引擎。
 *
 * @param index 当前下标
 * @param dir   方向:-1 上移,1 下移
 */
function moveEngine(index: number, dir: -1 | 1): void {
  // 1. 越界不处理
  const target = index + dir
  if (target < 0 || target >= localEngines.value.length) return
  // 2. 交换位置后提交
  const arr = [...localEngines.value]
  const [item] = arr.splice(index, 1)
  arr.splice(target, 0, item)
  localEngines.value = arr
  void commitOrder()
}

// ===== 设默认 =====

/**
 * 设某引擎为默认。
 *
 * @param id 引擎 id
 */
async function setDefault(id: string): Promise<void> {
  // 1. 调用并提示失败
  try {
    await engineStore.setDefault(id)
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '设置默认失败')
  }
}

// ===== 删除 =====

/**
 * 二次确认后删除引擎。
 *
 * @param engine 目标引擎
 */
async function confirmDelete(engine: Engine): Promise<void> {
  // 1. 弹确认框,取消则直接返回
  try {
    await ElMessageBox.confirm(`确定删除引擎「${engine.name}」吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  // 2. 执行删除(删默认会由后端补设新默认)
  try {
    await engineStore.remove(engine.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '删除失败')
  }
}

// ===== 新增 / 编辑对话框 =====

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const submitting = ref(false)
const form = reactive<{ name: string; urlTemplate: string; iconAssetId: string | null }>({
  name: '',
  urlTemplate: '',
  iconAssetId: null,
})

// favicon 抓取用的站点地址:取 URL 模板里 {query} 之前的部分
const iconSiteUrl = computed(() => (form.urlTemplate ? form.urlTemplate.split('{query}')[0] : ''))

// 表单校验:名称必填;URL 模板必填且须含 {query}
const rules: FormRules = {
  name: [{ required: true, message: '请输入引擎名称', trigger: 'blur' }],
  urlTemplate: [
    { required: true, message: '请输入搜索 URL 模板', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value && !value.includes('{query}')) callback(new Error('URL 模板需包含 {query} 占位符'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

/**
 * 打开"新增"对话框。
 */
function openCreate(): void {
  // 1. 清空表单并标记为新增
  editingId.value = null
  form.name = ''
  form.urlTemplate = ''
  form.iconAssetId = null
  dialogVisible.value = true
}

/**
 * 打开"编辑"对话框并回填。
 *
 * @param engine 目标引擎
 */
function openEdit(engine: Engine): void {
  // 1. 回填字段并记录编辑 id
  editingId.value = engine.id
  form.name = engine.name
  form.urlTemplate = engine.urlTemplate
  form.iconAssetId = engine.iconAssetId
  dialogVisible.value = true
}

/**
 * 对话框打开后清掉上一次的校验红字。
 */
function onDialogOpen(): void {
  // 1. 重置校验状态
  formRef.value?.clearValidate()
}

/**
 * 提交新增或更新。
 */
async function submit(): Promise<void> {
  // 1. 先过表单校验
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 2. 提交;新增走 create,编辑走 update
  submitting.value = true
  try {
    const input = {
      name: form.name.trim(),
      urlTemplate: form.urlTemplate.trim(),
      iconAssetId: form.iconAssetId,
    }
    if (editingId.value) await engineStore.update(editingId.value, input)
    else await engineStore.create(input)
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="engine-section">
    <!-- 区头:标题 + 新增 -->
    <header class="section-head">
      <div>
        <h2 class="section-head__title">搜索引擎</h2>
        <p class="section-head__hint">拖动排序,可设默认、改图标;预置引擎也能改或删。</p>
      </div>
      <el-button type="primary" round @click="openCreate">
        <AppIcon name="plus" :size="16" />
        <span class="section-head__btn-text">新增引擎</span>
      </el-button>
    </header>

    <!-- 加载 / 空态 -->
    <div v-if="!engineStore.loaded" class="section-state">加载中…</div>
    <div v-else-if="localEngines.length === 0" class="section-state">还没有引擎,点"新增引擎"添加。</div>

    <!-- 引擎列表(可拖拽;窄屏禁用拖拽改用按钮) -->
    <VueDraggable
      v-else
      v-model="localEngines"
      :animation="150"
      handle=".drag-handle"
      :disabled="isMobile"
      class="engine-list"
      @update="commitOrder"
    >
      <div v-for="(e, i) in localEngines" :key="e.id" class="engine-row">
        <!-- 桌面拖拽手柄 -->
        <span v-if="!isMobile" class="drag-handle"><AppIcon name="grip" :size="18" /></span>
        <!-- 窄屏上移/下移 -->
        <div v-else class="s-move-btns">
          <button :disabled="i === 0" aria-label="上移" @click="moveEngine(i, -1)">
            <AppIcon name="arrow-up" :size="15" />
          </button>
          <button :disabled="i === localEngines.length - 1" aria-label="下移" @click="moveEngine(i, 1)">
            <AppIcon name="arrow-down" :size="15" />
          </button>
        </div>

        <EngineIcon :engine="e" :size="28" />
        <div class="engine-row__info">
          <span class="engine-row__name">{{ e.name }}</span>
          <span class="engine-row__url">{{ e.urlTemplate }}</span>
        </div>

        <!-- 默认标记 / 设为默认 -->
        <span v-if="e.isDefault" class="s-badge-default"><AppIcon name="check" :size="13" />默认</span>
        <button v-else type="button" class="s-link-btn" @click="setDefault(e.id)">设为默认</button>

        <!-- 编辑 / 删除 -->
        <button type="button" class="s-icon-btn" aria-label="编辑" @click="openEdit(e)">
          <AppIcon name="edit" :size="16" />
        </button>
        <button
          type="button"
          class="s-icon-btn s-icon-btn--danger"
          aria-label="删除"
          @click="confirmDelete(e)"
        >
          <AppIcon name="trash" :size="16" />
        </button>
      </div>
    </VueDraggable>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑引擎' : '新增引擎'"
      width="460px"
      align-center
      @open="onDialogOpen"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 Google" maxlength="40" />
        </el-form-item>
        <el-form-item label="搜索 URL 模板" prop="urlTemplate">
          <el-input v-model="form.urlTemplate" placeholder="https://www.google.com/search?q={query}" />
          <div class="field-hint">用 {query} 表示搜索词的位置</div>
        </el-form-item>
        <el-form-item label="图标">
          <IconPicker v-model="form.iconAssetId" :site-url="iconSiteUrl" />
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
.engine-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* 区头 */
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

/* 列表 */
.engine-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.engine-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background: var(--bg-elevated);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-lg);
}

.drag-handle {
  display: inline-flex;
  flex-shrink: 0;
  color: var(--label-tertiary);
  cursor: grab;
}

.drag-handle:active {
  cursor: grabbing;
}

.engine-row__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.engine-row__name {
  font-size: var(--text-subhead);
  font-weight: 600;
  color: var(--label-primary);
}

.engine-row__url {
  overflow: hidden;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 表单内提示文字 */
.field-hint {
  margin-top: 4px;
  font-size: var(--text-caption1);
  line-height: 1.4;
  color: var(--label-tertiary);
}

@media (max-width: 640px) {
  .engine-row {
    gap: var(--space-2);
    padding: var(--space-2);
  }

  .engine-row__url {
    display: none;
  }
}
</style>
