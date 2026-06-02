<script setup lang="ts">
/**
 * 分组与快捷方式管理区:
 * - 分组:新增/编辑/删除 + 拖拽排序(窄屏用上移/下移)。
 * - 快捷方式:新增/编辑/删除 + 组内拖拽 + 组间拖拽(改所属分组),窄屏降级为上移/下移/移到分组。
 * 拖拽后按"全量快照"提交排序接口(后端要求覆盖全部快捷方式)。
 */
import { ref, reactive, computed, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useShortcutStore } from '@/stores/shortcut'
import type { Group, Shortcut, ShortcutOrderItem } from '@/api/types'
import { ApiClientError } from '@/api/http'
import { mediaUrl } from '@/api/media'
import { useIsMobile } from '@/composables/useIsMobile'
import IconPicker from '@/components/IconPicker.vue'
import AppIcon from '@/components/AppIcon.vue'

/** 本地拖拽用的分组结构:一个分组及其组内快捷方式 */
interface LocalGroup {
  group: Group
  shortcuts: Shortcut[]
}

const shortcutStore = useShortcutStore()
const { isMobile } = useIsMobile()

// 拖拽用的本地副本:跟随 store 同步,拖拽/移动后回写排序接口
const localGroups = ref<LocalGroup[]>([])

/**
 * 从 store 重建本地副本(初始化与排序失败回滚时用)。
 */
function syncFromStore(): void {
  // 1. 深拷贝分组与其组内快捷方式,避免直接改 store
  localGroups.value = shortcutStore.grouped.map((g) => ({
    group: g.group,
    shortcuts: [...g.shortcuts],
  }))
}

watch(() => shortcutStore.grouped, syncFromStore, { immediate: true, deep: true })

// 可选作"移动目标"的分组列表(供新增快捷方式时选择)
const groupsForSelect = computed<Group[]>(() => localGroups.value.map((lg) => lg.group))

/**
 * 取某个分组之外的其他分组(窄屏"移到分组"用)。
 *
 * @param currentGroupId 当前分组 id
 * @returns 其他分组
 */
function otherGroups(currentGroupId: string): Group[] {
  // 1. 过滤掉自己
  return localGroups.value.filter((lg) => lg.group.id !== currentGroupId).map((lg) => lg.group)
}

// ===== 排序提交 =====

/**
 * 把所有分组里所有快捷方式拼成"全量位置"快照。
 *
 * @returns 全部快捷方式的 {id, groupId, sortOrder}
 */
function buildOrderItems(): ShortcutOrderItem[] {
  // 1. 遍历每个分组,组内按当前下标作 sortOrder,groupId 取所在分组
  const items: ShortcutOrderItem[] = []
  for (const lg of localGroups.value) {
    lg.shortcuts.forEach((s, idx) => {
      items.push({ id: s.id, groupId: lg.group.id, sortOrder: idx })
    })
  }
  return items
}

/**
 * 提交分组顺序;失败回滚。
 */
async function commitGroupOrder(): Promise<void> {
  // 1. 按本地分组顺序提交全部分组 id
  try {
    await shortcutStore.reorderGroups(localGroups.value.map((lg) => lg.group.id))
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '分组排序失败')
    syncFromStore()
  }
}

/**
 * 提交快捷方式全量位置(组内排序与跨组移动都走这里);失败回滚。
 */
async function commitShortcutOrder(): Promise<void> {
  // 1. 提交全量快照
  try {
    await shortcutStore.reorderShortcuts(buildOrderItems())
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '快捷方式排序失败')
    syncFromStore()
  }
}

// ===== 窄屏移动 =====

/**
 * 窄屏上移/下移一个分组。
 *
 * @param index 当前下标
 * @param dir   -1 上移,1 下移
 */
function moveGroup(index: number, dir: -1 | 1): void {
  // 1. 越界不处理
  const target = index + dir
  if (target < 0 || target >= localGroups.value.length) return
  // 2. 交换后提交
  const arr = [...localGroups.value]
  const [item] = arr.splice(index, 1)
  arr.splice(target, 0, item)
  localGroups.value = arr
  void commitGroupOrder()
}

/**
 * 窄屏组内上移/下移一个快捷方式。
 *
 * @param lg    所在分组
 * @param index 当前下标
 * @param dir   -1 上移,1 下移
 */
function moveShortcut(lg: LocalGroup, index: number, dir: -1 | 1): void {
  // 1. 越界不处理
  const target = index + dir
  if (target < 0 || target >= lg.shortcuts.length) return
  // 2. 组内交换后提交
  const arr = [...lg.shortcuts]
  const [item] = arr.splice(index, 1)
  arr.splice(target, 0, item)
  lg.shortcuts = arr
  void commitShortcutOrder()
}

