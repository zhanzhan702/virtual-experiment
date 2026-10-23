import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { createPinia, disposePinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '@/api/auth'
import { authGuard } from '@/utils/auth-guard'

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  getCurrentUser: vi.fn()
}))
let pinia

beforeEach(() => {
  vi.resetAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
})
afterEach(() => disposePinia(pinia))

it('守卫等待恢复后再检查管理员权限', async () => {
  let resolve
  getCurrentUser.mockImplementation(
    () =>
      new Promise(done => {
        resolve = done
      })
  )
  const navigation = authGuard({ path: '/admin/users', meta: { minLevel: 20 } })
  resolve({ user: { id: 'teacher-id', maxLevel: 20 }, roles: ['teacher'] })
  await expect(navigation).resolves.toBe(true)
})

it.each(['/experiment', '/HWT', '/LWT', '/admin/users'])('未登录访问 %s 返回登录页', async path => {
  getCurrentUser.mockRejectedValue({ response: { status: 401 } })
  await expect(authGuard({ path, meta: {} })).resolves.toEqual({ path: '/' })
})

it.each(['/', '/register'])('公开页面 %s 不因 me 的 401 产生循环', async path => {
  getCurrentUser.mockRejectedValue({ response: { status: 401 } })
  await expect(authGuard({ path, meta: {} })).resolves.toBe(true)
})

it('学生无法进入教师管理页面', async () => {
  getCurrentUser.mockResolvedValue({ user: { id: 'student-id', maxLevel: 10 }, roles: ['student'] })
  await expect(authGuard({ path: '/admin/users', meta: { minLevel: 20 } })).resolves.toEqual({
    path: '/'
  })
})

it('教师无法进入管理员组织页面', async () => {
  getCurrentUser.mockResolvedValue({ user: { id: 'teacher-id', maxLevel: 20 }, roles: ['teacher'] })
  await expect(authGuard({ path: '/admin/org', meta: { minLevel: 30 } })).resolves.toEqual({
    path: '/'
  })
})

it('恢复失败阻止受保护导航并提示错误，后续导航可重试', async () => {
  getCurrentUser
    .mockRejectedValueOnce(new Error('offline'))
    .mockResolvedValueOnce({ user: { id: 'student-id', maxLevel: 10 }, roles: ['student'] })
  const destination = { path: '/experiment', meta: {} }
  await expect(authGuard(destination)).resolves.toBe(false)
  expect(ElMessage.error).toHaveBeenCalledTimes(1)
  await expect(authGuard(destination)).resolves.toBe(true)
})

it('恢复故障仍允许进入登录页面', async () => {
  getCurrentUser.mockRejectedValue(new Error('offline'))
  await expect(authGuard({ path: '/', meta: {} })).resolves.toBe(true)
})
