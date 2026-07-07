import request from '../utils/request'
import { mockUser, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function loginApi(payload) {
  if (useMock) {
    if (payload.username && payload.password) {
      return wait({ token: 'mock-jwt-token', user: mockUser })
    }
    throw new Error('请输入用户名和密码')
  }
  return request.post('/api/auth/login', payload)
}
