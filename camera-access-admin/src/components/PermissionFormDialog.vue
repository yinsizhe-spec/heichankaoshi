<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑访问权限' : '新建访问权限'"
    width="650px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="115px">
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="form.userId"
          filterable
          style="width: 100%"
          @change="syncUser"
        >
          <el-option
            v-for="item in users"
            :key="item.id"
            :label="`${item.displayName} (${item.username})`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="摄像头" prop="cameraId">
        <el-select
          v-model="form.cameraId"
          filterable
          style="width: 100%"
          @change="syncCamera"
        >
          <el-option
            v-for="item in cameras"
            :key="item.id"
            :label="`${item.cameraName} - ${item.location}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="每日访问时间">
        <el-time-picker
          v-model="timeRange"
          is-range
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="HH:mm"
          format="HH:mm"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="授权有效日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="允许AI分析">
        <el-switch v-model="form.aiAnalysisAllowed" />
      </el-form-item>
      <el-form-item label="权限状态">
        <el-radio-group v-model="form.status">
          <el-radio value="ACTIVE">启用</el-radio>
          <el-radio value="DISABLED">禁用</el-radio>
        </el-radio-group>
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
import { computed, reactive, ref } from 'vue'

const props = defineProps({
  users: { type: Array, default: () => [] },
  cameras: { type: Array, default: () => [] }
})

const emit = defineEmits(['save'])
const visible = ref(false)
const saving = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: null,
  userId: null,
  username: '',
  displayName: '',
  cameraId: null,
  cameraName: '',
  accessStartTime: '08:00',
  accessEndTime: '18:00',
  validFrom: '',
  validUntil: '',
  aiAnalysisAllowed: true,
  status: 'ACTIVE'
})

const form = reactive(defaultForm())

const timeRange = computed({
  get: () => [form.accessStartTime, form.accessEndTime],
  set: (value) => {
    form.accessStartTime = value?.[0] || ''
    form.accessEndTime = value?.[1] || ''
  }
})

const dateRange = computed({
  get: () =>
    form.validFrom && form.validUntil ? [form.validFrom, form.validUntil] : [],
  set: (value) => {
    form.validFrom = value?.[0] || ''
    form.validUntil = value?.[1] || ''
  }
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  cameraId: [{ required: true, message: '请选择摄像头', trigger: 'change' }]
}

function syncUser(id) {
  const user = props.users.find((item) => item.id === id)
  form.username = user?.username || ''
  form.displayName = user?.displayName || ''
}

function syncCamera(id) {
  const camera = props.cameras.find((item) => item.id === id)
  form.cameraName = camera?.cameraName || ''
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
