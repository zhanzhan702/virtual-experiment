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

/**
 * 自助改密（改自己的密码，需提供原密码）
 * @param {{oldPassword:string,newPassword:string,confirmPassword:string}} data
 */
export function changePassword(data) {
  return request.put('/auth/password', data)
}
