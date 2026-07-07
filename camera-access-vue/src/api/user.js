import request from '../utils/request'
import { mockUser, mockCameras, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function getCurrentUserApi() {
  if (useMock) return wait(mockUser)
  return request.get('/api/users/me')
}

export async function getProfileApi() {
  if (useMock) return wait({ ...mockUser, cameras: mockCameras })
  return request.get('/api/users/me')
}
