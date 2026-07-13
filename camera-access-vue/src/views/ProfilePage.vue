<template>
  <div>
    <AppHeader />

    <main class="container profile-page">
      <div class="page-title">
        <div>
          <h1>个人资料</h1>
          <p class="muted">
            查看当前账号信息和摄像头访问权限
          </p>
        </div>

        <button
          type="button"
          class="secondary"
          :disabled="loading"
          @click="loadProfile"
        >
          {{ loading ? '正在刷新...' : '刷新资料' }}
        </button>
      </div>

      <div
        v-if="loading"
        class="state-card"
      >
        正在加载个人资料...
      </div>

      <div
        v-else-if="error"
        class="state-card error-text"
      >
        {{ error }}
      </div>

      <template v-else>
        <section class="profile-card">
          <div class="profile-avatar">
            <img
              v-if="profile.avatarUrl"
              :src="profile.avatarUrl"
              alt="用户头像"
            />

            <span v-else>
              {{ avatarText }}
            </span>
          </div>

          <div class="profile-summary">
            <h2>
              {{ profile.username || '未知用户' }}
            </h2>

            <p>
              {{ profile.email || '未设置邮箱' }}
            </p>
          </div>

          <span class="role-badge">
            {{ formatRole(profile.role) }}
          </span>
        </section>

        <section class="content-card">
          <div class="section-header">
            <div>
              <h2>账号信息</h2>
              <p class="muted">
                当前登录账号的基本资料
              </p>
            </div>
          </div>

          <div class="profile-info-grid">
            <!-- <div class="profile-info-item">
              <span class="info-label">用户 ID</span>
              <span class="info-value">
                {{ profile.id || '-' }}
              </span>
            </div> -->

            <div class="profile-info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">
                {{ profile.username || '-' }}
              </span>
            </div>

            <div class="profile-info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">
                {{ profile.email || '未设置' }}
              </span>
            </div>

            <!-- <div class="profile-info-item">
              <span class="info-label">角色</span>
              <span class="info-value">
                {{ formatRole(profile.role) }}
              </span>
            </div> -->

            <div class="profile-info-item">
              <span class="info-label">账号状态</span>

              <span
                class="account-status"
                :class="isActive ? 'account-status--active' : 'account-status--disabled'"
              >
                {{ formatStatus(profile.status) }}
              </span>
            </div>
          </div>
        </section>

        <section class="content-card">
          <div class="section-header">
            <div>
              <h2>摄像头访问权限</h2>
              <p class="muted">
                当前账号可以访问的摄像头及授权时间
              </p>
            </div>

            <span class="camera-count">
              共 {{ cameras.length }} 个
            </span>
          </div>

          <div
            v-if="cameras.length === 0"
            class="empty-state"
          >
            当前账号没有摄像头访问权限
          </div>

          <div
            v-else
            class="permission-list"
          >
            <article
              v-for="camera in cameras"
              :key="camera.cameraId"
              class="permission-card"
            >
              <div class="permission-card__header">
                <div>
                  <h3>
                    {{ camera.cameraName || '未命名摄像头' }}
                  </h3>

                  <p>
                    {{ camera.location || '未知位置' }}
                  </p>
                </div>

                <span
                  class="camera-status"
                  :class="isCameraOnline(camera)
                    ? 'camera-status--online'
                    : 'camera-status--offline'"
                >
                  {{ isCameraOnline(camera) ? '在线' : '离线' }}
                </span>
              </div>

              <div class="permission-details">
                <div class="permission-row">
                  <span class="permission-label">
                    摄像头编号
                  </span>

                  <span class="permission-value">
                    {{ camera.cameraId || '-' }}
                  </span>
                </div>

                <div class="permission-row">
                  <span class="permission-label">
                    授权日期
                  </span>

                  <span class="permission-value">
                    {{
                      formatDateRange(
                        camera.validFrom,
                        camera.validUntil
                      )
                    }}
                  </span>
                </div>

                <div class="permission-row">
                  <span class="permission-label">
                    访问时间
                  </span>

                  <span class="permission-value">
                    {{
                      formatAccessTime(
                        camera.accessStartTime,
                        camera.accessEndTime
                      )
                    }}
                  </span>
                </div>

                <div class="permission-row">
                  <span class="permission-label">
                    当前权限
                  </span>

                  <span
                    class="permission-status"
                    :class="camera.canAccessNow
                      ? 'permission-status--allowed'
                      : 'permission-status--denied'"
                  >
                    {{
                      camera.canAccessNow
                        ? '当前可以访问'
                        : camera.accessMessage || '当前不可访问'
                    }}
                  </span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import { getCurrentUserApi } from '../api/user'
import { getCameraListApi } from '../api/camera'

const loading = ref(false)
const error = ref('')

const profile = ref({
  id: '',
  username: '',
  email: '',
  role: '',
  status: '',
  avatarUrl: ''
})

const cameras = ref([])

const avatarText = computed(() => {
  const username = String(
    profile.value.username || 'U'
  ).trim()

  return username.charAt(0).toUpperCase()
})

const isActive = computed(() => {
  return String(
    profile.value.status || ''
  ).toUpperCase() === 'ACTIVE'
})

