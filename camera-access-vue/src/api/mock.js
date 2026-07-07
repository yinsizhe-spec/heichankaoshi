const now = new Date()
const currentHour = String(now.getHours()).padStart(2, '0')
const nextHour = String(Math.min(23, now.getHours() + 2)).padStart(2, '0')

export const mockUser = {
  id: 'user_001',
  username: 'Zhang San',
  email: 'zhangsan@example.com',
  role: 'user',
  status: 'active'
}

export const mockCameras = [
  {
    cameraId: 'cam_001',
    cameraName: '仓库入口摄像头',
    location: 'Warehouse Gate',
    status: 'online',
    accessStartTime: `${currentHour}:00`,
    accessEndTime: `${nextHour}:59`,
    canAccessNow: true
  },
  {
    cameraId: 'cam_002',
    cameraName: '办公室摄像头',
    location: 'Office',
    status: 'online',
    accessStartTime: '13:00',
    accessEndTime: '17:00',
    canAccessNow: false
  },
  {
    cameraId: 'cam_003',
    cameraName: '停车场摄像头',
    location: 'Parking Area',
    status: 'offline',
    accessStartTime: '09:00',
    accessEndTime: '18:00',
    canAccessNow: false
  }
]

export function wait(data, delay = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(structuredClone(data)), delay))
}
