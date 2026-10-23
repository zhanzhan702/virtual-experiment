import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

/** 等待 Cookie 登录态恢复，再检查页面的登录和角色要求 */
export async function authGuard(to) {
  const authStore = useAuthStore()
  const isPublic = to.path === '/' || to.path === '/register'
  try {
    await authStore.ensureInitialized()
  } catch {
    ElMessage.error('登录状态恢复失败，请稍后重试')
    return isPublic
  }

  if (!isPublic && !authStore.isLoggedIn) return { path: '/' }
  if (to.meta?.minLevel != null && authStore.maxLevel < to.meta.minLevel) return { path: '/' }
  return true
}
