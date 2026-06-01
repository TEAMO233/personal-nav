import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

// 1. 创建应用实例
const app = createApp(App)

// 2. 装载 Pinia(状态管理)、路由、Element Plus(组件库)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 3. 挂载到页面
app.mount('#app')
