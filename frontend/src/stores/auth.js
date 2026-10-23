import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  getCurrentUser
} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 升级后不再保留可被 JavaScript 读取的旧凭证。
  localStorage.removeItem('token')
  const user = ref(null)
  const roles = ref([])
  const maxLevel = ref(0)
  const initialized = ref(false)
  let restoration = null
  let revision = 0

  const isLoggedIn = computed(() => !!user.value)
  const isStudent = computed(() => roles.value.includes('student'))
  const isTeacher = computed(() => roles.value.includes('teacher'))
  const isAdmin = computed(() => roles.value.includes('admin'))

  /** 写入服务端返回的用户和权限快照 */
  function applyUser(res) {
    user.value = res.user
    roles.value = res.roles || []
    maxLevel.value = res.maxLevel ?? res.user?.maxLevel ?? 0
    initialized.value = true
  }

  /** 登录，凭证仅由浏览器接收和保存 */
  async function login(loginDTO) {
    revision++
    restoration = null
    try {
      const res = await loginApi(loginDTO)
      applyUser(res)
      return res
    } catch (error) {
      if (error.response?.status === 401) clearAuth()
      throw error
    }
  }

  /** 注册 */
  async function register(registerDTO) {
    return await registerApi(registerDTO)
  }

  /** 恢复登录态，合并并发请求；忽略退出或重新登录前发起的旧响应 */
  function fetchMe() {
    if (restoration) return restoration
    const currentRevision = revision
    const pending = getCurrentUser()
      .then(res => {
        if (currentRevision === revision) applyUser(res)
        return res
      })
      .catch(error => {
        if (currentRevision === revision && error.response?.status === 401) clearAuth()
        throw error
      })
      .finally(() => {
        if (restoration === pending) restoration = null
      })
    restoration = pending
    return pending
  }

  /** 首次导航恢复一次；临时网络故障不标记为已完成，下次导航可重试 */
  async function ensureInitialized() {
    if (initialized.value) return
    try {
      await fetchMe()
    } catch (error) {
      if (error.response?.status !== 401) throw error
    }
  }

  /** 更新资料后保留已有的最高角色层级 */
  function setUser(next) {
    user.value = next ? { ...next, maxLevel: next.maxLevel ?? maxLevel.value } : null
  }

  /** 清空本地状态；Cookie 必须由后端清除 */
  function clearAuth() {
    revision++
    restoration = null
    user.value = null
    roles.value = []
    maxLevel.value = 0
    initialized.value = true
    localStorage.removeItem('token')
  }

  /** 后端退出成功后才清理本地状态，失败时保留状态供重试 */
  async function logout() {
    await logoutApi()
    clearAuth()
  }

  window.addEventListener('auth:unauthorized', clearAuth)
  onScopeDispose(() => window.removeEventListener('auth:unauthorized', clearAuth))

  return {
    user,
    roles,
    maxLevel,
    initialized,
    isLoggedIn,
    isStudent,
    isTeacher,
    isAdmin,
    login,
    register,
    fetchMe,
    ensureInitialized,
    setUser,
    clearAuth,
    logout
  }
})
