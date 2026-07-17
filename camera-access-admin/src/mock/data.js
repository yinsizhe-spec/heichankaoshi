export const mockUsers = [
  {
    id: 1,
    username: 'zhangsan',
    displayName: '张三',
    email: 'zhangsan@example.com',
    phone: '0123456789',
    role: 'USER',
    status: 'ACTIVE',
    cameraCount: 2,
    validFrom: '2026-07-01',
    validUntil: '2026-08-01',
    createdAt: '2026-07-01 10:00:00'
  },
  {
    id: 2,
    username: 'lisi',
    displayName: '李四',
    email: 'lisi@example.com',
    phone: '0123456790',
    role: 'USER',
    status: 'DISABLED',
    cameraCount: 1,
    validFrom: '2026-07-10',
    validUntil: '2026-07-30',
    createdAt: '2026-07-02 11:20:00'
  },
  {
    id: 3,
    username: 'admin',
    displayName: '系统管理员',
    email: 'admin@example.com',
    phone: '',
    role: 'ADMIN',
    status: 'ACTIVE',
    cameraCount: 4,
    validFrom: '2026-01-01',
    validUntil: '2027-01-01',
    createdAt: '2026-01-01 09:00:00'
  }
]

export const mockCameras = [
  {
    id: 1,
    cameraCode: 'CAM-001',
    cameraName: 'Camera-01',
    location: 'Exam Room A',
    streamType: 'HLS',
    streamUrl: 'http://64.176.57.254:8888/live/desktop/index.m3u8',
    sourceStreamUrl: 'rtsp://64.176.57.254:8554/live/desktop',
    adminStatus: 'ONLINE',
    runtimeStatus: 'ONLINE',
    aiEnabled: true,
    lastHeartbeatTime: '2026-07-17 12:30:20',
    offlineReason: ''
  },
  {
    id: 2,
    cameraCode: 'CAM-002',
    cameraName: 'Camera-02',
    location: 'Exam Room B',
    streamType: 'IFRAME',
    streamUrl: 'http://64.176.57.254:8888/live/classroom2',
    sourceStreamUrl: 'rtsp://64.176.57.254:8554/live/classroom2',
    adminStatus: 'ONLINE',
    runtimeStatus: 'OFFLINE',
    aiEnabled: true,
    lastHeartbeatTime: '2026-07-17 12:05:11',
    offlineReason: ''
  },
  {
    id: 3,
    cameraCode: 'CAM-003',
    cameraName: 'Camera-03',
    location: 'Exam Room C',
    streamType: 'HLS',
    streamUrl: 'http://64.176.57.254:8888/live/classroom3/index.m3u8',
    sourceStreamUrl: 'rtsp://64.176.57.254:8554/live/classroom3',
    adminStatus: 'OFFLINE',
    runtimeStatus: 'ONLINE',
    aiEnabled: false,
    lastHeartbeatTime: '2026-07-17 12:29:45',
    offlineReason: '设备维护'
  }
]

export const mockPermissions = [
  {
    id: 1,
    userId: 1,
    username: 'zhangsan',
    displayName: '张三',
    cameraId: 1,
    cameraName: 'Camera-01',
    accessStartTime: '08:00',
    accessEndTime: '12:00',
    validFrom: '2026-07-01',
    validUntil: '2026-08-01',
    aiAnalysisAllowed: true,
    status: 'ACTIVE'
  },
  {
    id: 2,
    userId: 1,
    username: 'zhangsan',
    displayName: '张三',
    cameraId: 2,
    cameraName: 'Camera-02',
    accessStartTime: '13:00',
    accessEndTime: '18:00',
    validFrom: '2026-07-01',
    validUntil: '2026-08-01',
    aiAnalysisAllowed: true,
    status: 'ACTIVE'
  },
  {
    id: 3,
    userId: 2,
    username: 'lisi',
    displayName: '李四',
    cameraId: 3,
    cameraName: 'Camera-03',
    accessStartTime: '09:00',
    accessEndTime: '17:00',
    validFrom: '2026-07-10',
    validUntil: '2026-07-30',
    aiAnalysisAllowed: false,
    status: 'DISABLED'
  }
]

export const mockOperations = [
  { id: 1, title: 'Camera-03 已被管理员下线', time: '12:32', type: 'warning' },
  { id: 2, title: '创建用户 zhangsan', time: '11:40', type: 'success' },
  { id: 3, title: 'Camera-01 视频流检测成功', time: '10:25', type: 'success' },
  { id: 4, title: '更新 lisi 的访问权限', time: '09:58', type: 'primary' }
]
