<template>
  <div>
    <AppHeader />
    <main class="container">
      <h1>个人资料</h1>
      <section class="profile-card">
        <div class="avatar">{{ avatarText }}</div>
        <div>
          <h2>{{ user?.username || '-' }}</h2>
          <p class="muted">{{ user?.email || '-' }}</p>
        </div>
      </section>

      <section class="info-section">
        <h2>账号信息</h2>
        <div class="info-grid">
          <span>用户 ID</span><strong>{{ user?.id || '-' }}</strong>
          <span>用户名</span><strong>{{ user?.username || '-' }}</strong>
          <span>邮箱</span><strong>{{ user?.email || '-' }}</strong>
          <span>角色</span><strong>{{ user?.role || '-' }}</strong>
          <span>账号状态</span><strong>{{ user?.status || 'active' }}</strong>
        </div>
      </section>

      <section class="info-section">
        <h2>可访问摄像头</h2>
        <div v-if="cameraStore.cameraList.length === 0" class="state-card">暂无可访问摄像头</div>
        <div v-else class="camera-grid">
          <article v-for="camera in cameraStore.cameraList" :key="camera.cameraId" class="mini-card">
            <h3>{{ camera.cameraName }}</h3>
            <p class="muted">{{ camera.location }}</p>
            <p>允许访问时间：{{ camera.accessStartTime }} - {{ camera.accessEndTime }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import { useUserStore } from '../stores/userStore'
import { useCameraStore } from '../stores/cameraStore'

const userStore = useUserStore()
const cameraStore = useCameraStore()
const user = computed(() => userStore.userInfo)
const avatarText = computed(() => user.value?.username?.slice(0, 1)?.toUpperCase() || 'U')

onMounted(async () => {
  if (!userStore.userInfo) await userStore.fetchMe()
  if (!cameraStore.cameraList.length) await cameraStore.fetchCameras()
})
</script>
