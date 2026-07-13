<template>
  <div>
    <AppHeader />

    <main class="container camera-page">
      <div class="page-header">
        <div>
          <button
            type="button"
            class="back-button"
            @click="goBack"
          >
            ← 返回摄像头列表
          </button>

          <h1>
            {{
              currentCamera?.cameraName ||
              streamResult?.cameraName ||
              '摄像头画面'
            }}
          </h1>

          <p class="muted">
            {{
              currentCamera?.location ||
              '正在加载摄像头信息'
            }}
          </p>
        </div>

        <span
          class="status-badge"
          :class="access.allowed
            ? 'status-badge--allowed'
            : 'status-badge--denied'"
        >
          {{
            access.allowed
              ? '当前可以访问'
              : '当前不可访问'
          }}
        </span>
      </div>

      <div
        v-if="pageLoading"
        class="state-card"
      >
        正在校验摄像头访问权限...
      </div>

      <div
        v-else-if="pageError"
        class="state-card state-card--error"
      >
        <h2>无法打开摄像头</h2>

        <p>{{ pageError }}</p>

        <button
          type="button"
          class="secondary-button"
          @click="initializePage"
        >
          重新尝试
        </button>
      </div>

      <template v-else>
        <section
          v-if="!access.allowed"
          class="state-card state-card--warning"
        >
          <h2>当前无法访问该摄像头</h2>

          <p>
            {{
              access.message ||
              '当前不在允许访问时间内'
            }}
          </p>

          <div class="access-time">
            <span>每日访问时间：</span>

            <strong>
              {{
                formatTime(
                  access.startTime
                )
              }}
              -
              {{
                formatTime(
                  access.endTime
                )
              }}
            </strong>
          </div>
        </section>

        <template v-else>
          <section class="camera-panel">
            <div class="camera-panel__header">
              <div>
                <h2>实时摄像头画面</h2>

                <p class="muted">
                  访问时间：
                  {{
                    formatTime(
                      access.startTime
                    )
                  }}
                  -
                  {{
                    formatTime(
                      access.endTime
                    )
                  }}
                </p>
              </div>

              <span
                v-if="remainingText"
                class="remaining-time"
              >
                剩余时间：
                {{ remainingText }}
              </span>
            </div>

            <div class="player-container">
              <iframe
                v-if="
                  streamType === 'iframe' &&
                  streamUrl
                "
                :src="streamUrl"
                class="camera-iframe"
                title="摄像头实时画面"
                allow="autoplay; fullscreen"
                allowfullscreen
              />

              <video
                v-else-if="
                  streamType === 'hls' &&
                  streamUrl
                "
                :src="streamUrl"
                class="camera-video"
                controls
                autoplay
                muted
                playsinline
              />

              <div
                v-else
                class="player-empty"
              >
                暂无可播放的视频地址
              </div>
            </div>
          </section>

          <section class="analysis-panel">
            <div class="analysis-panel__header">
              <div>
                <h2>AI 拍照搜题</h2>

                <p class="muted">
                  后端将从摄像头实时视频流中截取当前画面并进行分析
                </p>
              </div>

              <div class="analysis-actions">
                <select
                  v-model="answerMode"
                  class="mode-select"
                  :disabled="analysisLoading"
                >
                  <option value="simple">
                    简单答案
                  </option>

                  <option value="balanced">
                    答案和简要解析
                  </option>

                  <option value="best">
                    完整详细解析
                  </option>
                </select>

                <button
                  type="button"
                  class="analyze-button"
                  :disabled="
                    analysisLoading ||
                    !access.allowed
                  "
                  @click="handleAnalyze"
                >
                  {{
                    analysisLoading
                      ? '正在截图并分析...'
                      : 'AI 拍照搜题'
                  }}
                </button>
              </div>
            </div>

            <div
              v-if="analysisLoading"
              class="analysis-loading"
            >
              <div class="loading-spinner"></div>

              <div>
                <strong>
                  AI 正在分析当前画面
                </strong>

                <p>
                  正在从视频流截图并识别题目，可能需要几十秒，请不要关闭页面。
                </p>
              </div>
            </div>

            <div
              v-else-if="analysisError"
              class="analysis-error"
            >
              {{ analysisError }}
            </div>

            <article
              v-else-if="analysisResult"
              class="analysis-result"
            >
              <div class="result-header">
                <div>
                  <span class="question-number">
                    {{
                      analysisResult.questionNo ||
                      '未识别题号'
                    }}
                  </span>

                  <h3>
                    {{
                      analysisResult.questionTitle ||
                      'AI 搜题结果'
                    }}
                  </h3>
                </div>

                <span class="confidence">
                  置信度：
                  {{
                    formatConfidence(
                      analysisResult.confidence
                    )
                  }}
                </span>
              </div>

              <div class="result-section">
                <h4>识别出的题目</h4>

                <p class="recognized-text">
                  {{
                    analysisResult.recognizedText ||
                    '未识别到完整题目文字'
                  }}
                </p>
              </div>

              <div class="answer-grid">
                <div class="answer-card">
                  <span class="answer-label">
                    直接答案
                  </span>

                  <p>
                    {{
                      analysisResult.simpleAnswer ||
                      '暂无答案'
                    }}
                  </p>
                </div>

                <div class="answer-card">
                  <span class="answer-label">
                    简要解析
                  </span>

                  <p>
                    {{
                      analysisResult.balancedAnswer ||
                      '暂无简要解析'
                    }}
                  </p>
                </div>
              </div>

              <div class="result-section best-answer">
                <h4>完整解析</h4>

                <p>
                  {{
                    analysisResult.bestAnswer ||
                    '暂无完整解析'
                  }}
                </p>
              </div>

              <div class="result-footer">
                <span>
                  题目类型：
                  {{
                    formatQuestionType(
                      analysisResult.questionType
                    )
                  }}
                </span>

                <span>
                  分析时间：
                  {{
                    formatDateTime(
                      analysisResult.capturedAt
                    )
                  }}
                </span>
              </div>
            </article>

            <div
              v-else
              class="analysis-empty"
            >
              点击“AI 拍照搜题”，系统会从当前摄像头画面中识别题目。
            </div>
          </section>
        </template>
      </template>
    </main>
  </div>
