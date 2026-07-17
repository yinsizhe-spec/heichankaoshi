import http from './http'
import { mockCameras } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
let cameras = structuredClone(mockCameras)

function pageResult(params, records) {
  const page = Number(params.page || 1)
  const size = Number(params.size || 10)
  const start = (page - 1) * size
  return {
    records: records.slice(start, start + size),
    page,
    size,
    total: records.length
  }
}

export async function listCamerasApi(params = {}) {
  if (useMock) {
    let records = [...cameras]
    const keyword = String(params.keyword || '').trim().toLowerCase()
    if (keyword) {
      records = records.filter((item) =>
        [item.cameraName, item.cameraCode, item.location]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword))
      )
    }
    if (params.adminStatus) {
      records = records.filter((item) => item.adminStatus === params.adminStatus)
    }
    if (params.runtimeStatus) {
      records = records.filter((item) => item.runtimeStatus === params.runtimeStatus)
    }
    return pageResult(params, records)
  }
  return http.get('/api/admin/cameras', { params })
}

export async function createCameraApi(payload) {
  if (useMock) {
    const item = {
      ...payload,
      id: Date.now(),
      runtimeStatus: 'OFFLINE',
      lastHeartbeatTime: '',
      offlineReason: ''
    }
    cameras.unshift(item)
    return item
  }
  return http.post('/api/admin/cameras', payload)
}

export async function updateCameraApi(id, payload) {
  if (useMock) {
    const index = cameras.findIndex((item) => item.id === id)
    cameras[index] = { ...cameras[index], ...payload }
    return cameras[index]
  }
  return http.put(`/api/admin/cameras/${id}`, payload)
}

export async function deleteCameraApi(id) {
  if (useMock) {
    cameras = cameras.filter((item) => item.id !== id)
    return true
  }
  return http.delete(`/api/admin/cameras/${id}`)
}

export async function setCameraOnlineApi(id, reason = '') {
  if (useMock) {
    return updateCameraApi(id, {
      adminStatus: 'ONLINE',
      offlineReason: '',
      statusReason: reason
    })
  }
  return http.put(`/api/admin/cameras/${id}/online`, { reason })
}

export async function setCameraOfflineApi(id, reason) {
  if (useMock) {
    return updateCameraApi(id, {
      adminStatus: 'OFFLINE',
      offlineReason: reason
    })
  }
  return http.put(`/api/admin/cameras/${id}/offline`, { reason })
}

export async function testCameraStreamApi(id) {
  if (useMock) {
    await new Promise((resolve) => setTimeout(resolve, 900))
    const camera = cameras.find((item) => item.id === id)
    return {
      cameraId: id,
      success: camera?.runtimeStatus === 'ONLINE',
      runtimeStatus: camera?.runtimeStatus || 'ERROR',
      message:
        camera?.runtimeStatus === 'ONLINE'
          ? '视频流连接正常'
          : '当前无法连接视频流'
    }
  }
  return http.post(`/api/admin/cameras/${id}/test-stream`)
}
