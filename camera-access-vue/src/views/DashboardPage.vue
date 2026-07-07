<template>
  <div>
    <AppHeader />
    <main class="container">
      <div class="page-title">
        <div>
          <h1>摄像头列表</h1>
          <p class="muted">只显示当前用户被授权访问的摄像头</p>
        </div>
        <button class="secondary" @click="loadCameras">刷新状态</button>
      </div>

      <div v-if="cameraStore.loading" class="state-card">正在加载摄像头列表...</div>
      <div v-else-if="error" class="state-card error-text">{{ error }}</div>
      <div v-else class="camera-grid">
        <CameraCard v-for="camera in cameraStore.cameraList" :key="camera.cameraId" :camera="camera" @open="openCamera" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import CameraCard from '../components/CameraCard.vue'
import { useCameraStore } from '../stores/cameraStore'

const router = useRouter()
const cameraStore = useCameraStore()
const error = ref('')

async function loadCameras() {
  error.value = ''
  try {
    await cameraStore.fetchCameras()
  } catch (e) {
    error.value = e.message || '摄像头列表加载失败'
  }
}

function openCamera(cameraId) {
  router.push(`/cameras/${cameraId}`)
}

onMounted(loadCameras)
</script>