/**
 * 窄屏把快捷方式移到另一个分组(放到目标组末尾)。
 *
 * @param shortcut      目标快捷方式
 * @param targetGroupId 目标分组 id
 */
function moveShortcutToGroup(shortcut: Shortcut, targetGroupId: string): void {
  // 1. 找到原组与目标组
  const fromGroup = localGroups.value.find((g) => g.shortcuts.some((x) => x.id === shortcut.id))
  const toGroup = localGroups.value.find((g) => g.group.id === targetGroupId)
  if (!fromGroup || !toGroup) return
  // 2. 从原组移除并加到目标组末尾,再提交
  fromGroup.shortcuts = fromGroup.shortcuts.filter((x) => x.id !== shortcut.id)
  toGroup.shortcuts.push(shortcut)
  void commitShortcutOrder()
}

// ===== 分组对话框 =====

const groupFormRef = ref<FormInstance>()
const groupDialogVisible = ref(false)
const groupEditingId = ref<string | null>(null)
const groupSubmitting = ref(false)
const groupForm = reactive<{ name: string }>({ name: '' })
const groupRules: FormRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
}

/**
 * 打开"新增分组"对话框。
 */
function openCreateGroup(): void {
  // 1. 清空并标记新增
  groupEditingId.value = null
  groupForm.name = ''
  groupDialogVisible.value = true
}

/**
 * 打开"编辑分组"对话框。
 *
 * @param group 目标分组
 */
function openEditGroup(group: Group): void {
  // 1. 回填名称
  groupEditingId.value = group.id
  groupForm.name = group.name
  groupDialogVisible.value = true
}

/**
 * 提交分组新增/更新。
 */
async function submitGroup(): Promise<void> {
  // 1. 校验
  if (!groupFormRef.value) return
  try {
    await groupFormRef.value.validate()
  } catch {
    return
  }
  // 2. 新增或更新
  groupSubmitting.value = true
  try {
    if (groupEditingId.value) await shortcutStore.updateGroup(groupEditingId.value, { name: groupForm.name.trim() })
    else await shortcutStore.createGroup({ name: groupForm.name.trim() })
    ElMessage.success('已保存')
    groupDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '保存失败')
  } finally {
    groupSubmitting.value = false
  }
}

/**
 * 二次确认后删除分组(会级联删除组内全部快捷方式)。
 *
 * @param group 目标分组
 */
