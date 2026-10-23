import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

const AUTH_PATHS = new Set(['/auth/login', '/auth/register', '/auth/me', '/auth/logout'])

// 响应拦截器：统一返回 data 层 + 错误处理
request.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status

    // 登录和恢复请求由调用方处理 401；业务接口失效时通知 store，再回登录页。
    // 使用事件避免 request → store → api → request 的循环依赖。
    if (status === 401 && !AUTH_PATHS.has(error.config?.url)) {
      window.dispatchEvent(new Event('auth:unauthorized'))
      // 已在登录页时不再跳转，避免刷新循环
      if (window.location.pathname !== '/') {
        window.location.href = '/'
      }
    }

    console.error('请求失败:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default request
