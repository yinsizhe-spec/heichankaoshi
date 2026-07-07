<template>
  <div>
    <AppHeader />
    <main class="container">
      <button class="secondary" @click="router.push('/dashboard')">返回摄像头列表</button>

      <section class="watch-panel">
        <h1>{{ camera?.cameraName || '摄像头播放' }}</h1>
        <p class="muted">位置：{{ camera?.location || '-' }}</p>
        <div class="info-grid compact">
          <span>允许访问时间</span><strong>{{ access.startTime || '-' }} - {{ access.endTime || '-' }}</strong>
          <span>当前状态</span><strong>{{ access.allowed ? '正在观看' : '不可访问' }}</strong>
          <span>剩余时间</span><strong>{{ countdownText }}</strong>
        </div>
      </section>

      <section v-if="loading" class="state-card">正在校验访问权限...</section>
      <section v-else-if="!access.allowed" class="forbidden-box">
        <h2>当前无法访问该摄像头</h2>
        <p>你只能在以下时间段访问：</p>
        <strong>{{ access.startTime || '-' }} - {{ access.endTime || '-' }}</strong>
        <p>{{ access.reason || '当前不在允许访问时间内，请在允许访问时间内重新进入。' }}</p>
      </section>
      <section v-else>
        <CameraPlayer :stream-url="cameraStore.streamUrl" :stream-type="cameraStore.streamType" />
        <p v-if="endedMessage" class="error-text">{{ endedMessage }}</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import CameraPlayer from '../components/CameraPlayer.vue'
import { useCameraStore } from '../stores/cameraStore'
import { formatSeconds, getSecondsUntil } from '../utils/time'

const route = useRoute()
const router = useRouter()
const cameraStore = useCameraStore()
const loading = ref(false)
const secondsLeft = ref(0)
const endedMessage = ref('')
let timer = null

const cameraId = computed(() => route.params.cameraId)
const camera = computed(() => cameraStore.findCamera(cameraId.value) || cameraStore.currentCamera)
const access = computed(() => cameraStore.accessStatus)
const countdownText = computed(() => access.value.allowed ? formatSeconds(secondsLeft.value) : '-')

function startCountdown() {
  clearInterval(timer)
  secondsLeft.value = getSecondsUntil(access.value.endTime)
  timer = setInterval(() => {
    secondsLeft.value = getSecondsUntil(access.value.endTime)
    if (secondsLeft.value <= 0) {
      clearInterval(timer)
      cameraStore.stopStream()
      cameraStore.accessStatus.allowed = false
      endedMessage.value = '你的摄像头访问时间已结束，视频播放已停止。'
    }
  }, 1000)
}

async function init() {
  loading.value = true
  endedMessage.value = ''
  try {
    if (!cameraStore.cameraList.length) await cameraStore.fetchCameras()
    const result = await cameraStore.checkAccess(cameraId.value)
    if (result.allowed) {
      await cameraStore.fetchStream(cameraId.value)
      startCountdown()
    } else {
      cameraStore.stopStream()
    }
  } catch (e) {
    cameraStore.stopStream()
    cameraStore.accessStatus = {
      allowed: false,
      reason: e.message || '无权限、超时或视频加载失败',
      startTime: '',
      endTime: '',
      serverTime: ''
    }
  } finally {
    loading.value = false
  }
}

onMounted(init)
onBeforeUnmount(() => {
  clearInterval(timer)
  cameraStore.stopStream()
})
</script>
