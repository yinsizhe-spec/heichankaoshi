<template>
  <div>
    <AppHeader />
    <div class="camera-view-page">
      <header class="page-header">
        <div>
          <button class="back-link" type="button" @click="goBack">
            ← 返回摄像头列表
          </button>

          <h1>{{ currentCamera?.name || '摄像头播放' }}</h1>

          <p class="subtitle">
            {{ currentCamera?.location || '正在查看摄像头画面' }}
          </p>
        </div>

        <span v-if="!loading && !accessDenied" class="access-status">
          当前可以访问
        </span>
      </header>

      <section v-if="loading" class="state-card">
        正在加载摄像头...
      </section>

      <section v-else-if="accessDenied" class="state-card error">
        <h2>当前无法访问摄像头</h2>

        <p>
          {{ accessMessage || '当前时间段不允许访问该摄像头' }}
        </p>

        <button class="back-button" type="button" @click="goBack">
          返回摄像头列表
        </button>
      </section>

      <section v-else class="content-layout">
        <div class="main-panel">
          <div class="player-card">
            <div class="player-card-header">
              <div>
                <h2>实时摄像头画面</h2>

                <p>
                  访问时间：
                  {{ accessStartTime }}
                  -
                  {{ accessEndTime }}
                </p>
              </div>

              <span v-if="remainingTimeText" class="remaining-time">
                剩余时间：{{ remainingTimeText }}
              </span>
            </div>

            <div class="player-wrapper">
              <CameraPlayer ref="playerRef" :stream-url="cameraStore.streamUrl" :stream-type="cameraStore.streamType" />
            </div>
          </div>

          <!-- <div class="camera-info-card">
          <h2>摄像头信息</h2>

          <div class="info-grid">
            <span>摄像头 ID</span>
            <strong>{{ cameraId }}</strong>

            <span>状态</span>
            <strong>
              {{ currentCamera?.status || 'online' }}
            </strong>

            <span>允许访问时间</span>
            <strong>
              {{ accessStartTime }} - {{ accessEndTime }}
            </strong>

            <span>播放类型</span>
            <strong>
              {{ cameraStore.streamType || 'iframe' }}
            </strong>

            <span>剩余访问时间</span>
            <strong>
              {{ remainingTimeText || '--:--:--' }}
            </strong>
          </div>
        </div> -->
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
            点击按钮后截取当前摄像头画面，并识别画面中的题目和答案。
          </p>

          <div class="answer-mode-selector">
            <span class="answer-mode-label">请求回答模式</span>

            <div class="answer-mode-buttons">
              <button v-for="option in answerModeOptions" :key="option.value" type="button" class="answer-mode-button"
                :class="{
                  active: answerMode === option.value
                }" :disabled="analysisLoading" @click="answerMode = option.value">
                {{ option.label }}
              </button>
            </div>

            <p class="answer-mode-tip">
              选择内容会作为请求参数发送，分析结果仍然显示全部三种答案。
            </p>
          </div>

          <button class="analyze-button" type="button" :disabled="analysisLoading" @click="takeSnapshotAndAnalyze">
            {{
              analysisLoading
                ? '分析中...'
                : `AI 分析当前画面（${currentAnswerModeLabel}）`
            }}
          </button>

          <p v-if="captureError" class="error-text">
            {{ captureError }}
          </p>

          <div v-if="analysisResult" class="analysis-content">
            <p class="analysis-summary">
              {{ analysisResult.summary }}
            </p>

            <div class="info-grid compact">
              <span>分析时间</span>
              <strong>
                {{ formatDateTime(analysisResult.capturedAt) }}
              </strong>

              <span>置信度</span>
              <strong>{{ confidenceText }}</strong>

              <!-- <span>识别题目数</span>
            <strong>
              {{ analysisResult.questions?.length || 0 }} 道
            </strong> -->
            </div>

            <div v-for="question in analysisResult.questions" :key="question.questionNo" class="question-answer-card">
              <div class="question-answer-header">
                <h3>{{ question.questionNo }}</h3>

                <span>
                  {{ question.questionType }}
                </span>
              </div>

              <p v-if="question.questionTitle" class="question-title">
                {{ question.questionTitle }}
              </p>

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
  </div>
</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import CameraPlayer from '../components/CameraPlayer.vue'
import { analyzeCameraSnapshotApi } from '../api/camera'
import { useCameraStore } from '../stores/cameraStore'

