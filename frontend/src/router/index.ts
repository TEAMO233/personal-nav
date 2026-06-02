import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 给路由 meta 增加类型:是否需要登录 / 是否需要管理员
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
  }
}

// 路由表:首页/设置/后台需登录(后台另需 ADMIN),登录/注册公开;未知路径回首页
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
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
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
  // 3. 需要管理员却非管理员:挡回首页
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  // 4. 已登录还去登录/注册页:回首页
  if ((to.name === 'login' || to.name === 'register') && auth.isLoggedIn) {
    return { name: 'home' }
  }
  // 5. 其余放行
  return true
})

export default router
