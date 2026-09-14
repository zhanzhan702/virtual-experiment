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

/**
 * 自助改资料（姓名、性别、出生日期、手机号、邮箱）
 *
 * 学号、班级、用户名由教务或管理员确定，接口不接受这些字段。
 * 手机号 / 邮箱 / 出生日期传空字符串或 null 表示清空。
 *
 * @param {{name:string,gender?:string,birthday?:string|null,phone?:string,email?:string}} data
 * @returns {Promise<object>} 更新后的用户信息
 */
export function updateProfile(data) {
  return request.put('/auth/profile', data)
}
