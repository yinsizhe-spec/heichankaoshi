import request from '../utils/request'
import { mockCameras, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

/**
 * 统一摄像头字段格式。
 *
 * 后端返回：
 * status: ONLINE
 * streamType: IFRAME
 *
 * 前端页面原本使用：
 * status: online
 * streamType: iframe
 */
function normalizeCamera(camera) {
  return {
    ...camera,

    cameraId: camera.cameraId || camera.id || '',

    cameraName:
      camera.cameraName ||
      camera.name ||
      '未命名摄像头',

    location: camera.location || '未知位置',

    status: String(
      camera.status || 'OFFLINE'
    ).toLowerCase(),

    streamType: String(
      camera.streamType || ''
    ).toLowerCase(),

    canAccessNow: Boolean(
      camera.canAccessNow
    ),

    accessStartTime:
      camera.accessStartTime || '',

    accessEndTime:
      camera.accessEndTime || '',

    accessMessage:
      camera.accessMessage || ''
  }
}

/**
 * 获取当前用户的摄像头列表。
 */
export async function getCameraListApi() {
  if (useMock) {
    return wait(
      mockCameras.map(normalizeCamera)
    )
  }

  const result = await request.get(
    '/api/cameras'
  )

  if (!Array.isArray(result)) {
    throw new Error(
      '摄像头列表返回格式不正确'
    )
  }

  return result.map(normalizeCamera)
}

/**
 * 校验当前用户是否可以访问指定摄像头。
 */
export async function checkCameraAccessApi(
  cameraId
) {
  if (!cameraId) {
    throw new Error('摄像头编号不能为空')
  }

  if (useMock) {
    const camera = mockCameras.find(
      (item) =>
        item.cameraId === cameraId ||
        item.id === cameraId
    )

    if (!camera) {
      throw new Error('摄像头不存在')
    }

    const normalized =
      normalizeCamera(camera)

    return wait({
      allowed:
        normalized.canAccessNow &&
        normalized.status === 'online',

      cameraId,

      cameraName:
        normalized.cameraName,

      location:
        normalized.location,

      cameraStatus:
        normalized.status,

      serverTime:
        new Date().toISOString(),

      accessStartTime:
        normalized.accessStartTime,

      accessEndTime:
        normalized.accessEndTime,

      reason:
        normalized.canAccessNow
          ? 'ACCESS_ALLOWED'
          : 'OUTSIDE_ACCESS_TIME',

      message:
        normalized.canAccessNow
          ? '允许访问'
          : '当前不在允许访问时间内'
    })
  }

  const result = await request.get(
    `/api/cameras/${encodeURIComponent(cameraId)}/access-check`
  )

  return {
    ...result,

    allowed: Boolean(result.allowed),

    cameraStatus: String(
      result.cameraStatus || ''
    ).toLowerCase()
  }
}

/**
 * 获取摄像头播放地址。
 */
export async function getCameraStreamApi(
  cameraId
) {
  if (!cameraId) {
    throw new Error('摄像头编号不能为空')
  }

  if (useMock) {
    return wait({
      cameraId,
      streamType: 'iframe',
      streamUrl:
        'http://64.176.57.254:8889/live/desktop/',
      expiresAt: new Date(
        Date.now() + 60 * 60 * 1000
      ).toISOString()
    })
  }

  const result = await request.get(
    `/api/cameras/${encodeURIComponent(cameraId)}/stream`
  )

  return {
    ...result,

    streamType: String(
      result.streamType || ''
    ).toLowerCase()
  }
}

/**
 * 摄像头截图 AI 分析。
 */
export async function analyzeCameraSnapshotApi(
  cameraId,
  imageData
) {
  if (!cameraId) {
    throw new Error('摄像头编号不能为空')
  }

  if (!imageData) {
    throw new Error('截图数据不能为空')
  }

  if (useMock) {
    return wait({
      cameraId,
      capturedAt:
        new Date().toISOString(),

      summary:
        'AI 分析完成：画面中未发现明显异常。',

      riskLevel: 'low',
      confidence: 0.92,

      findings: [
        '画面亮度正常',
        '未检测到明显危险行为',
        '摄像头画面无遮挡'
      ],

      suggestion:
        '建议继续保持当前摄像头角度。'
    })
  }

  return request.post(
    `/api/cameras/${encodeURIComponent(cameraId)}/analysis`,
    {
      image: imageData
    }
  )
}