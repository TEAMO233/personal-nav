<script setup lang="ts">
/**
 * 搜索栏:左侧引擎下拉(图标随选中变化) + 关键词输入 + 搜索按钮。
 * 按当前引擎的 URL 模板把 {query} 替换为编码后的关键词,新标签打开。
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useEngineStore } from '@/stores/engine'
import EngineIcon from './EngineIcon.vue'
import AppIcon from './AppIcon.vue'

const engineStore = useEngineStore()

const keyword = ref('')
const selectedId = ref<string | null>(null)
const dropdownOpen = ref(false)
const inputEl = ref<HTMLInputElement | null>(null)

// 当前选中引擎:本地选择优先,兜底默认引擎
const selected = computed(
  () => engineStore.engines.find((e) => e.id === selectedId.value) ?? engineStore.defaultEngine,
)

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
 * 执行搜索:用引擎模板替换 {query} 后新标签打开。
 */
function doSearch(): void {
  // 1. 关键词与引擎都需就绪
  const kw = keyword.value.trim()
  const eng = selected.value
  if (!kw || !eng) return
  // 2. 替换占位并编码,新标签打开(noopener 防来源页被操控)
  const url = eng.urlTemplate.split('{query}').join(encodeURIComponent(kw))
  window.open(url, '_blank', 'noopener')
}

// 进入首页自动聚焦输入框
onMounted(() => inputEl.value?.focus())
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
        @click="dropdownOpen = !dropdownOpen"
      >
        <EngineIcon v-if="selected" :engine="selected" :size="26" />
        <AppIcon name="chevron-down" :size="16" class="engine-caret" />
      </button>

      <!-- 下拉:遮罩点击关闭 -->
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
      autocomplete="off"
      :placeholder="selected ? `用 ${selected.name} 搜索` : '搜索'"
      @focus="dropdownOpen = false"
    />

    <!-- 搜索按钮 -->
    <button
      type="submit"
      class="search-go"
      :disabled="!keyword.trim() || !selected"
      aria-label="搜索"
    >
      <AppIcon name="search" :size="20" />
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
  max-width: 640px;
  height: 60px;
  padding: 0 var(--space-2) 0 var(--space-3);
  background: var(--bg-elevated);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-card);
  transition: box-shadow var(--duration-fast) var(--ease-default);
}

.search-bar:focus-within {
  box-shadow: var(--shadow-card), 0 0 0 4px color-mix(in srgb, var(--system-blue) 18%, transparent);
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
  padding: 0 var(--space-2);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.engine-trigger:hover:not(:disabled) {
  background: var(--bg-secondary);
}

.engine-trigger:disabled {
  cursor: default;
  opacity: 0.5;
}

.engine-caret {
  color: var(--label-tertiary);
}

.search-divider {
  flex-shrink: 0;
  width: 1px;
  height: 26px;
  background: var(--separator);
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  font-family: inherit;
  font-size: var(--text-title3);
  color: var(--label-primary);
  background: transparent;
  border: none;
  outline: none;
}

.search-input::placeholder {
  color: var(--label-tertiary);
}

.search-go {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #ffffff;
  background: var(--system-blue);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
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
  z-index: 10;
}

.engine-menu {
  position: absolute;
  top: calc(100% + var(--space-2));
  left: 0;
  z-index: 11;
  min-width: 220px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
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
  background: var(--bg-secondary);
}

.engine-option--active {
  color: var(--system-blue);
  font-weight: 600;
}

.engine-option__name {
  flex: 1;
}

/* 移动端:搜索框略矮 */
@media (max-width: 640px) {
  .search-bar {
    height: 54px;
  }

  .search-input {
    font-size: var(--text-body);
  }
}
</style>
