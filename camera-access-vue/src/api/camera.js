import request from '../utils/request'
import { mockCameras, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

const MEDIAMTX_WEBRTC_URL =
  'http://64.176.57.254:8889/live/desktop/'

const SUPPORTED_ANSWER_MODES = [
  'simple',
  'balanced',
  'best'
]

/**
 * 统一后端和前端的摄像头字段格式。
 */
function normalizeCamera(camera = {}) {
  return {
    ...camera,

    cameraId:
      camera.cameraId ||
      camera.id ||
      '',

    cameraName:
      camera.cameraName ||
      camera.name ||
      '未命名摄像头',

    location:
      camera.location ||
      '未知位置',

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

    validFrom:
      camera.validFrom || '',

    validUntil:
      camera.validUntil || '',

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
 * 检查当前用户能否访问指定摄像头。
 */
export async function checkCameraAccessApi(
  cameraId
) {
  if (!cameraId) {
    throw new Error(
      '摄像头编号不能为空'
    )
  }

  if (useMock) {
    const camera = mockCameras.find(
      (item) =>
        String(item.cameraId || item.id) ===
        String(cameraId)
    )

    if (!camera) {
      throw new Error(
        '摄像头不存在'
      )
    }

    const normalized =
      normalizeCamera(camera)

    const allowed =
      normalized.status === 'online' &&
      normalized.canAccessNow

    return wait({
      allowed,
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
      validFrom:
        normalized.validFrom,
      validUntil:
        normalized.validUntil,
      reason: allowed
        ? 'ACCESS_ALLOWED'
        : 'OUTSIDE_ACCESS_TIME',
      message: allowed
        ? '允许访问'
        : normalized.accessMessage ||
          '当前不在允许访问时间内'
    })
  }

  const result = await request.get(
    `/api/cameras/${encodeURIComponent(cameraId)}/access-check`
  )

  return {
    ...result,

    allowed:
      Boolean(result?.allowed),

    cameraStatus:
      String(
        result?.cameraStatus || ''
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
    throw new Error(
      '摄像头编号不能为空'
    )
  }

  if (useMock) {
    return wait({
      cameraId,
      cameraName: '模拟摄像头',
      streamType: 'iframe',
      streamUrl:
        MEDIAMTX_WEBRTC_URL,
      serverTime:
        new Date().toISOString(),
      expiresAt: new Date(
        Date.now() +
          60 * 60 * 1000
      ).toISOString()
    })
  }

  const result = await request.get(
    `/api/cameras/${encodeURIComponent(cameraId)}/stream`
  )

  return {
    ...result,

    streamType: String(
      result?.streamType || ''
    ).toLowerCase()
  }
}

/**
 * 使用后端 FFmpeg 截图并调用 AI 搜题。
 *
 * 前端不再从 iframe 截图，也不再发送 image。
 */
export async function analyzeCameraSnapshotApi(
  cameraId,
  answerMode = 'best'
) {
  if (!cameraId) {
    throw new Error(
      '摄像头编号不能为空'
    )
  }

  const normalizedMode = String(
    answerMode || 'best'
  )
    .trim()
    .toLowerCase()

  const finalMode =
    SUPPORTED_ANSWER_MODES.includes(
      normalizedMode
    )
      ? normalizedMode
      : 'best'

  if (useMock) {
    return wait({
      cameraId,
      capturedAt:
        new Date().toISOString(),

      questionNo: '第 1 题',

      questionTitle:
        '请根据画面中的题目作答',

      recognizedText:
        '当前为模拟识别内容。请关闭 Mock 后连接真实后端。',

      questionType:
        'single_choice',

      simpleAnswer:
        'A',

      balancedAnswer:
        '答案：A。根据题目条件判断，A 选项最符合要求。',

      bestAnswer:
        '最优答案：A。根据题干中的核心条件进行分析，A 选项符合题目要求，其他选项与题干条件不完全对应。',

      confidence: 0.96
    })
  }

  return request.post(
    `/api/cameras/${encodeURIComponent(cameraId)}/analysis`,
    {
      answerMode: finalMode
    },
    {
      // FFmpeg 截图和 AI 分析可能耗时较长
      timeout: 150000
    }
  )
}