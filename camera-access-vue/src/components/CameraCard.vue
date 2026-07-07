<template>
  <article class="camera-card">
    <div class="card-top">
      <div>
        <h3>{{ camera.cameraName }}</h3>
        <p class="muted">{{ camera.location }}</p>
      </div>
      <AccessTimeBadge :allowed="camera.canAccessNow && camera.status === 'online'" />
    </div>

    <div class="info-grid">
      <span>状态</span><strong :class="camera.status === 'online' ? 'ok' : 'bad'">{{ statusText }}</strong>
      <span>允许访问</span><strong>{{ camera.accessStartTime }} - {{ camera.accessEndTime }}</strong>
      <span>当前状态</span><strong>{{ camera.canAccessNow ? '当前在允许时间内' : '当前不在允许访问时间内' }}</strong>
    </div>

    <button class="primary full" :disabled="!canOpen" @click="$emit('open', camera.cameraId)">
      {{ canOpen ? '查看摄像头' : disabledText }}
    </button>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import AccessTimeBadge from './AccessTimeBadge.vue'

const props = defineProps({
  camera: { type: Object, required: true }
})

defineEmits(['open'])

const canOpen = computed(() => props.camera.canAccessNow && props.camera.status === 'online')
const statusText = computed(() => props.camera.status === 'online' ? '在线' : '离线')
const disabledText = computed(() => props.camera.status !== 'online' ? '摄像头离线' : '当前不可访问')
</script>
