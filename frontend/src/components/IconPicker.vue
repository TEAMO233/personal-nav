<script setup lang="ts">
/**
 * 图标选择器:三来源(上传本地图片 / 图片外链 / 抓取站点 favicon)选一张图作图标。
 * 通过 v-model 绑定媒体 id(iconAssetId),引擎与快捷方式共用。
 */
import { ref, computed } from 'vue'
import { uploadImage, saveImageFromUrl, fetchFavicon, mediaUrl } from '@/api/media'
import { ApiClientError } from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'

const props = withDefaults(
  defineProps<{
    /** 当前图标的媒体 id(v-model),无图标为 null */
    modelValue: string | null
    /** 关联站点地址,用作"抓取 favicon"输入框默认值,方便一键抓取 */
    siteUrl?: string
  }>(),
  { siteUrl: '' },
)

const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()

// 当前展开的输入来源:'' 收起 / 'url' 图片外链 / 'favicon' 站点图标
const activeSource = ref<'' | 'url' | 'favicon'>('')
const urlInput = ref('')
const faviconInput = ref('')
const loading = ref(false)
// 隐藏的文件选择框
const fileInput = ref<HTMLInputElement>()

// 当前图标预览地址:有 id 则走后端读取接口
const previewSrc = computed(() => (props.modelValue ? mediaUrl(props.modelValue) : null))

/**
 * 统一执行一次图标保存,成功后回填 id 并收起输入。
 *
 * @param task 实际的保存请求(上传 / 外链 / favicon)
 */
