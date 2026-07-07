<template>
  <div class="player-box">
    <video ref="videoRef" controls autoplay muted playsinline></video>
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

watch(() => props.streamUrl, loadStream, { immediate: true })
onBeforeUnmount(destroyHls)
</script>
