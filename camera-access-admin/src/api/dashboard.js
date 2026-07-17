import http from './http'
import { mockCameras, mockOperations, mockPermissions, mockUsers } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function getDashboardSummaryApi() {
  if (useMock) {
    return {
      userCount: mockUsers.length,
      cameraCount: mockCameras.length,
      onlineCameraCount: mockCameras.filter((item) => item.runtimeStatus === 'ONLINE').length,
      enabledCameraCount: mockCameras.filter((item) => item.adminStatus === 'ONLINE').length,
      activePermissionCount: mockPermissions.filter((item) => item.status === 'ACTIVE').length,
      todayAnalysisCount: 37,
      cameras: mockCameras,
      operations: mockOperations
    }
  }
  return http.get('/api/admin/dashboard/summary')
}
