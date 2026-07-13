<template>
  <header class="app-header">
    <router-link class="logo" to="/dashboard">KJ考试终端</router-link>
    <nav>
      <router-link to="/dashboard">摄像头</router-link>
      <router-link to="/profile">个人资料</router-link>
      <button class="link-btn" @click="handleLogout">退出登录</button>
    </nav>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>
