<template>
  <div class="player-box">
    <iframe
      v-if="streamUrl"
      class="camera-iframe"
      :src="streamUrl"
      allow="autoplay; fullscreen; microphone; camera"
      allowfullscreen
    ></iframe>

    <div v-else class="player-placeholder">
      等待加载视频流
    </div>

    <div class="player-mode">
      WebRTC 低延迟模式
    </div>
  </div>
</template>

<script setup>
defineProps({
  streamUrl: {
    type: String,
    default: ''
  },
  streamType: {
    type: String,
    default: 'iframe'
  }
})

/**
 * 注意：
 * iframe 播放的是 MediaMTX 自带的 WebRTC 页面。
 * 浏览器安全机制不允许父页面直接截图 iframe 内部的视频画面。
 *
 * 这里返回一个 1x1 的占位图片，目的是：
 * 1. 保证 CameraViewPage.vue 里的 AI 分析按钮不会报错
 * 2. 继续使用 mock AI 分析返回多题答案
 *
 * 如果后面要做真实识别，建议改成：
 * 前端点击 AI 分析 -> 后端从 RTMP / HLS / WebRTC 流截图 -> 调用 AI -> 返回真实答案
 */
function captureFrame() {
  return 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII='
}

defineExpose({
  captureFrame
})
</script>

<style scoped>
.player-box {
  position: relative;
  width: 100%;
  min-height: 520px;
  background: #000000;
  border-radius: 16px;
  overflow: hidden;
}

.camera-iframe {
  width: 100%;
  min-height: 520px;
  display: block;
  border: none;
  background: #000000;
}

.player-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
  background: #111827;
}

.player-mode {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 5;
  padding: 6px 10px;
  border-radius: 999px;
  color: #ffffff;
  background: rgba(37, 99, 235, 0.85);
  font-size: 12px;
  font-weight: 700;
}
</style>