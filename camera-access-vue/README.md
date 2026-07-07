# 摄像头访问控制 Vue 前端项目

基于《摄像头访问控制应用前端设计文档》实现：登录认证、摄像头列表、摄像头播放、访问时间控制、个人资料、403/404 页面。

## 技术栈

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- hls.js

## 启动

```bash
npm install
cp .env.example .env
npm run dev
```

默认开启 `VITE_USE_MOCK=true`，可以直接用以下账号测试：

```text
用户名：admin
密码：123456
```

## 后端接口

关闭 mock 后，前端会请求：

- POST `/api/auth/login`
- GET `/api/users/me`
- GET `/api/cameras`
- GET `/api/cameras/{cameraId}/access-check`
- GET `/api/cameras/{cameraId}/stream`

## 目录结构

```text
src
├── api
├── router
├── stores
├── views
├── components
└── utils
```
