<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑摄像头' : '新建摄像头'"
    width="720px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="115px">
      <div class="form-grid">
        <el-form-item label="摄像头编码" prop="cameraCode">
          <el-input v-model="form.cameraCode" />
        </el-form-item>
        <el-form-item label="摄像头名称" prop="cameraName">
          <el-input v-model="form.cameraName" />
        </el-form-item>
        <el-form-item label="安装位置" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="播放类型" prop="streamType">
          <el-select v-model="form.streamType" style="width: 100%">
            <el-option label="HLS" value="HLS" />
            <el-option label="IFRAME" value="IFRAME" />
            <el-option label="WebRTC" value="WEBRTC" />
            <el-option label="MJPEG" value="MJPEG" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="播放地址" prop="streamSourceUrl">
        <el-input
          v-model="form.streamSourceUrl"
          placeholder="例如：http://server:8888/live/desktop/index.m3u8"
        />
      </el-form-item>

      <el-form-item label="截图源地址">
        <el-input
          v-model="form.sourceStreamUrl"
          placeholder="例如：rtsp://server:8554/live/desktop"
        />
      </el-form-item>

      <el-form-item label="摄像头状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio value="ONLINE">上线</el-radio>
          <el-radio value="OFFLINE">下线</el-radio>
          <el-radio value="MAINTENANCE">维护中</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="摄像头说明">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'

const emit = defineEmits(['save'])
const visible = ref(false)
const saving = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: null,
  cameraCode: '',
  cameraName: '',
  location: '',
  streamType: 'HLS',
  streamSourceUrl: '',
  sourceStreamUrl: '',
  snapshotUrl: '',
  description: '',
  status: 'OFFLINE'
})

const form = reactive(defaultForm())

const rules = {
  cameraCode: [{ required: true, message: '请输入摄像头编码', trigger: 'blur' }],
  cameraName: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入安装位置', trigger: 'blur' }],
  streamType: [{ required: true, message: '请选择播放类型', trigger: 'change' }],
  streamSourceUrl: [{ required: true, message: '请输入播放地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择摄像头状态', trigger: 'change' }]
}

function open(row = null) {
  Object.assign(form, defaultForm(), row || {})
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await new Promise((resolve, reject) => {
      emit('save', { ...form }, resolve, reject)
    })
    visible.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}
</style>
