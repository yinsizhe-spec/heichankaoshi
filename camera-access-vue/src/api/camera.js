import request from '../utils/request'
import { mockCameras, wait } from './mock'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

const MEDIAMTX_WEBRTC_URL = 'http://64.176.57.254:8889/live/desktop/'

export async function getCameraListApi() {
  if (useMock) {
    return wait(mockCameras)
  }

  return request.get('/api/cameras')
}

export async function checkCameraAccessApi(cameraId) {
  if (useMock) {
    const camera = mockCameras.find((item) => {
      return item.cameraId === cameraId || item.id === cameraId
    })

    if (!camera) {
      throw new Error('摄像头不存在')
    }

    return wait({
      allowed: camera.canAccessNow && camera.status === 'online',
      cameraId,
      serverTime: new Date().toISOString(),
      accessStartTime: camera.accessStartTime,
      accessEndTime: camera.accessEndTime,
      message: camera.canAccessNow
        ? 'Access allowed'
        : 'Current time is outside the allowed access period'
    })
  }

  return request.get(`/api/cameras/${cameraId}/access-check`)
}

export async function getCameraStreamApi(cameraId) {
  if (useMock) {
    return wait({
      cameraId,
      streamType: 'iframe',
      streamUrl: MEDIAMTX_WEBRTC_URL,
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
      confidence: 0.96,
      paperTitle: '当前画面试卷识别结果',
      summary: '已从当前画面中模拟识别到多道题目，以下为模拟 AI 答案结果。',
      questions: [
        {
          questionNo: '第 1 题',
          questionType: '选择题',
          simpleAnswer: 'A',
          balancedAnswer: '答案：A。根据题干关键词判断，A 选项最符合题意。',
          bestAnswer:
            '最优答案：A。题干中的核心条件与 A 选项完全对应，其他选项要么缺少关键条件，要么与题意存在偏差，因此选择 A。'
        },
        {
          questionNo: '第 2 题',
          questionType: '选择题',
          simpleAnswer: 'B',
          balancedAnswer: '答案：B。B 选项能够较好地对应题目中的主要条件。',
          bestAnswer:
            '最优答案：B。根据题干要求，需要选择最符合条件的选项。B 同时满足题目给出的主要限制条件和结论要求，因此优先选择 B。'
        },
        {
          questionNo: '第 3 题',
          questionType: '简答题',
          simpleAnswer: '写出核心概念即可。',
          balancedAnswer:
            '平衡答案：先说明核心概念，再结合题干条件进行简要解释，保证答案完整但不过度展开。',
          bestAnswer:
            '最优答案：本题应先点明核心概念，然后结合题目中的具体条件进行分析，最后给出明确结论。回答时需要包含关键词、原因和结论三部分，这样得分更稳定。'
        },
        {
          questionNo: '第 4 题',
          questionType: '判断题',
          simpleAnswer: '正确',
          balancedAnswer: '答案：正确。题干表述与相关定义或规则一致。',
          bestAnswer:
            '最优答案：正确。根据题目涉及的定义或规则，题干中的说法没有与基本条件冲突，因此该判断为正确。'
        },
        {
          questionNo: '第 5 题',
          questionType: '选择题',
          simpleAnswer: 'C',
          balancedAnswer: '答案：C。C 选项覆盖了题目中的关键条件。',
          bestAnswer:
            '最优答案：C。题干要求选择最全面、最准确的选项。C 选项不仅符合题目条件，而且表达更完整，因此优于其他选项。'
        }
      ],
      imageData
    })
  }

  return request.post(`/api/cameras/${cameraId}/analysis`, {
    image: imageData
  })
}