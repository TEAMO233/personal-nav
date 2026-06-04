<script setup lang="ts">
/**
 * 搜索栏:左侧引擎下拉(图标随选中变化) + 关键词输入 + 搜索按钮。
 * 输入框聚焦时展示搜索历史(可点击直接搜、单条删除、一键清空)。
 * 按当前引擎的 URL 模板把 {query} 替换为编码后的关键词,新标签打开。
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useEngineStore } from '@/stores/engine'
import { useSearchHistoryStore } from '@/stores/searchHistory'
import EngineIcon from './EngineIcon.vue'
import AppIcon from './AppIcon.vue'

const engineStore = useEngineStore()
const searchHistoryStore = useSearchHistoryStore()

const keyword = ref('')
const selectedId = ref<string | null>(null)
const dropdownOpen = ref(false)
const historyOpen = ref(false)
const inputEl = ref<HTMLInputElement | null>(null)

// 当前选中引擎:本地选择优先,兜底默认引擎
const selected = computed(
  () => engineStore.engines.find((e) => e.id === selectedId.value) ?? engineStore.defaultEngine,
)

// 历史下拉展示的条目:有输入时按「包含」过滤,空输入显示最近全部
const historyMatches = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const all = searchHistoryStore.items
  if (!kw) return all
  return all.filter((h) => h.keyword.toLowerCase().includes(kw))
})

// 引擎加载后,若用户还没手动选,跟随默认引擎
watch(
  () => engineStore.defaultEngine,
  (def) => {
    if (!selectedId.value && def) selectedId.value = def.id
  },
  { immediate: true },
)

/**
 * 选中某引擎并收起下拉,焦点回到输入框。
 *
 * @param id 引擎 id
 */
function selectEngine(id: string): void {
  // 1. 记录选择并关闭下拉
  selectedId.value = id
  dropdownOpen.value = false
  inputEl.value?.focus()
}

/**
 * 切换引擎下拉,同时收起历史下拉(两者互斥)。
 */
function toggleEngineDropdown(): void {
  // 1. 开关引擎下拉并关掉历史下拉
  dropdownOpen.value = !dropdownOpen.value
  historyOpen.value = false
}

/**
 * 执行搜索:用引擎模板替换 {query} 后新标签打开,并记录搜索历史。
 */
function doSearch(): void {
  // 1. 关键词与引擎都需就绪
  const kw = keyword.value.trim()
  const eng = selected.value
  if (!kw || !eng) return
  // 2. 替换占位并编码,新标签打开(noopener 防来源页被操控)
  const url = eng.urlTemplate.split('{query}').join(encodeURIComponent(kw))
  window.open(url, '_blank', 'noopener')
  // 3. 记录搜索历史(失败静默,不阻塞跳转),收起历史下拉
  searchHistoryStore.record(kw)
  historyOpen.value = false
}

/**
 * 点击历史项:填入关键词并直接用当前引擎搜索。
 *
 * @param kw 历史关键词
 */
function searchKeyword(kw: string): void {
  // 1. 填入并搜索
  keyword.value = kw
  doSearch()
}

/**
 * 删除一条搜索历史(失败静默)。
 *
 * @param id 记录 id
 */
async function removeHistory(id: string): Promise<void> {
  // 1. 删除该条;失败忽略,不打扰用户
  try {
    await searchHistoryStore.remove(id)
  } catch {
    // 删除失败静默
  }
}

/**
 * 清空全部搜索历史并收起下拉(失败静默)。
 */
async function clearHistory(): Promise<void> {
  // 1. 清空全部;失败忽略
  try {
    await searchHistoryStore.clear()
  } catch {
    // 清空失败静默
  }
  historyOpen.value = false
}

/**
 * 输入框聚焦:展示历史下拉,收起引擎下拉。
 */
function onInputFocus(): void {
  // 1. 开历史下拉、关引擎下拉
  dropdownOpen.value = false
  historyOpen.value = true
}

/**
 * 输入框失焦:延迟收起历史下拉,留时间给历史项的点击。
 */
