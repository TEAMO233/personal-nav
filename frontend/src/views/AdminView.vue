<script setup lang="ts">
/**
 * 管理后台(仅 ADMIN 可达,角色由路由守卫保证):
 * 用户管理(列表 + 开户 + 重置密码 + 启用/禁用) + 邀请码签发。进入时拉取用户列表。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import * as adminApi from '@/api/admin'
import type { AdminUser, InviteCode, Role } from '@/api/types'
import { ApiClientError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
// 当前登录管理员(用于隐藏「禁用自己」)
const auth = useAuthStore()

// ===== 用户列表 =====

const users = ref<AdminUser[]>([])
const loadingUsers = ref(true)
const usersError = ref('')

/**
 * 拉取全部用户填表。
 */
async function loadUsers(): Promise<void> {
  // 1. 进入加载态
  loadingUsers.value = true
  usersError.value = ''
  try {
    // 2. 拉取列表
    users.value = await adminApi.listUsers()
  } catch (e) {
    // 3. 展示错误(401 会被拦截器跳登录)
    usersError.value = e instanceof ApiClientError ? e.message : '加载用户失败,请稍后重试'
  } finally {
    loadingUsers.value = false
  }
}

/**
 * 时间戳转本地可读字符串。
 *
 * @param iso ISO 8601 时间字符串
 */
function formatTime(iso: string): string {
  // 1. 直接用本地化格式
  return new Date(iso).toLocaleString()
}

// ===== 开户对话框 =====

const createFormRef = ref<FormInstance>()
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive<{ username: string; password: string; role: Role }>({
  username: '',
  password: '',
  role: 'USER',
})
// 用户名必填;密码必填且至少 8 位(对齐后端 @Size(min=8))
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

/**
 * 打开开户对话框并清空表单。
 */
function openCreate(): void {
  // 1. 重置为默认普通用户
  createForm.username = ''
  createForm.password = ''
  createForm.role = 'USER'
  createVisible.value = true
}

/**
 * 对话框打开后清掉上一次校验红字。
 */
function onCreateOpen(): void {
  // 1. 重置校验状态
  createFormRef.value?.clearValidate()
}

/**
 * 提交开户:校验 → 开户 → 刷新列表。
 */
async function submitCreate(): Promise<void> {
  // 1. 先过表单校验
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  // 2. 提交开户
  creating.value = true
  try {
    await adminApi.createUser({
      username: createForm.username.trim(),
      password: createForm.password,
      role: createForm.role,
    })
    ElMessage.success('开户成功')
    createVisible.value = false
    // 3. 刷新列表带出新用户
    await loadUsers()
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '开户失败')
  } finally {
    creating.value = false
  }
}

// ===== 重置密码对话框 =====

const resetFormRef = ref<FormInstance>()
const resetVisible = ref(false)
const resetting = ref(false)
const resetTarget = ref<AdminUser | null>(null)
const resetForm = reactive<{ newPassword: string }>({ newPassword: '' })
// 新密码必填且至少 8 位
const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

/**
 * 打开重置密码对话框,记下目标用户。
 *
 * @param user 目标用户
 */
function openReset(user: AdminUser): void {
  // 1. 记目标并清空输入
  resetTarget.value = user
  resetForm.newPassword = ''
  resetVisible.value = true
}

/**
 * 对话框打开后清掉上一次校验红字。
 */
function onResetOpen(): void {
  // 1. 重置校验状态
  resetFormRef.value?.clearValidate()
}

/**
 * 提交重置密码。
 */
async function submitReset(): Promise<void> {
  // 1. 校验并确认目标存在
  if (!resetFormRef.value || !resetTarget.value) return
  try {
    await resetFormRef.value.validate()
  } catch {
    return
  }
  // 2. 按用户 id 重置
  resetting.value = true
  try {
    await adminApi.resetPassword(resetTarget.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '重置失败')
  } finally {
    resetting.value = false
  }
}

/**
 * 启用 / 禁用用户:禁用前二次确认,成功后刷新列表。
 *
 * @param user 目标用户
 */
async function toggleStatus(user: AdminUser): Promise<void> {
  // 1. 禁用是危险操作,先二次确认
  const disabling = user.status === 'ACTIVE'
  if (disabling) {
    try {
      await ElMessageBox.confirm(
        `确定禁用「${user.username}」?该用户会被立即踢下线且无法登录。`,
        '禁用用户',
        { type: 'warning', confirmButtonText: '禁用', cancelButtonText: '取消' },
      )
    } catch {
      // 用户取消,不做任何变更
      return
    }
  }
  // 2. 提交状态变更并刷新列表
  try {
    await adminApi.setUserStatus(user.id, disabling ? 'DISABLED' : 'ACTIVE')
    ElMessage.success(disabling ? '已禁用' : '已启用')
    await loadUsers()
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '操作失败')
  }
}

// ===== 邀请码 =====

// 本次会话签发的邀请码(刷新即清空,仅作临时展示与复制)
const issuedCodes = ref<InviteCode[]>([])
const inviteDays = ref<number>(7)
const issuing = ref(false)

/**
 * 签发一个邀请码,放到列表最前。
 */
async function issueInvite(): Promise<void> {
  // 1. 按有效天数签发
  issuing.value = true
  try {
    const code = await adminApi.createInviteCode(inviteDays.value)
    issuedCodes.value.unshift(code)
    ElMessage.success('邀请码已签发')
  } catch (e) {
    ElMessage.error(e instanceof ApiClientError ? e.message : '签发失败')
  } finally {
    issuing.value = false
  }
}

/**
 * 复制邀请码到剪贴板。
 *
 * @param code 邀请码
 */