const route = useRoute()
const router = useRouter()
const cameraStore = useCameraStore()

const playerRef = ref(null)

const loading = ref(true)
const accessDenied = ref(false)
const accessMessage = ref('')

const analysisLoading = ref(false)
const captureError = ref('')
const analysisResult = ref(null)

const answerMode = ref('simple')

const answerModeOptions = [
  {
    value: 'simple',
    label: '最简化答案'
  },
  {
    value: 'balanced',
    label: '平衡答案'
  },
  {
    value: 'best',
    label: '最优答案'
  }
]

const remainingSeconds = ref(0)

let accessTimer = null
let countdownTimer = null
let serverTimeOffset = 0
let accessEndTimestamp = 0

const cameraId = computed(() => {
  return String(route.params.cameraId || '')
})

const currentCamera = computed(() => {
  return cameraStore.cameraList.find((item) => {
    return (
      String(item.cameraId) === cameraId.value ||
      String(item.id) === cameraId.value
    )
  })
})

const accessStartTime = computed(() => {
  return (
    currentCamera.value?.accessStartTime ||
    currentCamera.value?.startTime ||
    '00:00'
  )
})

const accessEndTime = computed(() => {
  return (
    currentCamera.value?.accessEndTime ||
    currentCamera.value?.endTime ||
    '23:59'
  )
})

const remainingTimeText = computed(() => {
  const totalSeconds = Math.max(
    0,
    Math.floor(Number(remainingSeconds.value) || 0)
  )

  const days = Math.floor(totalSeconds / 86400)

  const hours = Math.floor(
    (totalSeconds % 86400) / 3600
  )

  const minutes = Math.floor(
    (totalSeconds % 3600) / 60
  )

  const seconds = totalSeconds % 60

  const timeText = [
    hours,
    minutes,
    seconds
  ]
    .map((value) => {
      return String(value).padStart(2, '0')
    })
    .join(':')

  if (days > 0) {
    return `${days}天 ${timeText}`
  }

  return timeText
})

const confidenceText = computed(() => {
  const confidence = analysisResult.value?.confidence

  if (
    confidence === undefined ||
    confidence === null
  ) {
    return '-'
  }

  const numberValue = Number(confidence)

  if (Number.isNaN(numberValue)) {
    return '-'
  }

  return `${Math.round(numberValue * 100)}%`
})

const currentAnswerModeLabel = computed(() => {
  return (
    answerModeOptions.find(
      (option) => option.value === answerMode.value
    )?.label || '最简化答案'
  )
})

async function initPage() {
  loading.value = true
  accessDenied.value = false
  accessMessage.value = ''

  captureError.value = ''
  analysisResult.value = null

  stopAccessCheckTimer()
  stopCountdown()

  try {
    if (!cameraId.value) {
      throw new Error('摄像头编号不能为空')
    }

    if (!cameraStore.cameraList.length) {
      await cameraStore.fetchCameras()
    }

    const accessResult =
      await cameraStore.checkAccess(cameraId.value)

    if (!accessResult?.allowed) {
      accessDenied.value = true

      accessMessage.value =
        accessResult?.message ||
        accessResult?.reason ||
        '当前时间段不允许访问该摄像头'

      return
    }

    await cameraStore.fetchStream(cameraId.value)

    const startTime =
      accessResult.accessStartTime ||
      accessResult.startTime ||
      accessStartTime.value

    const endTime =
      accessResult.accessEndTime ||
      accessResult.endTime ||
      accessEndTime.value

    /*
     * 这里一定不能使用：
     *
     * streamResult.expiresAt
     * cameraStore.expiresAt
     *
     * expiresAt 通常只是临时播放地址的过期时间，
     * 可能只有 5 分钟，并不是用户权限结束时间。
     */
    startCountdown({
      accessStartTime: startTime,
      accessEndTime: endTime,
      serverTime: accessResult.serverTime
    })

    startAccessCheckTimer()
  } catch (error) {
    console.error('摄像头页面初始化失败:', error)

    accessDenied.value = true

    accessMessage.value =
      error?.message ||
      '摄像头加载失败'
  } finally {
    loading.value = false
  }
}

