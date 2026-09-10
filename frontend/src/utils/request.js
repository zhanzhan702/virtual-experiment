import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一返回 data 层 + 错误处理
request.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status

    // 401：token 缺失/过期/无效，清登录态并回登录页
    // 用 window.location 而非 import router —— router → views → api → request → router 会构成循环依赖
    if (status === 401) {
      localStorage.removeItem('token')
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