async function copyCode(code: string): Promise<void> {
  // 1. 写剪贴板,失败提示手动复制
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败,请手动选择复制')
  }
}

/**
 * 返回首页。
 */
function goHome(): void {
  // 1. 回首页
  router.push({ name: 'home' })
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin">
    <!-- 顶栏:返回 + 标题 + 主题切换 -->
    <header class="admin__topbar">
      <button type="button" class="back-btn" @click="goHome">
        <AppIcon name="arrow-left" :size="20" />
        <span class="back-btn__text">首页</span>
      </button>
      <h1 class="admin__title">管理后台</h1>
      <ThemeToggle />
    </header>

    <!-- 主体 -->
    <main class="admin__main">
      <!-- 用户管理 -->
      <section class="admin-section">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">用户管理</h2>
            <p class="section-head__hint">为用户开户、重置密码;新用户会自动预置 4 个搜索引擎。</p>
          </div>
          <el-button type="primary" round @click="openCreate">
            <AppIcon name="plus" :size="16" />
            <span class="section-head__btn-text">开户</span>
          </el-button>
        </header>

        <!-- 错误 / 加载 / 表格 -->
        <div v-if="usersError" class="section-state">
          <p>{{ usersError }}</p>
          <el-button round @click="loadUsers">重试</el-button>
        </div>
        <div v-else-if="loadingUsers" class="section-state">加载中…</div>
        <el-table v-else :data="users" class="user-table" stripe>
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column label="角色" width="96">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small" effect="light">
                {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="84">
            <template #default="{ row }">{{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="172" align="right">
            <template #default="{ row }">
              <div class="row-actions">
                <button type="button" class="s-link-btn" @click="openReset(row as AdminUser)">重置密码</button>
                <!-- 不给当前登录管理员自己显示启用/禁用,避免自锁 -->
                <button
                  v-if="(row as AdminUser).id !== auth.user?.id"
                  type="button"
                  class="s-link-btn"
                  :class="{ 's-link-btn--danger': (row as AdminUser).status === 'ACTIVE' }"
                  @click="toggleStatus(row as AdminUser)"
                >
                  {{ (row as AdminUser).status === 'ACTIVE' ? '禁用' : '启用' }}
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <!-- 邀请码 -->
      <section class="admin-section">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">邀请码</h2>
            <p class="section-head__hint">签发邀请码,发给新用户用于自助注册。</p>
          </div>
          <div class="invite-issue">
            <span class="invite-issue__label">有效天数</span>
            <el-input-number v-model="inviteDays" :min="1" :max="365" controls-position="right" />
            <el-button type="primary" round :loading="issuing" @click="issueInvite">
              <AppIcon name="plus" :size="16" />
              <span class="section-head__btn-text">签发</span>
            </el-button>
          </div>
        </header>

        <!-- 本次会话签发的码 -->
        <div v-if="issuedCodes.length === 0" class="section-state">还没签发邀请码。点"签发"生成一个。</div>
        <ul v-else class="invite-list">
          <li v-for="(c, i) in issuedCodes" :key="i" class="invite-row">
            <code class="invite-row__code">{{ c.code }}</code>
            <span class="invite-row__exp">过期 {{ formatTime(c.expiresAt) }}</span>
            <button type="button" class="s-icon-btn" aria-label="复制邀请码" @click="copyCode(c.code)">
              <AppIcon name="copy" :size="16" />
            </button>
          </li>
        </ul>
      </section>
    </main>

    <!-- 开户对话框 -->
    <el-dialog v-model="createVisible" title="开户" width="420px" align-center @open="onCreateOpen">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="登录用户名" maxlength="64" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">开户</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" align-center @open="onResetOpen">
      <p class="reset-target">为用户「{{ resetTarget?.username }}」设置新密码</p>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitReset">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* 顶栏:毛玻璃吸顶 */
.admin__topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 56px;
  padding: 0 var(--space-4);
  background: var(--material-bar);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 0.5px solid var(--separator);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 40px;
  padding: 0 var(--space-2);
  color: var(--system-blue);
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.back-btn:hover {
  background: var(--bg-secondary);
}

.admin__title {
  flex: 1;
  font-size: var(--text-body);
  font-weight: 600;
  text-align: center;
}

/* 主体容器 */
.admin__main {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5) var(--space-12);
}

.admin-section {
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
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-8) 0;
  color: var(--label-secondary);
  text-align: center;
}

.user-table {
  width: 100%;
}

/* 表格行内操作:重置密码 / 启用禁用 */
.row-actions {
  display: inline-flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: flex-end;
}

.s-link-btn--danger {
  color: var(--system-red, #ff3b30);
}

/* 邀请码签发控件 */
.invite-issue {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.invite-issue__label {
  font-size: var(--text-footnote);
  color: var(--label-secondary);
  white-space: nowrap;
}

/* 邀请码列表 */
.invite-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.invite-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background: var(--bg-elevated);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-lg);
}

.invite-row__code {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--text-subhead);
  color: var(--label-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-row__exp {
  font-size: var(--text-caption1);
  color: var(--label-secondary);
  white-space: nowrap;
}

.reset-target {
  margin: 0 0 var(--space-4);
  font-size: var(--text-subhead);
  color: var(--label-secondary);
}

@media (max-width: 640px) {
  .admin__topbar {
    padding: 0 var(--space-3);
  }

  .back-btn__text {
    display: none;
  }

  .admin__main {
    gap: var(--space-6);
    padding: var(--space-4) var(--space-3) var(--space-10);
  }

  .section-head {
    flex-direction: column;
  }

  .invite-issue {
    flex-wrap: wrap;
  }

  .invite-row__exp {
    display: none;
  }
}
</style>
