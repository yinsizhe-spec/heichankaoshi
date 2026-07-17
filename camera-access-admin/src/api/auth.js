import http from './http'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export async function loginApi(payload) {
  if (useMock) {
    await new Promise((resolve) => setTimeout(resolve, 500))
    if (!payload.username || !payload.password) {
      throw new Error('请输入用户名和密码')
    }
    return {
      token: 'mock-admin-token',
      user: {
        username: payload.username,
        displayName: '系统管理员',
        role: 'ADMIN'
      }
    }
  }

  const response = await http.post('/api/auth/login', {
    username: payload.username,
    password: payload.password
  })

  return {
    token: response.token || response.accessToken,
    user: response.user
  }
}