function startCountdown({
  accessStartTime,
  accessEndTime,
  serverTime
}) {
  stopCountdown()

  const serverDate = parseDateTime(serverTime)

  const correctedServerNow =
    serverDate || new Date()

  serverTimeOffset =
    correctedServerNow.getTime() - Date.now()

  const endDate = resolveAccessEndDate({
    accessStartTime,
    accessEndTime,
    serverNow: correctedServerNow
  })

  if (!endDate) {
    console.error(
      '无法解析摄像头访问结束时间:',
      accessEndTime
    )

    remainingSeconds.value = 0
    return
  }

  accessEndTimestamp = endDate.getTime()

  updateRemainingTime()

  countdownTimer = window.setInterval(() => {
    updateRemainingTime()
  }, 1000)
}

function updateRemainingTime() {
  if (!accessEndTimestamp) {
    remainingSeconds.value = 0
    return
  }

  const correctedNow =
    Date.now() + serverTimeOffset

  const seconds = Math.max(
    0,
    Math.ceil(
      (accessEndTimestamp - correctedNow) / 1000
    )
  )

  remainingSeconds.value = seconds

  if (seconds <= 0) {
    handleAccessExpired()
  }
}

function resolveAccessEndDate({
  accessStartTime,
  accessEndTime,
  serverNow
}) {
  if (!accessEndTime) {
    return null
  }

  /*
   * 后端直接返回完整日期时间时直接使用。
   *
   * 支持：
   * 2026-07-13T23:59:00
   * 2026-07-13T23:59:00+08:00
   * 2026-07-13 23:59:00
   */
  if (
    /^\d{4}-\d{2}-\d{2}/.test(
      String(accessEndTime).trim()
    )
  ) {
    return parseDateTime(accessEndTime)
  }

  const endParts = parseTimeParts(accessEndTime)

  if (!endParts) {
    return null
  }

  const endDate = new Date(serverNow)

  endDate.setHours(
    endParts.hour,
    endParts.minute,
    endParts.second,
    0
  )

  const startParts =
    parseTimeParts(accessStartTime)

  if (!startParts) {
    return endDate
  }

  const startDate = new Date(serverNow)

  startDate.setHours(
    startParts.hour,
    startParts.minute,
    startParts.second,
    0
  )

  /*
   * 普通访问区间：
   * 00:00 - 23:59
   *
   * start < end，不需要跨天。
   *
   * 跨天访问区间：
   * 22:00 - 02:00
   *
   * start >= end。
   */
  const isCrossDay =
    endDate.getTime() <= startDate.getTime()

  if (!isCrossDay) {
    return endDate
  }

  /*
   * 当前时间位于开始时间之后：
   * 例如当前 23:00，区间 22:00 - 02:00，
   * 结束时间应该是第二天 02:00。
   */
  if (
    serverNow.getTime() >= startDate.getTime()
  ) {
    endDate.setDate(endDate.getDate() + 1)
  }

  /*
   * 当前时间位于凌晨：
   * 例如当前 01:00，区间 22:00 - 02:00，
   * 结束时间就是今天 02:00。
   */
  return endDate
}

function parseTimeParts(value) {
  if (!value) {
    return null
  }

  const text = String(value).trim()

  const match = text.match(
    /^(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?$/
  )

  if (!match) {
    return null
  }

  const hour = Number(match[1])
  const minute = Number(match[2])
  const second = Number(match[3] || 0)

  if (
    !Number.isInteger(hour) ||
    !Number.isInteger(minute) ||
    !Number.isInteger(second) ||
    hour < 0 ||
    hour > 23 ||
    minute < 0 ||
    minute > 59 ||
    second < 0 ||
    second > 59
  ) {
    return null
  }

  return {
    hour,
    minute,
    second
  }
}

function parseDateTime(value) {
  if (!value) {
    return null
  }

  const normalizedValue = String(value)
    .trim()
    .replace(' ', 'T')

  const date = new Date(normalizedValue)

  if (Number.isNaN(date.getTime())) {
    return null
  }

  return date
}

function stopCountdown() {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }

  accessEndTimestamp = 0
}

function handleAccessExpired() {
  stopCountdown()
  stopAccessCheckTimer()

  remainingSeconds.value = 0
  accessDenied.value = true
  accessMessage.value = '摄像头访问时间已结束'

  stopCameraStream()
}

