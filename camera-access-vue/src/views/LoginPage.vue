<template>
  <main class="login-page">
    <section class="login-card">
      <h1>摄像头访问控制系统</h1>
      <p class="muted">请登录后查看授权摄像头内容</p>

      <form @submit.prevent="handleLogin">
        <label>用户名 / 邮箱</label>
        <input v-model.trim="form.username" placeholder="请输入用户名或邮箱" autocomplete="username" />

        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />

        <p v-if="error" class="error-text">{{ error }}</p>
        <button class="primary full" :disabled="userStore.loading">
          {{ userStore.loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="demo-tip">Mock 测试：任意用户名 + 任意密码均可登录</div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const error = ref('')
const form = reactive({ username: 'admin', password: '123456' })

async function handleLogin() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  try {
    await userStore.login(form)
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    error.value = e.message || '账号错误、密码错误或网络错误'
  }
}
</script>
