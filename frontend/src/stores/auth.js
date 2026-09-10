import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)
  const roles = ref([])
  // 最高角色层级：学生 10 / 教师 20 / 管理员 30 / 超级管理员 40
  // 仅用于路由守卫与菜单显隐，真正的权限拦截在后端
  const maxLevel = ref(0)

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => roles.value.includes('student'))
  const isTeacher = computed(() => roles.value.includes('teacher'))
  const isAdmin = computed(() => roles.value.includes('admin'))

  /** 登录 */
  async function login(loginDTO) {
    const res = await loginApi(loginDTO)
    token.value = res.token
    user.value = res.user
    roles.value = res.roles || []
    maxLevel.value = res.user?.maxLevel ?? 0
    localStorage.setItem('token', res.token)
    return res
  }

  /** 注册 */
  async function register(registerDTO) {
    return await registerApi(registerDTO)
  }

  /**
   * 恢复登录态。
   *
   * token 存在但 user/roles 为空（页面刷新后的首次导航）时调用，从后端重新拉取。
   * token 失效时抛错，由路由守卫处理跳转。
   */
  async function fetchMe() {
    const res = await getCurrentUser()
    user.value = res.user
    roles.value = res.roles || []
    maxLevel.value = res.maxLevel ?? res.user?.maxLevel ?? 0
    return res
  }

  /** 清空本地登录态（不调后端，JWT 无状态） */
  function clearAuth() {
    token.value = ''
    user.value = null
    roles.value = []
    maxLevel.value = 0
    localStorage.removeItem('token')
  }

  /** 登出 */
  function logout() {
    clearAuth()
  }

  return {
    token,
    user,
    roles,
    maxLevel,
    isLoggedIn,
    isStudent,
    isTeacher,
    isAdmin,
    login,
    register,
    fetchMe,
    clearAuth,
    logout
  }
})
