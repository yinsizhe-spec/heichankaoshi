<template>
  <div class="player-box">
    <video ref="videoRef" controls autoplay muted playsinline crossorigin="anonymous"></video>
    <div v-if="!streamUrl" class="player-placeholder">等待加载视频流</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import Hls from 'hls.js'

const props = defineProps({
  streamUrl: { type: String, default: '' },
  streamType: { type: String, default: '' }
})

const videoRef = ref(null)
let hls = null

function destroyHls() {
  if (hls) {
    hls.destroy()
    hls = null
  }
}

function loadStream() {
  destroyHls()
  const video = videoRef.value
  if (!video || !props.streamUrl) return

  if (props.streamType === 'hls' || props.streamUrl.includes('.m3u8')) {
    if (Hls.isSupported()) {
      hls = new Hls()
      hls.loadSource(props.streamUrl)
      hls.attachMedia(video)
      return
    }
  }
  video.src = props.streamUrl
}

function captureFrame() {
  const video = videoRef.value
  if (!video || !props.streamUrl) {
    throw new Error('视频流尚未加载，无法拍照')
  }
  if (!video.videoWidth || !video.videoHeight) {
    throw new Error('视频画面尚未准备好，请稍后再拍照')
  }

  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/jpeg', 0.9)
}

watch(() => props.streamUrl, loadStream, { immediate: true })
onBeforeUnmount(destroyHls)

defineExpose({ captureFrame })
</script>
