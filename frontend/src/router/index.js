import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', component: LoginView },
  { path: '/register', component: RegisterView },
  { path: '/experiment', component: () => import('@/views/ExperimentView.vue') },
  {
    path: '/admin',
    component: () => import('@/views/Admin/AdminLayoutView.vue'),
    // 最低角色层级：教师(20) 及以上可进入后台
    meta: { minLevel: 20 },
    children: [
      { path: '', redirect: '/admin/users' },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/Admin/UserManageView.vue')
      },
      {
        path: 'grades',
        name: 'AdminGrades',
        component: () => import('@/views/Admin/GradeView.vue')
      },
      {
        path: 'org',
        name: 'AdminOrg',
        component: () => import('@/views/Admin/OrgManageView.vue'),
        // 覆盖父路由的 minLevel：组织架构改动影响全局，后端门槛是管理员
        meta: { minLevel: 30 }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/Admin/ProfileView.vue')
      }
    ]
  },
  { path: '/HWT', component: () => import('@/views/HighVoltage/HWorkTicketView.vue') },
  { path: '/HTS', component: () => import('@/views/HighVoltage/HToolSelectionView.vue') },
  { path: '/HSO', component: () => import('@/views/HighVoltage/HSceneOverviewView.vue') },
  { path: '/HCL', component: () => import('@/views/HighVoltage/HCabinetLocalView.vue') },
  { path: '/LWT', component: () => import('@/views/LowVoltage/LWorkTicketView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫。
 *
 * 这是 UX 兜底，不是安全边界 —— 即便被绕过，后端 @RequireRole 仍会返回 403。
 * 守卫只决定「页面进不进得去」。
 */
router.beforeEach(async to => {
  const authStore = useAuthStore()

  // 刷新后 user/roles 是内存态会丢失，先恢复一次
  if (authStore.token && !authStore.roles.length) {
    try {
      await authStore.fetchMe()
    } catch {
      // token 失效或已过期
      authStore.clearAuth()
      return to.path === '/' ? true : { path: '/' }
    }
  }

  const minLevel = to.meta?.minLevel
  if (minLevel != null) {
    if (!authStore.token || authStore.maxLevel < minLevel) {
      return { path: '/' }
    }
  }

  return true
})

export default router
