<template>
  <span class="badge" :class="allowed ? 'badge-success' : 'badge-danger'">
    {{ allowed ? '可访问' : '不可访问' }}
  </span>
</template>

<script setup>
defineProps({
  allowed: { type: Boolean, default: false }
})
</script>
