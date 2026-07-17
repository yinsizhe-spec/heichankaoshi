import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginApi } from '@/api/auth'

const TOKEN_KEY = 'camera_admin_token'
const USER_KEY = 'camera_admin_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isLoggedIn = computed(() => Boolean(token.value))

  async function login(payload) {
    const data = await loginApi(payload)
    token.value = data.token
    user.value = data.user || {
      username: payload.username,
      displayName: '系统管理员',
      role: 'ADMIN'
    }

    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    logout
  }
})
