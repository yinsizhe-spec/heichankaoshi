import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('camera_admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error.response?.data?.error?.message ||
      error.response?.data?.message ||
      error.message ||
      '请求失败'

    if (error.response?.status === 401) {
      localStorage.removeItem('camera_admin_token')
      localStorage.removeItem('camera_admin_user')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
