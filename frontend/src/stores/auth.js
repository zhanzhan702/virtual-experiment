import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)
  const roles = ref([])

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
    localStorage.setItem('token', res.token)
    return res
  }

  /** 注册 */
  async function register(registerDTO) {
    return await registerApi(registerDTO)
  }

  /** 登出 */
  function logout() {
    token.value = ''
    user.value = null
    roles.value = []
    localStorage.removeItem('token')
  }

  return { token, user, roles, isLoggedIn, isStudent, isTeacher, isAdmin, login, register, logout }
})
