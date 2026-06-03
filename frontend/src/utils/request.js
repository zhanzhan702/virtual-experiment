import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
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
    console.error('请求失败:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default request
