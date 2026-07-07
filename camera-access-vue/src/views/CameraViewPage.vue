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
      <section v-else class="camera-watch-layout">
        <div class="video-column">
          <CameraPlayer
            ref="playerRef"
            :stream-url="cameraStore.streamUrl"
            :stream-type="cameraStore.streamType"
          />
          <div class="player-actions">
            <button class="primary" :disabled="analysisLoading" @click="takeSnapshotAndAnalyze">
              {{ analysisLoading ? 'AI 分析中...' : '拍照并进行 AI 分析' }}
            </button>
            <span class="muted">分析结果显示在右侧，不会覆盖摄像头画面。</span>
          </div>
          <p v-if="captureError" class="error-text">{{ captureError }}</p>
          <p v-if="endedMessage" class="error-text">{{ endedMessage }}</p>
        </div>

        <aside class="analysis-panel">
          <div class="analysis-header">
            <h2>AI 分析结果</h2>
            <span v-if="analysisResult" class="badge" :class="riskBadgeClass">
              {{ riskText }}
            </span>
          </div>

          <div v-if="!snapshotImage && !analysisResult" class="empty-analysis">
            <p>点击“拍照并进行 AI 分析”后，这里会显示抓拍图片和分析内容。</p>
            <p class="muted">该区域独立于视频播放器，不会遮挡实时画面。</p>
          </div>

          <img v-if="snapshotImage" class="snapshot-preview" :src="snapshotImage" alt="摄像头抓拍图片" />

          <div v-if="analysisLoading" class="state-card inline-card">正在分析当前抓拍画面...</div>

          <div v-if="analysisResult" class="analysis-content">
            <p class="analysis-summary">{{ analysisResult.summary }}</p>
            <div class="info-grid compact">
              <span>分析时间</span><strong>{{ formatDateTime(analysisResult.capturedAt) }}</strong>
              <span>置信度</span><strong>{{ confidenceText }}</strong>
            </div>
            <h3>发现内容</h3>
            <ul>
              <li v-for="item in analysisResult.findings" :key="item">{{ item }}</li>
            </ul>
            <h3>建议</h3>
            <p>{{ analysisResult.suggestion }}</p>
          </div>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import CameraPlayer from '../components/CameraPlayer.vue'
import { analyzeCameraSnapshotApi } from '../api/camera'
import { useCameraStore } from '../stores/cameraStore'
import { formatSeconds, getSecondsUntil } from '../utils/time'

const route = useRoute()
const router = useRouter()
const cameraStore = useCameraStore()
const loading = ref(false)
const secondsLeft = ref(0)
const endedMessage = ref('')
const playerRef = ref(null)
const snapshotImage = ref('')
const analysisResult = ref(null)
const analysisLoading = ref(false)
const captureError = ref('')
let timer = null

const cameraId = computed(() => route.params.cameraId)
const camera = computed(() => cameraStore.findCamera(cameraId.value) || cameraStore.currentCamera)
const access = computed(() => cameraStore.accessStatus)
const countdownText = computed(() => access.value.allowed ? formatSeconds(secondsLeft.value) : '-')
const confidenceText = computed(() => {
  if (!analysisResult.value?.confidence && analysisResult.value?.confidence !== 0) return '-'
  return `${Math.round(analysisResult.value.confidence * 100)}%`
})
const riskText = computed(() => {
  const level = analysisResult.value?.riskLevel
  if (level === 'high') return '高风险'
  if (level === 'medium') return '中风险'
  return '低风险'
})
const riskBadgeClass = computed(() => {
  const level = analysisResult.value?.riskLevel
  if (level === 'high') return 'badge-danger'
  if (level === 'medium') return 'badge-warning'
  return 'badge-success'
})

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

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

async function takeSnapshotAndAnalyze() {
  captureError.value = ''
  analysisLoading.value = true
  try {
    const imageData = playerRef.value.captureFrame()
    snapshotImage.value = imageData
    analysisResult.value = await analyzeCameraSnapshotApi(cameraId.value, imageData)
  } catch (e) {
    captureError.value = e.message || '拍照或 AI 分析失败，请稍后重试'
  } finally {
    analysisLoading.value = false
  }
}

async function init() {
  loading.value = true
  endedMessage.value = ''
  captureError.value = ''
  snapshotImage.value = ''
  analysisResult.value = null
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
