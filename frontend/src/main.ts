import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useThemeStore } from '@/stores/theme'
// 全局样式与 HIG 设计令牌
import './style.css'

// 1. 创建应用实例
const app = createApp(App)

// 2. 装载 Pinia 与路由(M6 首页用原生 HIG 组件,不引入 Element Plus;EP 留待 M7 按需引入)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 3. 尽早实例化主题 store,启动即应用 html.dark,避免首屏闪烁
useThemeStore()

// 4. 挂载到页面
app.mount('#app')