async function confirmDeleteGroup(group: Group): Promise<void> {
  // 1. 确认(提示会连带删快捷方式)
  try {
    await ElMessageBox.confirm(`删除分组「${group.name}」会同时删除组内全部快捷方式,确定吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  // 2. 执行删除
  try {
    await shortcutStore.removeGroup(group.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '删除失败')
  }
}

// ===== 快捷方式对话框 =====

const scFormRef = ref<FormInstance>()
const scDialogVisible = ref(false)
const scEditingId = ref<string | null>(null)
const scSubmitting = ref(false)
const scForm = reactive<{ groupId: string; name: string; url: string; iconAssetId: string | null }>({
  groupId: '',
  name: '',
  url: '',
  iconAssetId: null,
})
const scRules: FormRules = {
  groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网址', trigger: 'blur' }],
}

/**
 * 打开"新增快捷方式"对话框(预设所属分组)。
 *
 * @param groupId 所属分组 id
 */
function openCreateShortcut(groupId: string): void {
  // 1. 清空并预设分组
  scEditingId.value = null
  scForm.groupId = groupId
  scForm.name = ''
  scForm.url = ''
  scForm.iconAssetId = null
  scDialogVisible.value = true
}

/**
 * 打开"编辑快捷方式"对话框(不在此改分组,跨组移动走拖拽/移到分组)。
 *
 * @param shortcut 目标快捷方式
 */
function openEditShortcut(shortcut: Shortcut): void {
  // 1. 回填字段
  scEditingId.value = shortcut.id
  scForm.groupId = shortcut.groupId
  scForm.name = shortcut.name
  scForm.url = shortcut.url
  scForm.iconAssetId = shortcut.iconAssetId
  scDialogVisible.value = true
}

/**
 * 对话框打开后清掉上次校验红字。
 */
function onScDialogOpen(): void {
  scFormRef.value?.clearValidate()
}

/**
 * 提交快捷方式新增/更新。
 */
async function submitShortcut(): Promise<void> {
  // 1. 校验
  if (!scFormRef.value) return
  try {
    await scFormRef.value.validate()
  } catch {
    return
  }
  // 2. 新增或更新(更新时后端忽略 groupId)
  scSubmitting.value = true
  try {
    const input = {
      groupId: scForm.groupId,
      name: scForm.name.trim(),
      url: scForm.url.trim(),
      iconAssetId: scForm.iconAssetId,
    }
    if (scEditingId.value) await shortcutStore.updateShortcut(scEditingId.value, input)
    else await shortcutStore.createShortcut(input)
    ElMessage.success('已保存')
    scDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '保存失败')
  } finally {
    scSubmitting.value = false
  }
}

/**
 * 二次确认后删除快捷方式。
 *
 * @param shortcut 目标快捷方式
 */
async function confirmDeleteShortcut(shortcut: Shortcut): Promise<void> {
  // 1. 确认
  try {
    await ElMessageBox.confirm(`确定删除「${shortcut.name}」吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  // 2. 执行删除
  try {
    await shortcutStore.removeShortcut(shortcut.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '删除失败')
  }
}

/**
 * 快捷方式无图标时的占位首字母。
 *
 * @param name 名称
 * @returns 大写首字母
 */
function initialOf(name: string): string {
  // 1. 取首字符大写,空名兜底问号
  return name.trim().charAt(0).toUpperCase() || '?'
}
</script>

<template>
  <section class="shortcut-section">
    <!-- 区头:标题 + 新增分组 -->
    <header class="section-head">
      <div>
        <h2 class="section-head__title">快捷方式</h2>
        <p class="section-head__hint">按分组管理网站;拖动排序,可在分组间移动。</p>
      </div>
      <el-button type="primary" round @click="openCreateGroup">
        <AppIcon name="plus" :size="16" />
        <span class="section-head__btn-text">新增分组</span>
      </el-button>
    </header>

    <!-- 加载 / 空态 -->
    <div v-if="!shortcutStore.loaded" class="section-state">加载中…</div>
    <div v-else-if="localGroups.length === 0" class="section-state">
      还没有分组,点"新增分组"开始整理你的网站。
    </div>

    <!-- 分组列表(可拖拽排序;窄屏用按钮) -->
    <VueDraggable
      v-else
      v-model="localGroups"
      group="groups"
      handle=".group-drag-handle"
      :animation="150"
      :disabled="isMobile"
      class="group-list"
      @update="commitGroupOrder"
    >
      <div v-for="(lg, gi) in localGroups" :key="lg.group.id" class="group-card">
        <!-- 分组头 -->
        <div class="group-card__head">
          <!-- 桌面分组拖拽手柄 -->
          <span v-if="!isMobile" class="group-drag-handle"><AppIcon name="grip" :size="18" /></span>
          <!-- 窄屏分组上移/下移 -->
          <div v-else class="s-move-btns">
            <button :disabled="gi === 0" aria-label="上移分组" @click="moveGroup(gi, -1)">
              <AppIcon name="arrow-up" :size="15" />
            </button>
            <button
              :disabled="gi === localGroups.length - 1"
              aria-label="下移分组"
              @click="moveGroup(gi, 1)"
            >
              <AppIcon name="arrow-down" :size="15" />
            </button>
          </div>

          <span class="group-card__name">{{ lg.group.name }}</span>
          <span class="group-card__count">{{ lg.shortcuts.length }}</span>

          <button type="button" class="s-icon-btn" aria-label="编辑分组" @click="openEditGroup(lg.group)">
            <AppIcon name="edit" :size="16" />
          </button>
          <button
            type="button"
            class="s-icon-btn s-icon-btn--danger"
            aria-label="删除分组"
            @click="confirmDeleteGroup(lg.group)"
          >
            <AppIcon name="trash" :size="16" />
          </button>
        </div>

        <!-- 组内快捷方式(组内 + 组间拖拽) -->
        <VueDraggable
          v-model="lg.shortcuts"
          group="shortcuts"
          handle=".sc-drag-handle"
          :animation="150"
          :disabled="isMobile"
          class="sc-list"
          @end="commitShortcutOrder"
        >
          <div v-for="(s, si) in lg.shortcuts" :key="s.id" class="sc-row">
            <!-- 桌面拖拽手柄 -->
            <span v-if="!isMobile" class="sc-drag-handle"><AppIcon name="grip" :size="16" /></span>
            <!-- 窄屏上移/下移 -->
            <div v-else class="s-move-btns">
              <button :disabled="si === 0" aria-label="上移" @click="moveShortcut(lg, si, -1)">
                <AppIcon name="arrow-up" :size="14" />
              </button>
              <button
                :disabled="si === lg.shortcuts.length - 1"
                aria-label="下移"
                @click="moveShortcut(lg, si, 1)"
              >
                <AppIcon name="arrow-down" :size="14" />
              </button>
            </div>

            <!-- 图标:有自定义图标用图,否则首字母 -->
            <img v-if="s.iconAssetId" :src="mediaUrl(s.iconAssetId)" :alt="s.name" class="sc-icon" />
            <span v-else class="sc-icon sc-icon--text">{{ initialOf(s.name) }}</span>

            <div class="sc-row__info">
              <span class="sc-row__name">{{ s.name }}</span>
              <span class="sc-row__url">{{ s.url }}</span>
            </div>

            <!-- 窄屏:移到其他分组(多于一个分组时才显示) -->
            <el-dropdown
              v-if="isMobile && localGroups.length > 1"
              trigger="click"
              @command="(gid: string) => moveShortcutToGroup(s, gid)"
            >
              <button type="button" class="s-icon-btn" aria-label="移到分组">
                <AppIcon name="folder" :size="16" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="g in otherGroups(lg.group.id)"
                    :key="g.id"
                    :command="g.id"
                  >
                    {{ g.name }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <button type="button" class="s-icon-btn" aria-label="编辑" @click="openEditShortcut(s)">
              <AppIcon name="edit" :size="16" />
            </button>
            <button
              type="button"
              class="s-icon-btn s-icon-btn--danger"
              aria-label="删除"
              @click="confirmDeleteShortcut(s)"
            >
              <AppIcon name="trash" :size="16" />
            </button>
          </div>
        </VueDraggable>

        <!-- 空组提示(也是组间拖入的落点) -->
        <div v-if="lg.shortcuts.length === 0" class="sc-empty">
          这个分组还没有快捷方式
        </div>

        <!-- 添加快捷方式 -->
        <button type="button" class="add-sc" @click="openCreateShortcut(lg.group.id)">
          <AppIcon name="plus" :size="15" />
          添加快捷方式
        </button>
      </div>
    </VueDraggable>

    <!-- 分组对话框 -->
    <el-dialog
      v-model="groupDialogVisible"
      :title="groupEditingId ? '编辑分组' : '新增分组'"
      width="400px"
      align-center
      @open="() => groupFormRef?.clearValidate()"
    >
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-position="top">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如 常用、工具" maxlength="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 快捷方式对话框 -->
    <el-dialog
      v-model="scDialogVisible"
      :title="scEditingId ? '编辑快捷方式' : '新增快捷方式'"
      width="460px"
      align-center
      @open="onScDialogOpen"
    >
      <el-form ref="scFormRef" :model="scForm" :rules="scRules" label-position="top">
        <!-- 仅新增时可选分组;编辑时跨组移动走拖拽/移到分组 -->
        <el-form-item v-if="!scEditingId" label="所属分组" prop="groupId">
          <el-select v-model="scForm.groupId" placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in groupsForSelect" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="scForm.name" placeholder="如 GitHub" maxlength="40" />
        </el-form-item>
        <el-form-item label="网址" prop="url">
          <el-input v-model="scForm.url" placeholder="https://github.com" />
        </el-form-item>
        <el-form-item label="图标">
          <IconPicker v-model="scForm.iconAssetId" :site-url="scForm.url" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scSubmitting" @click="submitShortcut">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.shortcut-section {
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

/* 分组列表 */
.group-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.group-card {
  padding: var(--space-3);
  background: var(--bg-elevated);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-xl);
}

.group-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-1) var(--space-2);
}