function startAccessCheckTimer() {
  stopAccessCheckTimer()

  accessTimer = window.setInterval(async () => {
    try {
      const accessResult =
        await cameraStore.checkAccess(cameraId.value)

      if (!accessResult?.allowed) {
        accessDenied.value = true

        accessMessage.value =
          accessResult?.message ||
          accessResult?.reason ||
          '访问时间已结束'

        stopAccessCheckTimer()
        stopCountdown()
        stopCameraStream()

        return
      }

      /*
       * 每分钟重新获取服务器时间和权限结束时间，
       * 避免管理员修改权限后页面仍使用旧时间。
       */
      const startTime =
        accessResult.accessStartTime ||
        accessResult.startTime ||
        accessStartTime.value

      const endTime =
        accessResult.accessEndTime ||
        accessResult.endTime ||
        accessEndTime.value

      startCountdown({
        accessStartTime: startTime,
        accessEndTime: endTime,
        serverTime: accessResult.serverTime
      })
    } catch (error) {
      console.error(
        '定时检查摄像头访问权限失败:',
        error
      )
    }
  }, 60 * 1000)
}

function stopAccessCheckTimer() {
  if (accessTimer !== null) {
    window.clearInterval(accessTimer)
    accessTimer = null
  }
}

function stopCameraStream() {
  if (
    typeof cameraStore.stopStream === 'function'
  ) {
    cameraStore.stopStream()
  }
}

async function takeSnapshotAndAnalyze() {
  captureError.value = ''
  analysisLoading.value = true
  analysisResult.value = null

  try {
    const result =
      await analyzeCameraSnapshotApi(
        cameraId.value,
        answerMode.value
      )

    analysisResult.value =
      normalizeAnalysisResult(result)
  } catch (error) {
    console.error('AI 分析失败:', error)

    captureError.value =
      error?.message ||
      'AI 分析失败，请稍后重试'
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
      capturedAt:
        result.capturedAt ||
        new Date().toISOString(),

      confidence:
        result.confidence ?? 0,

      summary:
        result.summary ||
        '已完成试卷题目识别和答案生成。',

      questions: result.questions
    }
  }

  return {
    capturedAt:
      result.capturedAt ||
      new Date().toISOString(),

    confidence:
      result.confidence ?? 0,

    summary:
      result.summary ||
      '已完成试卷题目识别和答案生成。',

    questions: [
      {
        questionNo:
          result.questionNo || '第 1 题',

        questionType:
          result.questionType || '识别题目',

        questionTitle:
          result.questionTitle || '',

        simpleAnswer:
          result.simpleAnswer ||
          result.answer ||
          '暂无答案',

        balancedAnswer:
          result.balancedAnswer ||
          result.answer ||
          '暂无答案',

        bestAnswer:
          result.bestAnswer ||
          result.answer ||
          '暂无答案'
      }
    ]
  }
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

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
  stopCountdown()
  stopCameraStream()
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
  max-width: 1280px;
  margin: 0 auto 24px;
}

.back-link {
  margin: 0 0 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.back-link:hover {
  text-decoration: underline;
}

.page-header h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
}

.subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 15px;
  line-height: 1.6;
}

.access-status {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
  font-size: 13px;
  font-weight: 700;
}

.state-card {
  max-width: 1280px;
  margin: 0 auto;
  padding: 36px;
  border-radius: 18px;
  background: #ffffff;
  color: #374151;
  text-align: center;
  box-shadow:
    0 12px 30px rgba(15, 23, 42, 0.08);
}

.state-card.error {
  color: #991b1b;
  background: #fee2e2;
}

.state-card h2 {
  margin: 0 0 12px;
}

.state-card p {
  margin: 0 0 20px;
}

.back-button {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  background: #2563eb;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.content-layout {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr) 400px;
  gap: 24px;
  align-items: start;
  max-width: 1280px;
  margin: 0 auto;
}

.main-panel {
  min-width: 0;
}

.player-card {
  overflow: hidden;
  border-radius: 18px;
  background: #ffffff;
  box-shadow:
    0 18px 40px rgba(15, 23, 42, 0.12);
}

.player-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
}

.player-card-header h2 {
  margin: 0;
  color: #111827;
  font-size: 23px;
  font-weight: 800;
}

