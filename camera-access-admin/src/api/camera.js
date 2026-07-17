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

function normalizePage(data) {
  return {
    records: data.records || [],
    page: data.current || data.page || 1,
    size: data.size || 10,
    total: data.total || 0
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

    if (params.status) {
      records = records.filter((item) => item.status === params.status)
    }

    return pageResult(params, records)
  }

  const data = await http.get('/api/admin/cameras', { params })
  return normalizePage(data)
}

export async function createCameraApi(payload) {
  if (useMock) {
    const item = {
      ...payload,
      id: Date.now(),
      status: payload.status || 'OFFLINE'
    }
    cameras.unshift(item)
    return item
  }

  return http.post('/api/admin/cameras', payload)
}

export async function updateCameraApi(id, payload) {
  if (useMock) {
    const index = cameras.findIndex((item) => item.id === id)
    if (index < 0) {
      throw new Error('摄像头不存在')
    }
    cameras[index] = { ...cameras[index], ...payload }
    return cameras[index]
  }

  return http.put(`/api/admin/cameras/${id}`, payload)
}

export async function updateCameraStatusApi(id, status) {
  if (useMock) {
    return updateCameraApi(id, { status })
  }

  return http.put(`/api/admin/cameras/${id}/status`, { status })
}

export async function deleteCameraApi(id) {
  if (useMock) {
    cameras = cameras.filter((item) => item.id !== id)
    return true
  }

  return http.delete(`/api/admin/cameras/${id}`)
}
