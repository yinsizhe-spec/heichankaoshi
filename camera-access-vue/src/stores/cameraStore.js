import { defineStore } from 'pinia'
import {
  getCameraListApi,
  checkCameraAccessApi,
  getCameraStreamApi
} from '../api/camera'

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
      message: '',
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
      return this.cameraList.find(
        (item) =>
          item.cameraId === cameraId ||
          String(item.id) === String(cameraId)
      )
    },

    async checkAccess(cameraId) {
      const result =
        await checkCameraAccessApi(cameraId)

      this.accessStatus = {
        allowed: Boolean(result.allowed),
        reason: result.reason || '',
        message: result.message || '',
        startTime: result.accessStartTime || '',
        endTime: result.accessEndTime || '',
        serverTime: result.serverTime || ''
      }

      return result
    },

    async fetchStream(cameraId) {
      const result =
        await getCameraStreamApi(cameraId)

      this.streamUrl = result.streamUrl || ''
      this.streamType = String(
        result.streamType || ''
      ).toLowerCase()
      this.expiresAt = result.expiresAt || ''

      return result
    },

    stopStream() {
      this.streamUrl = ''
      this.streamType = ''
      this.expiresAt = ''
    },

    resetAccessStatus() {
      this.accessStatus = {
        allowed: false,
        reason: '',
        message: '',
        startTime: '',
        endTime: '',
        serverTime: ''
      }
    }
  }
})