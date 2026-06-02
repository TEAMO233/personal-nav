<script setup lang="ts">
/**
 * 通用图标组件:内联 SVG 线性图标(HIG 风格),用 currentColor 自适应主题与字号。
 * 零外部依赖,按 name 渲染对应图标。
 */

/** 支持的图标名 */
export type IconName =
  | 'search'
  | 'sun'
  | 'moon'
  | 'user'
  | 'logout'
  | 'settings'
  | 'chevron-down'
  | 'external-link'
  | 'plus'
  | 'edit'
  | 'trash'
  | 'grip'
  | 'arrow-up'
  | 'arrow-down'
  | 'arrow-left'
  | 'folder'
  | 'check'
  | 'upload'
  | 'link'
  | 'image'
  | 'close'
  | 'shield'
  | 'copy'

const props = withDefaults(defineProps<{ name: IconName; size?: number }>(), {
  size: 20,
})

// 各图标的 SVG 内部元素(24x24 视图,stroke 风格);硬编码常量,v-html 注入安全
const paths: Record<IconName, string> = {
  search:
    '<circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>',
  sun:
    '<circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>',
  moon: '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>',
  user:
    '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle>',
  logout:
    '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line>',
  settings:
    '<line x1="4" y1="21" x2="4" y2="14"></line><line x1="4" y1="10" x2="4" y2="3"></line><line x1="12" y1="21" x2="12" y2="12"></line><line x1="12" y1="8" x2="12" y2="3"></line><line x1="20" y1="21" x2="20" y2="16"></line><line x1="20" y1="12" x2="20" y2="3"></line><line x1="1" y1="14" x2="7" y2="14"></line><line x1="9" y1="8" x2="15" y2="8"></line><line x1="17" y1="16" x2="23" y2="16"></line>',
  'chevron-down': '<polyline points="6 9 12 15 18 9"></polyline>',
  'external-link':
    '<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line>',
  // 加号:新增引擎/分组/快捷方式
  plus: '<line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line>',
  // 铅笔:编辑
  edit:
    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4z"></path>',
  // 垃圾桶:删除
  trash:
    '<polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line>',
  // 六点手柄:拖拽排序(实心点,故覆盖 fill)
  grip:
    '<circle cx="9" cy="6" r="1.3" fill="currentColor" stroke="none"></circle><circle cx="9" cy="12" r="1.3" fill="currentColor" stroke="none"></circle><circle cx="9" cy="18" r="1.3" fill="currentColor" stroke="none"></circle><circle cx="15" cy="6" r="1.3" fill="currentColor" stroke="none"></circle><circle cx="15" cy="12" r="1.3" fill="currentColor" stroke="none"></circle><circle cx="15" cy="18" r="1.3" fill="currentColor" stroke="none"></circle>',
  // 上箭头:移动端上移
  'arrow-up': '<line x1="12" y1="19" x2="12" y2="5"></line><polyline points="5 12 12 5 19 12"></polyline>',
  // 下箭头:移动端下移
  'arrow-down': '<line x1="12" y1="5" x2="12" y2="19"></line><polyline points="19 12 12 19 5 12"></polyline>',
  // 左箭头:返回首页
  'arrow-left': '<line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline>',
  // 文件夹:移到分组
  folder:
    '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>',
  // 对勾:默认标记/确认
  check: '<polyline points="20 6 9 17 4 12"></polyline>',
  // 上传:本地图片上传
  upload:
    '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line>',
  // 链接:图片外链
  link:
    '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>',
  // 图片:抓站点 favicon / 图片占位
  image:
    '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline>',
  // 叉号:清除/关闭
  close: '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>',
  // 盾牌:管理后台入口
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>',
  // 复制:复制邀请码到剪贴板
  copy:
    '<rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>',
}
</script>

<template>
  <!-- 线性图标:fill 透明、stroke 用 currentColor -->
  <svg
    :width="props.size"
    :height="props.size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    v-html="paths[props.name]"
  ></svg>
</template>
