# Camera Access Admin

智慧考试系统后台管理前端。

## 功能

- 管理员登录
- 后台控制台
- 用户新增、编辑、删除、启用、禁用和重置密码
- 摄像头新增、编辑、删除和视频预览
- 使用 `status` 字段控制摄像头上线、下线和维护状态
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

摄像头列表接口：

```text
GET /api/admin/cameras?page=1&size=10&keyword=&status=ONLINE
```

修改摄像头状态接口：

```text
PUT /api/admin/cameras/{cameraId}/status
```

请求体：

```json
{
  "status": "ONLINE"
}
```

支持状态：

- `ONLINE`：上线
- `OFFLINE`：下线
- `MAINTENANCE`：维护中

前端不会检测视频流是否可以播放，上线和下线也不要求填写原因。
