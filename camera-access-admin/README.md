# Camera Access Admin

智慧考试系统后台管理前端。

## 功能

- 管理员登录
- 后台控制台
- 用户新增、编辑、删除、启用、禁用和重置密码
- 摄像头新增、编辑、删除和视频预览
- 摄像头管理员上线、下线和下线原因
- 摄像头视频流状态展示和检测
- 用户摄像头访问权限和时间段管理
- 模拟数据模式

## 启动

```bash
npm install
npm run dev
```

开发地址：

```text
http://localhost:5174
```

开发环境默认启用模拟数据：

```env
VITE_USE_MOCK=true
```

模拟模式下输入任意用户名和密码即可登录。

## 对接后端

编辑 `.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=false
```

项目调用的接口：

```text
POST   /api/auth/login

GET    /api/admin/dashboard/summary

GET    /api/admin/users
POST   /api/admin/users
PUT    /api/admin/users/{id}
DELETE /api/admin/users/{id}
PUT    /api/admin/users/{id}/status
PUT    /api/admin/users/{id}/password

GET    /api/admin/cameras
POST   /api/admin/cameras
PUT    /api/admin/cameras/{id}
DELETE /api/admin/cameras/{id}
PUT    /api/admin/cameras/{id}/online
PUT    /api/admin/cameras/{id}/offline
POST   /api/admin/cameras/{id}/test-stream

GET    /api/admin/camera-permissions
POST   /api/admin/camera-permissions
PUT    /api/admin/camera-permissions/{id}
DELETE /api/admin/camera-permissions/{id}
```

## 摄像头状态设计

`adminStatus`：

- `ONLINE`：管理员已上线，允许用户访问
- `OFFLINE`：管理员已下线，禁止用户访问

`runtimeStatus`：

- `ONLINE`：视频流实际在线
- `OFFLINE`：视频流实际离线
- `CONNECTING`：正在连接
- `ERROR`：推流异常

用户可以观看摄像头需要同时满足：

```text
adminStatus = ONLINE
runtimeStatus = ONLINE
用户拥有访问权限
当前日期在授权有效期内
当前时间在每日允许时间段内
```