.player-card-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.remaining-time {
  flex-shrink: 0;
  padding: 7px 12px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.player-wrapper {
  margin: 0 24px 24px;
  overflow: hidden;
  border-radius: 14px;
  background: #1f1f1f;
}

.camera-info-card,
.analysis-panel {
  margin-top: 20px;
  padding: 22px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow:
    0 12px 30px rgba(15, 23, 42, 0.08);
}

.camera-info-card h2,
.analysis-panel h2 {
  margin: 0;
  color: #111827;
}

.info-grid {
  display: grid;
  grid-template-columns:
    120px minmax(0, 1fr);
  gap: 12px 16px;
  margin-top: 18px;
  color: #6b7280;
  font-size: 14px;
}

.info-grid strong {
  color: #111827;
  font-weight: 700;
  word-break: break-word;
}

.info-grid.compact {
  grid-template-columns:
    100px minmax(0, 1fr);
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
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

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panel-header h2 {
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
}

.panel-desc {
  margin: 0 0 18px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.8;
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

.answer-mode-selector {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: #f8fbff;
}

.answer-mode-label {
  display: block;
  margin-bottom: 12px;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.answer-mode-buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.answer-mode-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  color: #334155 !important;
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    "Microsoft YaHei",
    Arial,
    sans-serif;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  text-align: center;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.15s ease;
}

.answer-mode-button:hover:not(:disabled) {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8 !important;
}

.answer-mode-button:active:not(:disabled) {
  transform: scale(0.98);
}

.answer-mode-button.active,
.answer-mode-button.active:hover,
.answer-mode-button.active:focus,
.answer-mode-button.active:disabled {
  border-color: #2563eb !important;
  background: #2563eb !important;
  color: #ffffff !important;
  font-weight: 700;
  box-shadow:
    0 6px 14px rgba(37, 99, 235, 0.22);
}

.answer-mode-button:disabled {
  cursor: not-allowed;
}

.answer-mode-button:disabled:not(.active) {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #94a3b8 !important;
}

.answer-mode-tip {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.7;
}

.analyze-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 54px;
  padding: 13px 18px;
  border: none;
  border-radius: 15px;
  background: #2563eb;
  color: #ffffff !important;
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    "Microsoft YaHei",
    Arial,
    sans-serif;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
  text-align: center;
  cursor: pointer;
  box-shadow:
    0 8px 18px rgba(37, 99, 235, 0.2);
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.15s ease;
}

.analyze-button:hover:not(:disabled) {
  background: #1d4ed8;
  color: #ffffff !important;
  box-shadow:
    0 10px 22px rgba(37, 99, 235, 0.28);
}

.analyze-button:active:not(:disabled) {
  transform: scale(0.99);
}

.analyze-button:disabled {
  background: #93c5fd;
  color: #ffffff !important;
  cursor: not-allowed;
  box-shadow: none;
}

.error-text {
  margin: 14px 0 0;
  padding: 12px;
  border-radius: 12px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 14px;
  line-height: 1.6;
}

.analysis-content {
  margin-top: 18px;
}

.analysis-summary {
  margin: 0 0 14px;
  padding: 16px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1e3a8a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.8;
}

.question-answer-card {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
}

.question-answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-answer-header h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.4;
}

.question-answer-header span {
  padding: 5px 9px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
}

.question-title {
  margin: 12px 0 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
}

.answer-block {
  margin-top: 12px;
  padding: 14px;
  border-radius: 12px;
}

.answer-block h4 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.answer-block p {
  margin: 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.simple-answer {
  background: #f8fafc;
}

.balanced-answer {
  background: #eff6ff;
}

.best-answer {
  background: #f0fdf4;
}

@media (max-width: 1050px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .analysis-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 700px) {
  .camera-view-page {
    padding: 18px;
  }

  .page-header {
    flex-direction: column;
  }

  .page-header h1 {
    font-size: 26px;
  }

  .player-card-header {
    flex-direction: column;
  }

  .remaining-time {
    align-self: flex-start;
  }

  .content-layout {
    gap: 18px;
  }

  .analysis-panel {
    padding: 18px;
  }

  .answer-mode-buttons {
    grid-template-columns: 1fr;
  }

  .answer-mode-button {
    min-height: 46px;
    font-size: 15px;
  }

  .analyze-button {
    min-height: 54px;
    font-size: 15px;
  }

  .info-grid,
  .info-grid.compact {
    grid-template-columns: 1fr;
  }

  .question-answer-header {
    align-items: flex-start;
  }
}
</style>