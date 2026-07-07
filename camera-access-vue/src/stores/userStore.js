import { defineStore } from 'pinia'
import { loginApi } from '../api/auth'
import { getCurrentUserApi } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('camera_token') || '',
    userInfo: JSON.parse(localStorage.getItem('camera_user') || 'null'),
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(form) {
      this.loading = true
      try {
        const data = await loginApi(form)
        this.token = data.token
        this.userInfo = data.user
        localStorage.setItem('camera_token', data.token)
        localStorage.setItem('camera_user', JSON.stringify(data.user))
        return data
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      const data = await getCurrentUserApi()
      this.userInfo = data
      localStorage.setItem('camera_user', JSON.stringify(data))
      return data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('camera_token')
      localStorage.removeItem('camera_user')
    }
  }
})