async function loadProfile() {
  loading.value = true
  error.value = ''

  try {
    const [userResult, cameraResult] =
      await Promise.all([
        getCurrentUserApi(),
        getCameraListApi()
      ])

    profile.value = {
      id: userResult?.id || '',
      username: userResult?.username || '',
      email: userResult?.email || '',
      role: userResult?.role || '',
      status: userResult?.status || '',
      avatarUrl: userResult?.avatarUrl || ''
    }

    cameras.value = Array.isArray(cameraResult)
      ? cameraResult
      : []
  } catch (e) {
    error.value =
      e?.message || '个人资料加载失败'
  } finally {
    loading.value = false
  }
}

function isCameraOnline(camera) {
  return String(
    camera?.status || ''
  ).toLowerCase() === 'online'
}

function formatRole(role) {
  const value = String(role || '').toUpperCase()

  const roleMap = {
    USER: '普通用户',
    ADMIN: '管理员'
  }

  return roleMap[value] || role || '未知角色'
}

function formatStatus(status) {
  const value = String(status || '').toUpperCase()

  const statusMap = {
    ACTIVE: '正常',
    DISABLED: '已禁用',
    LOCKED: '已锁定'
  }

  return statusMap[value] || status || '未知状态'
}

function formatChineseDate(dateValue) {
  if (!dateValue) {
    return ''
  }

  const dateText = String(dateValue).substring(0, 10)
  const parts = dateText.split('-')

  if (parts.length !== 3) {
    return String(dateValue)
  }

  const year = Number(parts[0])
  const month = Number(parts[1])
  const day = Number(parts[2])

  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day)
  ) {
    return String(dateValue)
  }

  return `${year}年${month}月${day}日`
}

function formatDateRange(validFrom, validUntil) {
  if (!validFrom && !validUntil) {
    return '长期有效'
  }

  if (validFrom && !validUntil) {
    return `${formatChineseDate(validFrom)} 起长期有效`
  }

  if (!validFrom && validUntil) {
    return `有效期至 ${formatChineseDate(validUntil)}`
  }

  return `${formatChineseDate(validFrom)} 至 ${formatChineseDate(validUntil)}`
}

function formatAccessTime(startTime, endTime) {
  if (!startTime || !endTime) {
    return '未配置'
  }

  return `${formatTime(startTime)} - ${formatTime(endTime)}`
}

function formatTime(timeValue) {
  if (!timeValue) {
    return '--:--'
  }

  return String(timeValue).substring(0, 5)
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  padding-bottom: 48px;
}

.page-title {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title h1 {
  margin: 0;
  color: #172033;
}

.muted {
  margin: 6px 0 0;
  color: #7c879b;
}

.secondary {
  padding: 10px 18px;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}

.secondary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.state-card,
.content-card,
.profile-card {
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 10px 28px rgb(15 23 42 / 6%);
}

.error-text {
  color: #dc2626;
}

.profile-card {
  display: flex;
  gap: 18px;
  align-items: center;
  margin-bottom: 24px;
}

.profile-avatar {
  display: flex;
  flex: 0 0 72px;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  color: #ffffff;
  font-size: 28px;
  font-weight: 700;
  background: #2563eb;
  border-radius: 50%;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-summary {
  flex: 1;
}

.profile-summary h2 {
  margin: 0;
  color: #172033;
}

.profile-summary p {
  margin: 6px 0 0;
  color: #7c879b;
}

.role-badge,
.camera-count {
  padding: 6px 11px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  background: #eff6ff;
  border-radius: 999px;
}

.content-card {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 18px;
  margin-bottom: 20px;
  border-bottom: 1px solid #eef2f7;
}

.section-header h2 {
  margin: 0;
  color: #172033;
  font-size: 19px;
}

.profile-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-info-item {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.info-label,
.permission-label {
  color: #7c879b;
  font-size: 13px;
}

.info-value,
.permission-value {
  overflow-wrap: anywhere;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
}

.account-status,
.camera-status,
.permission-status {
  width: fit-content;
  padding: 4px 9px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
}

.account-status--active,
.camera-status--online,
.permission-status--allowed {
  color: #15803d;
  background: #ecfdf3;
}

.account-status--disabled,
.camera-status--offline {
  color: #64748b;
  background: #f1f5f9;
}

.permission-status--denied {
  color: #b45309;
  background: #fff7ed;
}

.permission-list {
  display: grid;
  grid-template-columns: repeat(
    auto-fit,
    minmax(300px, 1fr)
  );
  gap: 18px;
}

.permission-card {
  padding: 20px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
}

.permission-card__header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.permission-card__header h3 {
  margin: 0;
  color: #172033;
  font-size: 17px;
}

.permission-card__header p {
  margin: 5px 0 0;
  color: #7c879b;
  font-size: 13px;
}

.permission-details {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 16px;
}

.permission-row {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
}

.empty-state {
  padding: 32px;
  color: #7c879b;
  text-align: center;
  background: #f8fafc;
  border-radius: 12px;
}

@media (max-width: 720px) {
  .page-title,
  .profile-card,
  .section-header {
    align-items: flex-start;
  }

  .page-title {
    flex-direction: column;
  }

  .profile-card {
    flex-wrap: wrap;
  }

  .profile-info-grid {
    grid-template-columns: 1fr;
  }

  .permission-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .profile-card,
  .content-card {
    padding: 18px;
  }

  .permission-row {
    grid-template-columns: 1fr;
    gap: 5px;
  }
}
</style>