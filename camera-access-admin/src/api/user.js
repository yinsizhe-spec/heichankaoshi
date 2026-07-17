import http from './http'
import { mockUsers } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
let users = structuredClone(mockUsers)

function normalizePage(params, records) {
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

export async function listUsersApi(params = {}) {
  if (useMock) {
    let records = [...users]
    const keyword = String(params.keyword || '').trim().toLowerCase()
    if (keyword) {
      records = records.filter((item) =>
        [item.username, item.displayName, item.email]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword))
      )
    }
    if (params.status) {
      records = records.filter((item) => item.status === params.status)
    }
    return normalizePage(params, records)
  }
  return http.get('/api/admin/users', { params })
}

export async function createUserApi(payload) {
  if (useMock) {
    const item = {
      ...payload,
      id: Date.now(),
      cameraCount: 0,
      createdAt: new Date().toLocaleString()
    }
    users.unshift(item)
    return item
  }
  return http.post('/api/admin/users', payload)
}

export async function updateUserApi(id, payload) {
  if (useMock) {
    const index = users.findIndex((item) => item.id === id)
    users[index] = { ...users[index], ...payload }
    return users[index]
  }
  return http.put(`/api/admin/users/${id}`, payload)
}

export async function deleteUserApi(id) {
  if (useMock) {
    users = users.filter((item) => item.id !== id)
    return true
  }
  return http.delete(`/api/admin/users/${id}`)
}

export async function updateUserStatusApi(id, status) {
  if (useMock) {
    return updateUserApi(id, { status })
  }
  return http.put(`/api/admin/users/${id}/status`, { status })
}

export async function resetUserPasswordApi(id, password) {
  if (useMock) {
    return { id, success: Boolean(password) }
  }
  return http.put(`/api/admin/users/${id}/password`, { password })
}