async function runSave(task: () => Promise<{ id: string }>): Promise<void> {
  // 1. 进入加载态
  loading.value = true
  try {
    // 2. 执行保存并把返回的媒体 id 回传给父组件
    const asset = await task()
    emit('update:modelValue', asset.id)
    // 3. 收起输入并清空外链框
    activeSource.value = ''
    urlInput.value = ''
  } catch (e) {
    // 4. 失败弹提示(SSRF / 超时 / 类型不符等由后端返回文案)
    ElMessage.error(e instanceof ApiClientError ? e.message : '保存图标失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开系统文件选择框。
 */
function pickFile(): void {
  // 1. 上传无需输入框,先收起再打开选择
  activeSource.value = ''
  fileInput.value?.click()
}

/**
 * 选中本地文件后上传。
 *
 * @param e 文件选择事件
 */
function onFileChange(e: Event): void {
  // 1. 取选中的第一个文件
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  // 2. 上传;完成后清空 input 以便能再次选同一文件
  void runSave(() => uploadImage(file)).finally(() => {
    input.value = ''
  })
}

/**
 * 切换某个输入来源的展开 / 收起。
 *
 * @param source 来源类型
 */
function toggleSource(source: 'url' | 'favicon'): void {
  // 1. 再次点击同一来源则收起
  if (activeSource.value === source) {
    activeSource.value = ''
    return
  }
  // 2. 展开目标来源;favicon 预填关联站点地址
  activeSource.value = source
  if (source === 'favicon' && !faviconInput.value) faviconInput.value = props.siteUrl
}

/**
 * 确认图片外链。
 */
function confirmUrl(): void {
  // 1. 空地址不处理
  const url = urlInput.value.trim()
  if (!url) return
  void runSave(() => saveImageFromUrl(url))
}

/**
 * 确认抓取站点 favicon。
 */
function confirmFavicon(): void {
  // 1. 空地址不处理
  const url = faviconInput.value.trim()
  if (!url) return
  void runSave(() => fetchFavicon(url))
}

/**
 * 清除当前图标。
 */
function clearIcon(): void {
  // 1. 回传 null 并收起输入
  emit('update:modelValue', null)
  activeSource.value = ''
}
</script>

<template>
  <div class="icon-picker">
    <!-- 预览 + 来源按钮 -->
    <div class="icon-picker__row">
      <!-- 预览方块:有图标显示图,否则占位 -->
      <div class="icon-picker__preview">
        <img v-if="previewSrc" :src="previewSrc" alt="图标预览" />
        <AppIcon v-else name="image" :size="22" />
        <!-- 加载遮罩 -->
        <span v-if="loading" class="icon-picker__spinner"></span>
      </div>

      <!-- 三来源 + 清除按钮 -->
      <div class="icon-picker__sources">
        <button type="button" class="src-btn" :disabled="loading" @click="pickFile">
          <AppIcon name="upload" :size="16" /><span>上传</span>
        </button>
        <button
          type="button"
          class="src-btn"
          :class="{ 'src-btn--active': activeSource === 'url' }"
          :disabled="loading"
          @click="toggleSource('url')"
        >
          <AppIcon name="link" :size="16" /><span>图片链接</span>
        </button>
        <button
          type="button"
          class="src-btn"
          :class="{ 'src-btn--active': activeSource === 'favicon' }"
          :disabled="loading"
          @click="toggleSource('favicon')"
        >
          <AppIcon name="image" :size="16" /><span>网站图标</span>
        </button>
        <!-- 有图标时才显示清除 -->
        <button
          v-if="modelValue"
          type="button"
          class="src-btn src-btn--clear"
          :disabled="loading"
          @click="clearIcon"
        >
          <AppIcon name="close" :size="16" /><span>清除</span>
        </button>
      </div>
    </div>

    <!-- 图片外链输入 -->
    <div v-if="activeSource === 'url'" class="icon-picker__input">
      <input
        v-model="urlInput"
        type="url"
        class="picker-field"
        placeholder="粘贴图片地址,如 https://.../logo.png"
        @keyup.enter="confirmUrl"
      />
      <button type="button" class="confirm-btn" :disabled="loading" @click="confirmUrl">确定</button>
    </div>

    <!-- 抓取 favicon 输入 -->
    <div v-if="activeSource === 'favicon'" class="icon-picker__input">
      <input
        v-model="faviconInput"
        type="url"
        class="picker-field"
        placeholder="填站点地址,自动抓取其图标"
        @keyup.enter="confirmFavicon"
      />
      <button type="button" class="confirm-btn" :disabled="loading" @click="confirmFavicon">
        抓取
      </button>
    </div>

    <!-- 隐藏的文件选择框 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="icon-picker__file"
      @change="onFileChange"
    />
  </div>
</template>

<style scoped>
.icon-picker {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.icon-picker__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/* 预览方块 */
.icon-picker__preview {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  overflow: hidden;
  color: var(--label-tertiary);
  background: var(--bg-secondary);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-md);
}

.icon-picker__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 加载转圈 */
.icon-picker__spinner {
  position: absolute;
  inset: 0;
  width: 18px;
  height: 18px;
  margin: auto;
  border: 2px solid var(--label-quaternary);
  border-top-color: var(--system-blue);
  border-radius: var(--radius-full);
  animation: icon-picker-spin 0.7s linear infinite;
}

@keyframes icon-picker-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 来源按钮组 */
.icon-picker__sources {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.src-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 32px;
  padding: 0 var(--space-3);
  font-size: var(--text-footnote);
  color: var(--label-primary);
  background: var(--bg-secondary);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.src-btn:hover:not(:disabled) {
  background: var(--bg-tertiary);
}

.src-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.src-btn--active {
  color: var(--system-blue);
  background: color-mix(in srgb, var(--system-blue) 12%, transparent);
  border-color: transparent;
}

.src-btn--clear {
  color: var(--system-red);
}

/* 外链 / favicon 输入行 */
.icon-picker__input {
  display: flex;
  gap: var(--space-2);
}

.picker-field {
  flex: 1;
  height: 36px;
  padding: 0 var(--space-3);
  font-size: var(--text-subhead);
  color: var(--label-primary);
  background: var(--bg-primary);
  border: 0.5px solid var(--separator);
  border-radius: var(--radius-md);
  outline: none;
}

.picker-field:focus {
  border-color: var(--system-blue);
}

.confirm-btn {
  height: 36px;
  padding: 0 var(--space-4);
  font-size: var(--text-subhead);
  font-weight: 600;
  color: #fff;
  background: var(--system-blue);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.icon-picker__file {
  display: none;
}
</style>
