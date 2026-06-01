import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 路由表:M0 先放首页占位,后续里程碑补登录/设置/后台页面
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