function onInputBlur(): void {
  // 1. 延迟关闭(历史项已用 mousedown.prevent 兜底防抢先关闭)
  window.setTimeout(() => {
    historyOpen.value = false
  }, 150)
}

/**
 * 全局快捷键:Cmd/Ctrl + K 聚焦搜索框。
 *
 * @param e 键盘事件
 */
function onGlobalKeydown(e: KeyboardEvent): void {
  // 1. 命中 Cmd/Ctrl + K:阻止浏览器默认并聚焦输入框
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    inputEl.value?.focus()
  }
}

// 进入首页注册全局快捷键;不自动聚焦,避免首屏弹出历史下拉遮挡内容
onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
})

// 离开页面时解绑,避免在其他页面继续拦截 Cmd/Ctrl+K
onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
})
</script>

<template>
  <form class="search-bar" @submit.prevent="doSearch">
    <!-- 引擎选择器 -->
    <div class="engine-select">
      <button
        type="button"
        class="engine-trigger"
        :disabled="!selected"
        :aria-label="selected ? `当前引擎 ${selected.name},点击切换` : '无可用引擎'"
        @click="toggleEngineDropdown"
      >
        <AppIcon name="search" :size="26" />
      </button>

      <!-- 引擎下拉:遮罩点击关闭 -->
      <template v-if="dropdownOpen">
        <div class="dropdown-backdrop" @click="dropdownOpen = false"></div>
        <ul class="engine-menu" role="listbox">
          <li v-for="e in engineStore.engines" :key="e.id">
            <button
              type="button"
              class="engine-option"
              :class="{ 'engine-option--active': selected && e.id === selected.id }"
              @click="selectEngine(e.id)"
            >
              <EngineIcon :engine="e" :size="22" />
              <span class="engine-option__name">{{ e.name }}</span>
            </button>
          </li>
        </ul>
      </template>
    </div>

    <!-- 分隔线 -->
    <span class="search-divider" aria-hidden="true"></span>

    <!-- 关键词输入 -->
    <input
      ref="inputEl"
      v-model="keyword"
      class="search-input"
      type="text"
      name="personal-nav-search"
      autocomplete="new-password"
      autocapitalize="off"
      spellcheck="false"
      placeholder="搜索常用网站、工具或内容"
      @focus="onInputFocus"
      @blur="onInputBlur"
    />

    <!-- 搜索历史下拉 -->
    <ul v-if="historyOpen && historyMatches.length" class="history-menu" role="listbox">
      <li v-for="h in historyMatches" :key="h.id" class="history-item">
        <!-- mousedown.prevent 防输入框失焦抢先关闭下拉 -->
        <button type="button" class="history-pick" @mousedown.prevent @click="searchKeyword(h.keyword)">
          <AppIcon name="clock" :size="16" class="history-icon" />
          <span class="history-kw">{{ h.keyword }}</span>
        </button>
        <button
          type="button"
          class="history-del"
          aria-label="删除该条历史"
          @mousedown.prevent
          @click.stop="removeHistory(h.id)"
        >
          <AppIcon name="close" :size="14" />
        </button>
      </li>
      <!-- 清空全部 -->
      <li class="history-foot">
        <button type="button" class="history-clear" @mousedown.prevent @click="clearHistory">
          <AppIcon name="trash" :size="14" />
          <span>清除全部历史</span>
        </button>
      </li>
    </ul>

    <!-- 快捷键提示 -->
    <kbd class="search-kbd" aria-hidden="true">⌘K</kbd>

    <!-- 搜索按钮 -->
    <button type="submit" class="search-go" :disabled="!keyword.trim() || !selected" aria-label="搜索">
      <AppIcon name="arrow-right" :size="24" />
    </button>
  </form>
</template>

<style scoped>
.search-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  box-sizing: border-box;
  width: 100%;
  max-width: 800px;
  height: 64px;
  padding: 0 8px 0 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(160, 190, 255, 0.34);
  border-radius: var(--radius-full);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16), 0 20px 60px rgba(30, 100, 255, 0.22);
  transition: box-shadow var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default);
}

