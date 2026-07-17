<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="brand-badge">
        <el-icon><VideoCameraFilled /></el-icon>
      </div>
      <h1>智慧考试系统</h1>
      <p>集中管理用户、摄像头设备、在线状态和访问授权。</p>

      <div class="features">
        <div><el-icon><CircleCheck /></el-icon> 摄像头实时上下线管理</div>
        <div><el-icon><CircleCheck /></el-icon> 用户与访问时间授权</div>
        <div><el-icon><CircleCheck /></el-icon> 视频流健康状态检测</div>
      </div>
    </section>

    <section class="login-panel">
      <el-card class="login-card" shadow="never">
        <div class="login-title">
          <h2>管理员登录</h2>
          <p>请输入后台管理账号继续</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入管理员用户名"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="submit"
          >
            登录后台
          </el-button>
        </el-form>

        <p v-if="useMock" class="mock-tip">
          当前为模拟模式，输入任意用户名和密码即可登录。
        </p>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const useMock = import.meta.env.VITE_USE_MOCK === 'true'

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.login(form)
    router.replace(String(route.query.redirect || '/dashboard'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 1.1fr 0.9fr;
  background: #fff;
}

.login-brand {
  position: relative;
  display: flex;
  overflow: hidden;
  padding: 12%;
  justify-content: center;
  flex-direction: column;
  background:
    radial-gradient(circle at 15% 20%, rgb(59 130 246 / 35%), transparent 35%),
    radial-gradient(circle at 80% 80%, rgb(99 102 241 / 35%), transparent 35%),
    #0f172a;
  color: white;
}

.login-brand::after {
  position: absolute;
  right: -160px;
  bottom: -160px;
  width: 420px;
  height: 420px;
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 50%;
  box-shadow:
    0 0 0 70px rgb(255 255 255 / 3%),
    0 0 0 140px rgb(255 255 255 / 2%);
  content: "";
}

.brand-badge {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 18px;
  background: rgb(255 255 255 / 9%);
  font-size: 30px;
}

.login-brand h1 {
  margin: 28px 0 10px;
  font-size: 42px;
}

.login-brand > p {
  max-width: 520px;
  margin: 0;
  color: #bcc8d9;
  font-size: 17px;
  line-height: 1.8;
}

.features {
  display: grid;
  margin-top: 42px;
  gap: 18px;
  color: #dbe6f5;
}

.features div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.login-panel {
  display: grid;
  padding: 40px;
  place-items: center;
  background: #f7f9fc;
}

.login-card {
  width: 430px;
  padding: 24px 18px;
  border: 0;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgb(15 23 42 / 10%);
}

.login-title {
  margin-bottom: 28px;
}

.login-title h2 {
  margin: 0;
  color: #172033;
  font-size: 28px;
}

.login-title p {
  margin: 8px 0 0;
  color: #8995a7;
}

.login-button {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
}

.mock-tip {
  margin: 20px 0 0;
  color: #9aa5b5;
  text-align: center;
  font-size: 12px;
}
</style>
