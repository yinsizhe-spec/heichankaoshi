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
      streamType: 'iframe',
      streamUrl: 'http://64.176.57.254:8889/live/desktop/',
      expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString()
    })
  }

  return request.get(`/api/cameras/${cameraId}/stream`)
}


export async function analyzeCameraSnapshotApi(cameraId, imageData) {
  if (useMock) {
    return wait({
      cameraId,
      capturedAt: new Date().toISOString(),
      summary: 'AI 分析完成：画面中未发现明显异常，摄像头画面清晰，入口区域状态正常。',
      riskLevel: 'low',
      confidence: 0.92,
      findings: [
        '画面亮度正常，可以识别主要场景',
        '未检测到人员聚集或明显危险行为',
        '未检测到烟雾、火光或遮挡摄像头的情况'
      ],
      suggestion: '建议继续保持当前摄像头角度，并定期检查夜间补光效果。'
    })
  }
  return request.post(`/api/cameras/${cameraId}/analysis`, { image: imageData })
}
