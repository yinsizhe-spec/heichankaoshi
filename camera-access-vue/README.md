# Camera Access Vue

基于 Vue 3 + Vite 的摄像头访问控制前端项目。

## 功能

- 用户登录
- 摄像头列表
- 摄像头访问时间控制
- 摄像头播放
- 播放页拍照
- 拍照后调用 AI 分析并在独立区域展示结果，不覆盖摄像头画面
- 个人资料
- 403 / 404 页面
- Pinia 状态管理
- Axios 接口封装
- HLS 播放支持

## 运行

```bash
npm install
npm run dev
```

## 环境变量

复制 `.env.example` 为 `.env`。

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=true
```

`VITE_USE_MOCK=true` 时使用前端 mock 数据，不需要后端也可以预览页面。

## AI 分析接口

前端在用户点击“拍照并进行 AI 分析”后，会从视频播放器中截取当前帧，并调用：

```http
POST /api/cameras/{cameraId}/analysis
```

请求体：

```json
{
  "image": "data:image/jpeg;base64,..."
}
```

建议后端响应：

```json
{
  "cameraId": "cam_001",
  "capturedAt": "2026-07-07T12:30:00+08:00",
  "summary": "AI 分析完成：画面中未发现明显异常。",
  "riskLevel": "low",
  "confidence": 0.92,
  "findings": [
    "画面亮度正常",
    "未检测到人员聚集",
    "未检测到烟雾、火光或遮挡"
  ],
  "suggestion": "建议继续保持当前摄像头角度。"
}
```

## 登录 mock 账号

mock 模式下可以使用任意用户名和密码登录，页面默认填写：

```text
admin
123456
```
