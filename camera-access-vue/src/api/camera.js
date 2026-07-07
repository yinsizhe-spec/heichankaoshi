import request from '../utils/request'
import { mockCameras, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function getCameraListApi() {
  if (useMock) return wait(mockCameras)
  return request.get('/api/cameras')
}

export async function checkCameraAccessApi(cameraId) {
  if (useMock) {
    const camera = mockCameras.find((item) => item.cameraId === cameraId)
    if (!camera) throw new Error('摄像头不存在')
    return wait({
      allowed: camera.canAccessNow && camera.status === 'online',
      cameraId,
      serverTime: new Date().toISOString(),
      accessStartTime: camera.accessStartTime,
      accessEndTime: camera.accessEndTime,
      message: camera.canAccessNow ? 'Access allowed' : 'Current time is outside the allowed access period'
    })
  }
  return request.get(`/api/cameras/${cameraId}/access-check`)
}

export async function getCameraStreamApi(cameraId) {
  if (useMock) {
    return wait({
      cameraId,
      streamType: 'hls',
      streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
      expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString()
    })
  }
  return request.get(`/api/cameras/${cameraId}/stream`)
}