</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref
} from 'vue'

import {
  useRoute,
  useRouter
} from 'vue-router'

import AppHeader from '../components/AppHeader.vue'

import {
  analyzeCameraSnapshotApi
} from '../api/camera'

import {
  useCameraStore
} from '../stores/cameraStore'

const route = useRoute()
const router = useRouter()

const cameraStore = useCameraStore()

const pageLoading = ref(false)
const pageError = ref('')

const streamResult = ref(null)

const answerMode = ref('best')

const analysisLoading = ref(false)
const analysisError = ref('')
const analysisResult = ref(null)

const remainingSeconds = ref(0)

let countdownTimer = null

const cameraId = computed(() => {
  return String(
    route.params.cameraId || ''
  )
})

const currentCamera = computed(() => {
  return (
    cameraStore.findCamera(
      cameraId.value
    ) ||
    cameraStore.currentCamera
  )
})

const access = computed(() => {
  return cameraStore.accessStatus
})

const streamUrl = computed(() => {
  return cameraStore.streamUrl
})

const streamType = computed(() => {
  return String(
    cameraStore.streamType || ''
  ).toLowerCase()
})

const remainingText = computed(() => {
  if (
    !remainingSeconds.value ||
    remainingSeconds.value <= 0
  ) {
    return ''
  }

  const hours = Math.floor(
    remainingSeconds.value / 3600
  )

  const minutes = Math.floor(
    (
      remainingSeconds.value % 3600
    ) / 60
  )

  const seconds =
    remainingSeconds.value % 60

  return [
    hours,
    minutes,
    seconds
  ]
    .map((value) =>
      String(value).padStart(2, '0')
    )
    .join(':')
})

