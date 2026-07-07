<template>
  <div class="camera-player">
    <iframe
      v-if="streamUrl"
      class="camera-iframe"
      :src="streamUrl"
      allow="autoplay; fullscreen; microphone; camera"
      allowfullscreen
    ></iframe>

    <div v-else class="empty-player">
      暂无播放地址
    </div>
  </div>
</template>

<script setup>
defineProps({
  streamUrl: {
    type: String,
    required: true
  },
  streamType: {
    type: String,
    default: 'iframe'
  }
})
</script>

<style scoped>
.camera-player {
  width: 100%;
  height: 100%;
  min-height: 520px;
  background: #000;
  border-radius: 16px;
  overflow: hidden;
}

.camera-iframe {
  width: 100%;
  height: 100%;
  min-height: 520px;
  border: none;
  display: block;
  background: #000;
}

.empty-player {
  width: 100%;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #111;
}
</style>