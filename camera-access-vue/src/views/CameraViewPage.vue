<template>
  <div class="camera-view-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">实时摄像头</p>
        <h1>{{ currentCamera?.name || '摄像头播放' }}</h1>
        <p class="subtitle">
          {{ currentCamera?.location || '正在查看摄像头画面' }}
        </p>
      </div>

      <button class="back-button" @click="goBack">
        返回
      </button>
    </header>

    <section v-if="loading" class="state-card">
      正在加载摄像头...
    </section>

    <section v-else-if="accessDenied" class="state-card error">
      {{ accessMessage || '当前时间段不允许访问该摄像头' }}
    </section>

    <section v-else class="content-layout">
      <div class="main-panel">
        <div class="player-card">
          <CameraPlayer
            ref="playerRef"
            :stream-url="cameraStore.streamUrl"
            :stream-type="cameraStore.streamType"
          />
        </div>

        <div class="camera-info-card">
          <h2>摄像头信息</h2>

          <div class="info-grid">
            <span>摄像头 ID</span>
            <strong>{{ cameraId }}</strong>

            <span>状态</span>
            <strong>{{ currentCamera?.status || 'online' }}</strong>

            <span>允许访问时间</span>
            <strong>
              {{ currentCamera?.accessStartTime || '00:00' }}
              -
              {{ currentCamera?.accessEndTime || '23:59' }}
            </strong>

            <span>播放类型</span>
            <strong>{{ cameraStore.streamType || 'iframe' }}</strong>
          </div>
        </div>
      </div>

      <aside class="analysis-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">AI Analysis</p>
            <h2>试卷答案分析</h2>
          </div>

          <span v-if="analysisResult" class="badge badge-success">
            已生成答案
          </span>
        </div>

        <p class="panel-desc">
          点击按钮后会模拟识别当前画面中的卷子，并返回每道题的最简化答案、平衡答案和最优答案。
        </p>

        <button
          class="analyze-button"
          :disabled="analysisLoading"
          @click="takeSnapshotAndAnalyze"
        >
          {{ analysisLoading ? '分析中...' : 'AI 分析当前画面' }}
        </button>

        <p v-if="captureError" class="error-text">
          {{ captureError }}
        </p>

        <div v-if="snapshotImage" class="snapshot-box">
          <h3>截图占位预览</h3>
          <p class="snapshot-tip">
            当前使用 iframe WebRTC 播放，浏览器不能真实截取 iframe 内部画面。这里仅用于 mock AI 分析流程。
          </p>
          <img :src="snapshotImage" alt="camera snapshot" />
        </div>

        <div v-if="analysisResult" class="analysis-content">
          <p class="analysis-summary">
            {{ analysisResult.summary }}
          </p>

          <div class="info-grid compact">
            <span>分析时间</span>
            <strong>{{ formatDateTime(analysisResult.capturedAt) }}</strong>

            <span>置信度</span>
            <strong>{{ confidenceText }}</strong>

            <span>识别题目数</span>
            <strong>{{ analysisResult.questions?.length || 0 }} 道</strong>
          </div>

          <div
            v-for="question in analysisResult.questions"
            :key="question.questionNo"
            class="question-answer-card"
          >
            <div class="question-answer-header">
              <h3>{{ question.questionNo }}</h3>
              <span>{{ question.questionType }}</span>
            </div>

            <div class="answer-block simple-answer">
              <h4>最简化答案</h4>
              <p>{{ question.simpleAnswer }}</p>
            </div>

            <div class="answer-block balanced-answer">
              <h4>平衡答案</h4>
              <p>{{ question.balancedAnswer }}</p>
            </div>

            <div class="answer-block best-answer">
              <h4>最优答案</h4>
              <p>{{ question.bestAnswer }}</p>
            </div>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CameraPlayer from '../components/CameraPlayer.vue'
import { useCameraStore } from '../stores/cameraStore'
import { analyzeCameraSnapshotApi } from '../api/camera'

const route = useRoute()
const router = useRouter()
const cameraStore = useCameraStore()

const playerRef = ref(null)
const loading = ref(true)
const accessDenied = ref(false)
const accessMessage = ref('')
const analysisLoading = ref(false)
const captureError = ref('')
const snapshotImage = ref('')
const analysisResult = ref(null)

let accessTimer = null

const cameraId = computed(() => route.params.cameraId)

const currentCamera = computed(() => {
  return cameraStore.cameraList.find((item) => {
    return item.cameraId === cameraId.value || item.id === cameraId.value
  })
})

const confidenceText = computed(() => {
  const confidence = analysisResult.value?.confidence

  if (confidence === undefined || confidence === null) {
    return '-'
  }

  return `${Math.round(confidence * 100)}%`
})

async function initPage() {
  loading.value = true
  accessDenied.value = false
  accessMessage.value = ''
  captureError.value = ''
  snapshotImage.value = ''
  analysisResult.value = null

  try {
    if (!cameraStore.cameraList.length) {
      await cameraStore.fetchCameras()
    }

    const accessResult = await cameraStore.checkAccess(cameraId.value)

    if (!accessResult.allowed) {
      accessDenied.value = true
      accessMessage.value =
        accessResult.message ||
        accessResult.reason ||
        '当前时间段不允许访问该摄像头'
      return
    }

    await cameraStore.fetchStream(cameraId.value)

    startAccessCheckTimer()
  } catch (error) {
    console.error('摄像头页面初始化失败:', error)

    accessDenied.value = true
    accessMessage.value = error.message || '摄像头加载失败'
  } finally {
    loading.value = false
  }
}

