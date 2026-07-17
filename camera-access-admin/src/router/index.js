import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardPage.vue'),
        meta: { title: '控制台', icon: 'DataBoard' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UserManagementPage.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'cameras',
        name: 'cameras',
        component: () => import('@/views/CameraManagementPage.vue'),
        meta: { title: '摄像头管理', icon: 'VideoCamera' }
      },
      {
        path: 'permissions',
        name: 'permissions',
        component: () => import('@/views/PermissionManagementPage.vue'),
        meta: { title: '访问权限', icon: 'Key' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  document.title = `${to.meta.title || '后台管理'} - 智慧考试系统`

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'login' && authStore.isLoggedIn) {
    return { name: 'dashboard' }
  }
})

export default router
