import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('camera_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const data = error.response?.data
    const message = data?.error?.message || data?.message || error.message || '网络连接异常，请检查网络'

    if (status === 401) {
      localStorage.removeItem('camera_token')
      localStorage.removeItem('camera_user')
      window.location.href = '/login'
    }
    return Promise.reject(new Error(message))
  }
)

export default request