function startAccessCheckTimer() {
  stopAccessCheckTimer()

  accessTimer = window.setInterval(async () => {
    try {
      const accessResult = await cameraStore.checkAccess(cameraId.value)

      if (!accessResult.allowed) {
        accessDenied.value = true
        accessMessage.value =
          accessResult.message ||
          accessResult.reason ||
          '访问时间已结束'

        stopAccessCheckTimer()
      }
    } catch (error) {
      console.error('定时检查摄像头访问权限失败:', error)
    }
  }, 60 * 1000)
}

function stopAccessCheckTimer() {
  if (accessTimer) {
    window.clearInterval(accessTimer)
    accessTimer = null
  }
}

async function takeSnapshotAndAnalyze() {
  captureError.value = ''
  analysisLoading.value = true
  analysisResult.value = null

  try {
    if (!playerRef.value || typeof playerRef.value.captureFrame !== 'function') {
      throw new Error('播放器尚未准备好，无法进行 AI 分析')
    }

    const imageData = playerRef.value.captureFrame()

    snapshotImage.value = imageData

    const result = await analyzeCameraSnapshotApi(cameraId.value, imageData)

    analysisResult.value = normalizeAnalysisResult(result)
  } catch (error) {
    console.error('AI 分析失败:', error)
    captureError.value = error.message || 'AI 分析失败，请稍后重试'
  } finally {
    analysisLoading.value = false
  }
}

function normalizeAnalysisResult(result) {
  if (!result) {
    return {
      capturedAt: new Date().toISOString(),
      confidence: 0,
      summary: '未获取到 AI 分析结果。',
      questions: []
    }
  }

  if (Array.isArray(result.questions)) {
    return {
      capturedAt: result.capturedAt || new Date().toISOString(),
      confidence: result.confidence ?? 0,
      summary: result.summary || '已完成试卷题目识别和答案生成。',
      questions: result.questions
    }
  }

  return {
    capturedAt: result.capturedAt || new Date().toISOString(),
    confidence: result.confidence ?? 0,
    summary: result.summary || '已完成试卷题目识别和答案生成。',
    questions: [
      {
        questionNo: result.questionNo || '第 1 题',
        questionType: result.questionType || '未知题型',
        simpleAnswer: result.simpleAnswer || result.answer || '暂无',
        balancedAnswer: result.balancedAnswer || result.summary || '暂无',
        bestAnswer: result.bestAnswer || result.suggestion || '暂无'
      }
    ]
  }
}

function formatDateTime(value) {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function goBack() {
  router.push('/dashboard')
}

onMounted(() => {
  initPage()
})

onBeforeUnmount(() => {
  stopAccessCheckTimer()
})
</script>

<style scoped>
.camera-view-page {
  min-height: 100vh;
  padding: 32px;
  background: #f5f7fb;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-header h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.2;
}

.subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.back-button {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  background: #e5e7eb;
  color: #111827;
  font-weight: 600;
  cursor: pointer;
}

.back-button:hover {
  background: #d1d5db;
}

.state-card {
  padding: 36px;
  border-radius: 18px;
  background: #ffffff;
  color: #374151;
  text-align: center;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.state-card.error {
  color: #b91c1c;
  background: #fee2e2;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 24px;
  align-items: start;
}

.main-panel {
  min-width: 0;
}

.player-card {
  overflow: hidden;
  border-radius: 18px;
  background: #000000;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.camera-info-card,
.analysis-panel {
  margin-top: 20px;
  padding: 22px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.camera-info-card h2,
.analysis-panel h2 {
  margin: 0;
  color: #111827;
}

.info-grid {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px 16px;
  margin-top: 18px;
  color: #6b7280;
  font-size: 14px;
}

.info-grid strong {
  color: #111827;
  word-break: break-all;
}

.info-grid.compact {
  grid-template-columns: 100px minmax(0, 1fr);
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.analysis-panel {
  position: sticky;
  top: 24px;
  margin-top: 0;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.panel-desc {
  margin: 0 0 18px;
  color: #6b7280;
  line-height: 1.7;
  font-size: 14px;
}

.badge {
  flex-shrink: 0;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge-success {
  color: #166534;
  background: #dcfce7;
}

.analyze-button {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 13px 18px;
  background: #2563eb;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.analyze-button:hover {
  background: #1d4ed8;
}

.analyze-button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.error-text {
  margin: 14px 0 0;
  padding: 12px;
  border-radius: 12px;
  background: #fee2e2;
  color: #b91c1c;
  line-height: 1.6;
  font-size: 14px;
}

.snapshot-box {
  margin-top: 18px;
}

.snapshot-box h3 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 16px;
}

.snapshot-tip {
  margin: 0 0 10px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.snapshot-box img {
  width: 40px;
  height: 40px;
  display: block;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  image-rendering: pixelated;
}

.analysis-content {
  margin-top: 18px;
}

.analysis-summary {
  margin: 0 0 14px;
  color: #374151;
  line-height: 1.7;
  font-size: 14px;
}

.question-answer-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

.question-answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.question-answer-header h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.question-answer-header span {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 700;
}

.answer-block {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  line-height: 1.7;
}

.answer-block h4 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 14px;
}

.answer-block p {
  margin: 0;
  color: #374151;
  font-size: 14px;
}

.simple-answer {
  background: #eff6ff;
}

.balanced-answer {
  background: #f0fdf4;
}

.best-answer {
  background: #fff7ed;
}

@media (max-width: 1080px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .analysis-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 640px) {
  .camera-view-page {
    padding: 20px;
  }

  .page-header {
    flex-direction: column;
  }

  .back-button {
    width: 100%;
  }

  .info-grid,
  .info-grid.compact {
    grid-template-columns: 1fr;
  }
}
</style>