import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 给路由 meta 增加类型:是否需要登录
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
  }
}

// 路由表:首页需登录,登录页公开;未知路径回首页
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 全局前置守卫:首次导航探测登录态,再按是否需要登录放行或跳转。
 */
router.beforeEach(async (to) => {
  // 1. 首次进入先探测登录态,顺带让后端下发 CSRF cookie
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.init()
  }
  // 2. 需要登录却未登录:去登录页并记下原地址
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 3. 已登录还去登录页:回首页
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'home' }
  }
  // 4. 其余放行
  return true
})

export default router
