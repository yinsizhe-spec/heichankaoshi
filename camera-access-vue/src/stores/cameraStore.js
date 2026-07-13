import { defineStore } from 'pinia'

import {
  getCameraListApi,
  checkCameraAccessApi,
  getCameraStreamApi
} from '../api/camera'

export const useCameraStore = defineStore(
  'camera',
  {
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
        validFrom: '',
        validUntil: '',
        serverTime: ''
      },

      loading: false
    }),

    actions: {
      async fetchCameras() {
        this.loading = true

        try {
          this.cameraList =
            await getCameraListApi()

          return this.cameraList
        } finally {
          this.loading = false
        }
      },

      findCamera(cameraId) {
        return this.cameraList.find(
          (item) =>
            String(item.cameraId) ===
            String(cameraId)
        )
      },

      async checkAccess(cameraId) {
        const result =
          await checkCameraAccessApi(
            cameraId
          )

        this.accessStatus = {
          allowed:
            Boolean(result.allowed),

          reason:
            result.reason || '',

          message:
            result.message || '',

          startTime:
            result.accessStartTime || '',

          endTime:
            result.accessEndTime || '',

          validFrom:
            result.validFrom || '',

          validUntil:
            result.validUntil || '',

          serverTime:
            result.serverTime || ''
        }

        return result
      },

      async fetchStream(cameraId) {
        const result =
          await getCameraStreamApi(
            cameraId
          )

        this.streamUrl =
          result.streamUrl || ''

        this.streamType = String(
          result.streamType || ''
        ).toLowerCase()

        this.expiresAt =
          result.expiresAt || ''

        return result
      },

      setCurrentCamera(camera) {
        this.currentCamera =
          camera || null
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
          validFrom: '',
          validUntil: '',
          serverTime: ''
        }
      },

      resetCameraState() {
        this.currentCamera = null
        this.stopStream()
        this.resetAccessStatus()
      }
    }
  }
)