.search-bar:focus-within {
  border-color: color-mix(in srgb, var(--system-blue) 55%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 20px 60px rgba(30, 100, 255, 0.28),
    0 0 0 4px rgba(59, 130, 246, 0.18);
}

.engine-select {
  display: flex;
  align-items: center;
}

.engine-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 44px;
  width: 44px;
  padding: 0;
  color: rgba(235, 242, 255, 0.92);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.engine-trigger:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
}

.engine-trigger:disabled {
  cursor: default;
  opacity: 0.5;
}

.engine-caret {
  color: var(--label-tertiary);
}

.search-divider { display: none; }

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  font-family: inherit;
  font-size: 17px;
  font-weight: 550;
  color: rgba(255, 255, 255, 0.92);
  background: transparent;
  border: none;
  outline: none;
}

.search-input::placeholder {
  color: rgba(220, 230, 255, 0.46);
}

.search-kbd {
  flex-shrink: 0;
  padding: 5px 9px;
  font-family: var(--font-system);
  font-size: var(--text-caption1);
  font-weight: 600;
  letter-spacing: 0.5px;
  color: rgba(235, 242, 255, 0.86);
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-full);
}

.search-go {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #ffffff;
  background: linear-gradient(135deg, #5ea0ff, #2563eb);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.4);
  transition: opacity var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
}

.search-go:hover:not(:disabled) {
  opacity: 0.9;
}

.search-go:active:not(:disabled) {
  transform: scale(0.96);
}

.search-go:disabled {
  cursor: default;
  opacity: 0.4;
}

/* 下拉遮罩:铺满视口,点击即收起 */
.dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
}

.engine-menu {
  position: absolute;
  top: calc(100% + var(--space-2));
  left: 0;
  z-index: 100;
  min-width: 220px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  background: rgba(11, 28, 58, 0.9);
  border: 1px solid rgba(178, 203, 255, 0.18);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 22px 55px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(22px) saturate(170%);
  -webkit-backdrop-filter: blur(22px) saturate(170%);
}

.engine-option {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-subhead);
  color: var(--label-primary);
  text-align: left;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.engine-option:hover {
  background: rgba(255, 255, 255, 0.08);
}

.engine-option--active {
  color: var(--system-blue);
  font-weight: 600;
}

.engine-option__name {
  flex: 1;
}

/* 搜索历史下拉:宽度与搜索框一致 */
.history-menu {
  position: absolute;
  top: calc(100% + var(--space-2));
  left: 0;
  right: 0;
  z-index: 100;
  max-height: 360px;
  margin: 0;
  padding: var(--space-1);
  overflow-y: auto;
  list-style: none;
  background: rgba(11, 28, 58, 0.9);
  border: 1px solid rgba(178, 203, 255, 0.18);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 22px 55px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(22px) saturate(170%);
  -webkit-backdrop-filter: blur(22px) saturate(170%);
}

.history-item {
  display: flex;
  align-items: center;
}

.history-pick {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-subhead);
  color: var(--label-primary);
  text-align: left;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.history-pick:hover {
  background: rgba(255, 255, 255, 0.08);
}

.history-icon {
  flex-shrink: 0;
  color: var(--label-tertiary);
}

.history-kw {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-del {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--label-tertiary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.history-del:hover {
  color: var(--system-red, #ff3b30);
  background: var(--bg-secondary);
}

.history-foot {
  margin-top: var(--space-1);
  border-top: 0.5px solid rgba(178, 203, 255, 0.14);
}

.history-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2);
  font-size: var(--text-footnote);
  color: var(--label-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.history-clear:hover {
  color: var(--system-red, #ff3b30);
  background: rgba(255, 255, 255, 0.08);
}

/* 移动端:搜索框略矮 */
@media (max-width: 640px) {
  .search-bar {
    height: 54px;
  }

  .search-input {
    font-size: var(--text-body);
  }

  /* 小屏隐藏快捷键提示 */
  .search-kbd {
    display: none;
  }
}
</style>