async function initializePage() {
  pageLoading.value = true
  pageError.value = ''

  analysisError.value = ''
  analysisResult.value = null

  stopCountdown()
  cameraStore.stopStream()
  cameraStore.resetAccessStatus()

  try {
    if (!cameraId.value) {
      throw new Error(
        '摄像头编号不能为空'
      )
    }

    if (
      cameraStore.cameraList.length === 0
    ) {
      await cameraStore.fetchCameras()
    }

    const camera =
      cameraStore.findCamera(
        cameraId.value
      )

    cameraStore.setCurrentCamera(
      camera || null
    )

    const accessResult =
      await cameraStore.checkAccess(
        cameraId.value
      )

    if (!accessResult.allowed) {
      return
    }

    streamResult.value =
      await cameraStore.fetchStream(
        cameraId.value
      )

    startCountdown(
      streamResult.value?.expiresAt,
      accessResult.accessEndTime,
      accessResult.serverTime
    )
  } catch (error) {
    pageError.value =
      error?.message ||
      '摄像头页面加载失败'
  } finally {
    pageLoading.value = false
  }
}

async function handleAnalyze() {
  if (analysisLoading.value) {
    return
  }

  analysisLoading.value = true
  analysisError.value = ''
  analysisResult.value = null

  try {
    analysisResult.value =
      await analyzeCameraSnapshotApi(
        cameraId.value,
        answerMode.value
      )
  } catch (error) {
    analysisError.value =
      error?.message ||
      'AI 搜题失败，请稍后重试'
  } finally {
    analysisLoading.value = false
  }
}

function startCountdown(
  expiresAt,
  accessEndTime,
  serverTime
) {
  stopCountdown()

  let endTimestamp = 0

  if (expiresAt) {
    const expiresDate =
      new Date(expiresAt)

    if (
      !Number.isNaN(
        expiresDate.getTime()
      )
    ) {
      endTimestamp =
        expiresDate.getTime()
    }
  }

  if (
    !endTimestamp &&
    accessEndTime
  ) {
    const baseTime = serverTime
      ? new Date(serverTime)
      : new Date()

    const parts = String(
      accessEndTime
    )
      .split(':')
      .map(Number)

    const endDate = new Date(baseTime)

    endDate.setHours(
      parts[0] || 0,
      parts[1] || 0,
      parts[2] || 0,
      0
    )

    if (
      endDate.getTime() <=
      baseTime.getTime()
    ) {
      endDate.setDate(
        endDate.getDate() + 1
      )
    }

    endTimestamp =
      endDate.getTime()
  }

  if (!endTimestamp) {
    return
  }

  const updateRemaining = () => {
    const seconds = Math.max(
      0,
      Math.floor(
        (
          endTimestamp -
          Date.now()
        ) / 1000
      )
    )

    remainingSeconds.value =
      seconds

    if (seconds <= 0) {
      stopCountdown()
      cameraStore.stopStream()

      pageError.value =
        '摄像头访问时间已结束，视频播放已停止'
    }
  }

  updateRemaining()

  countdownTimer = window.setInterval(
    updateRemaining,
    1000
  )
}

function stopCountdown() {
  if (countdownTimer) {
    window.clearInterval(
      countdownTimer
    )

    countdownTimer = null
  }

  remainingSeconds.value = 0
}

function formatTime(value) {
  if (!value) {
    return '--:--'
  }

  return String(value).substring(0, 5)
}

function formatConfidence(value) {
  const number = Number(value)

  if (Number.isNaN(number)) {
    return '未知'
  }

  return `${Math.round(number * 100)}%`
}

function formatQuestionType(value) {
  const typeMap = {
    single_choice: '单选题',
    multiple_choice: '多选题',
    true_false: '判断题',
    calculation: '计算题',
    short_answer: '简答题',
    unknown: '未知题型'
  }

  return (
    typeMap[value] ||
    value ||
    '未知题型'
  )
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)

  if (
    Number.isNaN(date.getTime())
  ) {
    return value
  }

  return date.toLocaleString(
    'zh-CN',
    {
      hour12: false
    }
  )
}

