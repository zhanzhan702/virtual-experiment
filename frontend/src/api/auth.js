import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 学生注册 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 当前登录用户信息
 * 刷新页面后 store 的 user/roles 丢失，靠本接口恢复（否则路由守卫会把人踢回登录页）
 */
export function getCurrentUser() {
  return request.get('/auth/me')
}
