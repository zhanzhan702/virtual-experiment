import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, disposePinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser, login, logout } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  getCurrentUser: vi.fn()
}))

const session = {
  user: { id: 'student-id', username: 'student1', maxLevel: 10 },
  roles: ['student'],
  redirectUrl: '/experiment'
}
const unauthorized = { response: { status: 401 } }
let pinia

beforeEach(() => {
  vi.resetAllMocks()
  localStorage.clear()
  pinia = createPinia()
  setActivePinia(pinia)
})

afterEach(() => disposePinia(pinia))

describe('Cookie 登录状态', () => {
  it('升级时删除旧 token，保留实验存档', () => {
    localStorage.setItem('token', 'legacy-secret')
    localStorage.setItem('experimentSteps_1', '[]')
    const store = useAuthStore()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('experimentSteps_1')).toBe('[]')
    expect(store).not.toHaveProperty('token')
    expect(store.isLoggedIn).toBe(false)
  })

  it('登录无需响应 token 即可更新用户和角色，不持久化凭证', async () => {
    login.mockResolvedValue(session)
    const store = useAuthStore()
    await store.login({ username: 'student1', password: 'student123' })
    expect(store.isLoggedIn).toBe(true)
    expect(store.isStudent).toBe(true)
    expect(store.maxLevel).toBe(10)
    expect(store.initialized).toBe(true)
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('登录失败清空旧身份并保留错误供表单显示', async () => {
    login.mockResolvedValueOnce(session).mockRejectedValueOnce(unauthorized)
    const store = useAuthStore()
    await store.login({})
    await expect(store.login({})).rejects.toBe(unauthorized)
    expect(store.isLoggedIn).toBe(false)
    expect(store.roles).toEqual([])
  })

  it('刷新后通过 me 恢复，并合并并发请求', async () => {
    let resolve
    getCurrentUser.mockImplementation(
      () =>
        new Promise(done => {
          resolve = done
        })
    )
    const store = useAuthStore()
    const first = store.ensureInitialized()
    const second = store.ensureInitialized()
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
    resolve({ ...session, maxLevel: 20, roles: ['teacher'] })
    await Promise.all([first, second])
    expect(store.isLoggedIn).toBe(true)
    expect(store.isTeacher).toBe(true)
    expect(store.maxLevel).toBe(20)
    await store.ensureInitialized()
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it('me 返回 401 后记录未登录状态，后续导航不重复恢复', async () => {
    getCurrentUser.mockRejectedValue(unauthorized)
    const store = useAuthStore()
    await store.ensureInitialized()
    await store.ensureInitialized()
    expect(store.initialized).toBe(true)
    expect(store.isLoggedIn).toBe(false)
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it.each([new Error('Network Error'), { response: { status: 503 } }])(
    '临时故障不标记恢复完成，可重试：%j',
    async error => {
      getCurrentUser.mockRejectedValueOnce(error).mockResolvedValueOnce(session)
      const store = useAuthStore()
      await expect(store.ensureInitialized()).rejects.toBe(error)
      expect(store.initialized).toBe(false)
      await store.ensureInitialized()
      expect(store.isLoggedIn).toBe(true)
      expect(getCurrentUser).toHaveBeenCalledTimes(2)
    }
  )

  it('刷新用户时发生网络错误不会清除现有身份', async () => {
    login.mockResolvedValue(session)
    getCurrentUser.mockRejectedValue(new Error('Network Error'))
    const store = useAuthStore()
    await store.login({})
    await expect(store.fetchMe()).rejects.toThrow('Network Error')
    expect(store.isLoggedIn).toBe(true)
  })

  it('退出请求成功前保留状态，成功后清空', async () => {
    login.mockResolvedValue(session)
    let resolve
    logout.mockImplementation(
      () =>
        new Promise(done => {
          resolve = done
        })
    )
    const store = useAuthStore()
    await store.login({})
    const pending = store.logout()
    expect(store.isLoggedIn).toBe(true)
    resolve({ message: '已退出登录' })
    await pending
    expect(logout).toHaveBeenCalledTimes(1)
    expect(store.isLoggedIn).toBe(false)
    expect(store.maxLevel).toBe(0)
  })

  it('退出失败时保留状态，允许再次退出', async () => {
    login.mockResolvedValue(session)
    logout.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({})
    const store = useAuthStore()
    await store.login({})
    await expect(store.logout()).rejects.toThrow('offline')
    expect(store.isLoggedIn).toBe(true)
    await store.logout()
    expect(store.isLoggedIn).toBe(false)
  })

  it('退出后迟到的 me 响应不能恢复旧身份', async () => {
    let resolve
    getCurrentUser.mockImplementation(
      () =>
        new Promise(done => {
          resolve = done
        })
    )
    logout.mockResolvedValue({})
    const store = useAuthStore()
    const pending = store.fetchMe()
    await store.logout()
    resolve(session)
    await pending
    expect(store.isLoggedIn).toBe(false)
  })

  it('旧 me 请求的 401 不得清空重新登录后的身份', async () => {
    let reject
    getCurrentUser.mockImplementation(
      () =>
        new Promise((resolve, fail) => {
          reject = fail
        })
    )
    login.mockResolvedValue(session)
    const store = useAuthStore()
    const pending = store.fetchMe()
    await store.login({})
    reject(unauthorized)
    await expect(pending).rejects.toBe(unauthorized)
    expect(store.isLoggedIn).toBe(true)
  })

  it('受保护接口的失效事件清空内存身份', async () => {
    login.mockResolvedValue(session)
    const store = useAuthStore()
    await store.login({})
    window.dispatchEvent(new Event('auth:unauthorized'))
    expect(store.isLoggedIn).toBe(false)
    expect(store.roles).toEqual([])
  })

  it('修改资料保留原有角色层级', async () => {
    login.mockResolvedValue(session)
    const store = useAuthStore()
    await store.login({})
    store.setUser({ id: 'student-id', name: '新姓名' })
    expect(store.user.maxLevel).toBe(10)
  })
})
