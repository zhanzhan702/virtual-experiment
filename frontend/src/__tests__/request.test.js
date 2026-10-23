import { AxiosError } from 'axios'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import request from '@/utils/request'

let location
let dispatchEvent

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
  location = { pathname: '/experiment', href: '/experiment' }
  dispatchEvent = vi.fn()
  vi.stubGlobal('window', { location, dispatchEvent })
})
afterEach(() => vi.unstubAllGlobals())

function rejectWith(status) {
  return config =>
    Promise.reject(
      new AxiosError('请求失败', 'ERR_BAD_REQUEST', config, null, {
        status,
        data: { message: '请求失败' },
        config
      })
    )
}

it('请求使用 Cookie，旧 localStorage token 不会成为 Authorization 头', async () => {
  localStorage.setItem('token', 'old-token')
  const result = await request.get('/experiment/unfinished', {
    adapter: async config => {
      expect(config.withCredentials).toBe(true)
      expect(config.headers.get('Authorization')).toBeUndefined()
      return { status: 200, data: [], headers: {}, config }
    }
  })
  expect(result).toEqual([])
})

it.each(['/auth/login', '/auth/register', '/auth/me', '/auth/logout'])(
  '%s 的 401 交给调用者，不重定向',
  async url => {
    await expect(request.get(url, { adapter: rejectWith(401) })).rejects.toBeInstanceOf(AxiosError)
    expect(location.href).toBe('/experiment')
    expect(dispatchEvent).not.toHaveBeenCalled()
  }
)

it('业务 401 通知清理状态并跳转登录页', async () => {
  await expect(
    request.get('/experiment/unfinished', { adapter: rejectWith(401) })
  ).rejects.toBeTruthy()
  expect(location.href).toBe('/')
  expect(dispatchEvent.mock.calls[0][0].type).toBe('auth:unauthorized')
})

it('已在登录页时业务 401 不重复刷新', async () => {
  location.pathname = '/'
  location.href = 'unchanged'
  await expect(
    request.get('/experiment/unfinished', { adapter: rejectWith(401) })
  ).rejects.toBeTruthy()
  expect(location.href).toBe('unchanged')
})

it.each([403, 500, 503])('%s 不清理身份或跳转', async status => {
  await expect(
    request.get('/experiment/unfinished', { adapter: rejectWith(status) })
  ).rejects.toBeTruthy()
  expect(location.href).toBe('/experiment')
  expect(dispatchEvent).not.toHaveBeenCalled()
})
