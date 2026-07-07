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
    cameraName: 'SPM考试',
    location: 'taylors大礼堂',
    status: 'online',
    accessStartTime: `2026/7/7 ${currentHour}:00`,
    accessEndTime: `${nextHour}:59`,
    canAccessNow: true
  },
  {
    cameraId: 'cam_002',
    cameraName: 'TOC考试',
    location: 'taylors大礼堂',
    status: 'online',
    accessStartTime: '2026/7/7 13:00',
    accessEndTime: '2026/7/7 17:00',
    canAccessNow: false
  },
  {
    cameraId: 'cam_003',
    cameraName: 'PPIS考试',
    location: 'taylors大礼堂',
    status: 'offline',
    accessStartTime: '2026/7/7 09:00',
    accessEndTime: '2026/7/7 18:00',
    canAccessNow: false
  }
]

export function wait(data, delay = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(structuredClone(data)), delay))
}
