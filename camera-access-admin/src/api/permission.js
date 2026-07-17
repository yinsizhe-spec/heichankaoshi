import http from './http'
import { mockPermissions } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
let permissions = structuredClone(mockPermissions)

export async function listPermissionsApi(params = {}) {
  if (useMock) {
    let records = [...permissions]
    const keyword = String(params.keyword || '').trim().toLowerCase()
    if (keyword) {
      records = records.filter((item) =>
        [item.username, item.displayName, item.cameraName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword))
      )
    }
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
  return http.get('/api/admin/camera-permissions', { params })
}

export async function createPermissionApi(payload) {
  if (useMock) {
    const item = { ...payload, id: Date.now() }
    permissions.unshift(item)
    return item
  }
  return http.post('/api/admin/camera-permissions', payload)
}

export async function updatePermissionApi(id, payload) {
  if (useMock) {
    const index = permissions.findIndex((item) => item.id === id)
    permissions[index] = { ...permissions[index], ...payload }
    return permissions[index]
  }
  return http.put(`/api/admin/camera-permissions/${id}`, payload)
}

export async function deletePermissionApi(id) {
  if (useMock) {
    permissions = permissions.filter((item) => item.id !== id)
    return true
  }
  return http.delete(`/api/admin/camera-permissions/${id}`)
}
