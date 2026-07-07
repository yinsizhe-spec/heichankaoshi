import { defineStore } from 'pinia'
import { getCameraListApi, checkCameraAccessApi, getCameraStreamApi } from '../api/camera'

export const useCameraStore = defineStore('camera', {
  state: () => ({
    cameraList: [],
    currentCamera: null,
    streamUrl: '',
    streamType: '',
    expiresAt: '',
    accessStatus: {
      allowed: false,
      reason: '',
      startTime: '',
      endTime: '',
      serverTime: ''
    },
    loading: false
  }),
  actions: {
    async fetchCameras() {
      this.loading = true
      try {
        this.cameraList = await getCameraListApi()
        return this.cameraList
      } finally {
        this.loading = false
      }
    },
    findCamera(cameraId) {
      return this.cameraList.find((item) => item.cameraId === cameraId)
    },
    async checkAccess(cameraId) {
      const result = await checkCameraAccessApi(cameraId)
      this.accessStatus = {
        allowed: result.allowed,
        reason: result.message || '',
        startTime: result.accessStartTime,
        endTime: result.accessEndTime,
        serverTime: result.serverTime
      }
      return result
    },
    async fetchStream(cameraId) {
      const result = await getCameraStreamApi(cameraId)
      this.streamUrl = result.streamUrl
      this.streamType = result.streamType
      this.expiresAt = result.expiresAt
      return result
    },
    stopStream() {
      this.streamUrl = ''
      this.streamType = ''
      this.expiresAt = ''
    }
  }
})