.group-drag-handle {
  display: inline-flex;
  flex-shrink: 0;
  color: var(--label-tertiary);
  cursor: grab;
}

.group-drag-handle:active {
  cursor: grabbing;
}

.group-card__name {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--label-primary);
}

.group-card__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  margin-right: auto;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
}

/* 组内快捷方式列表 */
.sc-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-height: 8px;
}

.sc-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.sc-drag-handle {
  display: inline-flex;
  flex-shrink: 0;
  color: var(--label-tertiary);
  cursor: grab;
}

.sc-drag-handle:active {
  cursor: grabbing;
}

/* 快捷方式图标 */
.sc-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.sc-icon--text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-footnote);
  font-weight: 600;
  color: #fff;
  background: var(--system-gray);
}

.sc-row__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.sc-row__name {
  font-size: var(--text-subhead);
  color: var(--label-primary);
}

.sc-row__url {
  overflow: hidden;
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空组提示 */
.sc-empty {
  padding: var(--space-3);
  font-size: var(--text-footnote);
  color: var(--label-tertiary);
  text-align: center;
  border: 1px dashed var(--separator);
  border-radius: var(--radius-md);
}

/* 添加快捷方式 */
.add-sc {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 34px;
  margin-top: var(--space-2);
  padding: 0 var(--space-3);
  font-size: var(--text-footnote);
  color: var(--system-blue);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.add-sc:hover {
  background: color-mix(in srgb, var(--system-blue) 10%, transparent);
}

@media (max-width: 640px) {
  .sc-row__url {
    display: none;
  }
}
</style>
