import http from './http'
import { mockCameras, mockOperations, mockPermissions, mockUsers } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function getDashboardSummaryApi() {
  if (useMock) {
    return {
      userCount: mockUsers.length,
      cameraCount: mockCameras.length,
      onlineCameraCount: mockCameras.filter((item) => item.status === 'ONLINE').length,
      offlineCameraCount: mockCameras.filter((item) => item.status === 'OFFLINE').length,
      maintenanceCameraCount: mockCameras.filter((item) => item.status === 'MAINTENANCE').length,
      activePermissionCount: mockPermissions.filter((item) => item.status === 'ACTIVE').length,
      todayAnalysisCount: 37,
      cameras: mockCameras,
      operations: mockOperations
    }
  }
  return http.get('/api/admin/dashboard/summary')
}
