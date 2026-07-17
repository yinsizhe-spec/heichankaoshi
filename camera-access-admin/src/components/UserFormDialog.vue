<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑用户' : '新建用户'"
    width="620px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="Boolean(form.id)" />
      </el-form-item>
      <el-form-item label="姓名" prop="displayName">
        <el-input v-model="form.displayName" />
      </el-form-item>
      <el-form-item v-if="!form.id" label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="电子邮箱" prop="email">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="手机号码">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option label="普通用户" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="ACTIVE">正常</el-radio>
          <el-radio value="DISABLED">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="有效日期">
        <el-date-picker
          v-model="validRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
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

const emit = defineEmits(['save'])
const visible = ref(false)
const saving = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: null,
  username: '',
  displayName: '',
  password: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 'ACTIVE',
  validFrom: '',
  validUntil: ''
})

const form = reactive(defaultForm())

const validRange = computed({
  get: () =>
    form.validFrom && form.validUntil ? [form.validFrom, form.validUntil] : [],
  set: (value) => {
    form.validFrom = value?.[0] || ''
    form.validUntil = value?.[1] || ''
  }
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    {
      validator: (_, value, callback) => {
        if (!form.id && (!value || value.length < 6)) {
          callback(new Error('密码至少为6位'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
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