function goBack() {
  router.push('/dashboard')
}

onMounted(() => {
  initializePage()
})

onBeforeUnmount(() => {
  stopCountdown()
  cameraStore.stopStream()
})
</script>

<style scoped>
.camera-page {
  padding-bottom: 48px;
}

.page-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 10px 0 0;
  color: #172033;
}

.muted {
  margin: 6px 0 0;
  color: #7c879b;
}

.back-button {
  padding: 0;
  color: #2563eb;
  font-size: 14px;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.status-badge {
  flex-shrink: 0;
  padding: 7px 12px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 999px;
}

.status-badge--allowed {
  color: #15803d;
  background: #ecfdf3;
}

.status-badge--denied {
  color: #b45309;
  background: #fff7ed;
}

.state-card,
.camera-panel,
.analysis-panel {
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow:
    0 10px 28px
    rgb(15 23 42 / 6%);
}

.state-card--error {
  color: #b91c1c;
}

.state-card--warning {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}

.access-time {
  margin-top: 16px;
}

.secondary-button {
  margin-top: 16px;
  padding: 10px 18px;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}

.camera-panel {
  margin-bottom: 24px;
}

.camera-panel__header,
.analysis-panel__header {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.camera-panel__header h2,
.analysis-panel__header h2 {
  margin: 0;
  color: #172033;
}

.remaining-time {
  padding: 7px 11px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  background: #eff6ff;
  border-radius: 999px;
}

.player-container {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #050a14;
  border-radius: 14px;
  aspect-ratio: 16 / 9;
}

.camera-iframe,
.camera-video {
  width: 100%;
  height: 100%;
  border: 0;
}

.camera-video {
  object-fit: contain;
}

.player-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #cbd5e1;
}

.analysis-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.mode-select {
  min-height: 42px;
  padding: 0 12px;
  color: #263247;
  background: #ffffff;
  border: 1px solid #dbe2ea;
  border-radius: 10px;
}

.analyze-button {
  min-height: 42px;
  padding: 0 18px;
  color: #ffffff;
  font-weight: 600;
  cursor: pointer;
  background: #2563eb;
  border: 0;
  border-radius: 10px;
}

.analyze-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.analysis-loading {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 24px;
  background: #f8fafc;
  border-radius: 12px;
}

.analysis-loading p {
  margin: 5px 0 0;
  color: #64748b;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #dbeafe;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.analysis-error {
  padding: 16px;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 10px;
}

.analysis-empty {
  padding: 32px;
  color: #7c879b;
  text-align: center;
  background: #f8fafc;
  border-radius: 12px;
}

.analysis-result {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-header {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
}

.result-header h3 {
  margin: 8px 0 0;
  color: #172033;
}

.question-number {
  display: inline-flex;
  padding: 5px 9px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  background: #eff6ff;
  border-radius: 8px;
}

.confidence {
  color: #15803d;
  font-size: 14px;
  font-weight: 600;
}

.result-section,
.answer-card {
  padding: 18px;
  background: #f8fafc;
  border-radius: 12px;
}

.result-section h4 {
  margin: 0 0 10px;
  color: #172033;
}

.result-section p,
.answer-card p {
  margin: 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.recognized-text {
  white-space: pre-wrap;
}

.answer-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.answer-label {
  display: block;
  margin-bottom: 10px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.best-answer {
  background: #eff6ff;
}

.result-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #64748b;
  font-size: 13px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .page-header,
  .camera-panel__header,
  .analysis-panel__header {
    flex-direction: column;
  }

  .analysis-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .answer-grid {
    grid-template-columns: 1fr;
  }

  .state-card,
  .camera-panel,
  .analysis-panel {
    padding: 18px;
  }
}
</style>