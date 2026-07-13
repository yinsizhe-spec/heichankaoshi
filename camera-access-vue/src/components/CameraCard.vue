<template>
  <article class="camera-card">
    <div class="camera-card__header">
      <div>
        <h3 class="camera-card__title">
          {{ camera.cameraName || '未命名摄像头' }}
        </h3>

        <p class="camera-card__location">
          {{ camera.location || '未知位置' }}
        </p>
      </div>

      <div
        class="status-badge"
        :class="isOnline ? 'status-badge--online' : 'status-badge--offline'"
      >
        <span class="status-dot"></span>
        {{ isOnline ? '在线' : '离线' }}
      </div>
    </div>

    <div class="camera-card__content">
      <div class="info-row">
        <span class="info-label">摄像头编号</span>
        <span class="info-value">
          {{ camera.cameraId || '-' }}
        </span>
      </div>

      <div class="info-row">
        <span class="info-label">授权日期</span>
        <span class="info-value">
          {{ formatDateRange(camera.validFrom, camera.validUntil) }}
        </span>
      </div>

      <div class="info-row">
        <span class="info-label">访问时间</span>
        <span class="info-value">
          {{ formatAccessTime(camera.accessStartTime, camera.accessEndTime) }}
        </span>
      </div>

      <div class="info-row">
        <span class="info-label">当前权限</span>

        <span
          class="access-status"
          :class="canAccess ? 'access-status--allowed' : 'access-status--denied'"
        >
          {{ accessText }}
        </span>
      </div>
    </div>

    <div class="camera-card__footer">
      <p
        v-if="!canAccess"
        class="access-message"
      >
        {{ unavailableMessage }}
      </p>

      <button
        type="button"
        class="view-button"
        :disabled="!canAccess"
        @click="handleView"
      >
        {{ buttonText }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  camera: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['view'])

const normalizedStatus = computed(() => {
  return String(props.camera.status || '').toLowerCase()
})

const isOnline = computed(() => {
  return normalizedStatus.value === 'online'
})

const canAccess = computed(() => {
  return isOnline.value && Boolean(props.camera.canAccessNow)
})

const accessText = computed(() => {
  if (!isOnline.value) {
    return '摄像头离线'
  }

  if (props.camera.canAccessNow) {
    return '已授权'
  }

  return '当前不可访问'
})

const unavailableMessage = computed(() => {
  if (!isOnline.value) {
    return '摄像头当前离线，请稍后重试'
  }

  return (
    props.camera.accessMessage ||
    '当前不在允许访问时间内'
  )
})

const buttonText = computed(() => {
  if (!isOnline.value) {
    return '摄像头离线'
  }

  if (!props.camera.canAccessNow) {
    return '当前不可访问'
  }

  return '查看摄像头'
})

function formatChineseDate(dateValue) {
  if (!dateValue) {
    return '长期有效'
  }

  const dateText = String(dateValue).substring(0, 10)
  const parts = dateText.split('-')

  if (parts.length !== 3) {
    return dateValue
  }

  const year = Number(parts[0])
  const month = Number(parts[1])
  const day = Number(parts[2])

  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day)
  ) {
    return dateValue
  }

  return `${year}年${month}月${day}日`
}

function formatDateRange(validFrom, validUntil) {
  if (!validFrom && !validUntil) {
    return '长期有效'
  }

  if (validFrom && !validUntil) {
    return `${formatChineseDate(validFrom)} 起长期有效`
  }

  if (!validFrom && validUntil) {
    return `有效期至 ${formatChineseDate(validUntil)}`
  }

  return `${formatChineseDate(validFrom)} 至 ${formatChineseDate(validUntil)}`
}

function formatAccessTime(startTime, endTime) {
  const start = formatTime(startTime)
  const end = formatTime(endTime)

  if (!startTime || !endTime) {
    return '未配置'
  }

  return `${start} - ${end}`
}

function formatTime(timeValue) {
  if (!timeValue) {
    return '--:--'
  }

  return String(timeValue).substring(0, 5)
}

function handleView() {
  if (!canAccess.value) {
    return
  }

  emit('view', props.camera.cameraId)
}
</script>

<style scoped>
.camera-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 22px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 10px 28px rgb(15 23 42 / 7%);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.camera-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 36px rgb(15 23 42 / 11%);
}

.camera-card__header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
}

.camera-card__title {
  margin: 0;
  color: #172033;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.4;
}

.camera-card__location {
  margin: 6px 0 0;
  color: #7c879b;
  font-size: 14px;
}

.status-badge {
  display: inline-flex;
  flex-shrink: 0;
  gap: 7px;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-badge--online {
  color: #15803d;
  background: #ecfdf3;
}

.status-badge--online .status-dot {
  background: #22c55e;
  box-shadow: 0 0 0 4px rgb(34 197 94 / 14%);
}

.status-badge--offline {
  color: #64748b;
  background: #f1f5f9;
}

.status-badge--offline .status-dot {
  background: #94a3b8;
}

.camera-card__content {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 0;
}

.info-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 14px;
  align-items: flex-start;
}

.info-label {
  color: #7c879b;
  font-size: 14px;
}

.info-value {
  overflow-wrap: anywhere;
  color: #263247;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
}

.access-status {
  width: fit-content;
  padding: 4px 9px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
}

.access-status--allowed {
  color: #15803d;
  background: #ecfdf3;
}

.access-status--denied {
  color: #b45309;
  background: #fff7ed;
}

.camera-card__footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: auto;
}

.access-message {
  min-height: 20px;
  margin: 0;
  color: #b45309;
  font-size: 13px;
  line-height: 1.5;
}

.view-button {
  width: 100%;
  min-height: 44px;
  padding: 10px 18px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: #2563eb;
  border: 0;
  border-radius: 11px;
  transition:
    background 0.2s ease,
    transform 0.2s ease;
}

.view-button:not(:disabled):hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.view-button:disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: #e8edf4;
}

@media (max-width: 640px) {
  .camera-card {
    padding: 18px;
  }

  .camera-card__header {
    flex-direction: column;
  }

  .info-row {
    grid-template-columns: 1fr;
    gap: 5px;
  }
}
</style>