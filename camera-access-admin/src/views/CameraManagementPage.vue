<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">摄像头管理</h1>
        <p class="page-subtitle">
          管理设备资料、管理员上下线状态和视频流实时运行状态。
        </p>
      </div>
      <el-button type="primary" @click="formDialog.open()">
        <el-icon><Plus /></el-icon>
        新建摄像头
      </el-button>
    </div>

    <el-alert
      title="“已上线”表示管理员允许用户访问；“视频在线”表示后端检测到视频流真实可用。两种状态相互独立。"
      type="info"
      show-icon
      :closable="false"
      class="status-alert"
    />

    <el-card class="panel-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索名称、编码或位置"
          clearable
          style="width: 260px"
          @keyup.enter="loadData"
        />
        <el-select
          v-model="query.adminStatus"
          placeholder="管理状态"
          clearable
          style="width: 145px"
        >
          <el-option label="已上线" value="ONLINE" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
        <el-select
          v-model="query.runtimeStatus"
          placeholder="运行状态"
          clearable
          style="width: 145px"
        >
          <el-option label="在线" value="ONLINE" />
          <el-option label="离线" value="OFFLINE" />
          <el-option label="异常" value="ERROR" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="cameraCode" label="编码" width="110" />
        <el-table-column prop="cameraName" label="摄像头名称" min-width="130" />
        <el-table-column prop="location" label="安装位置" min-width="130" />
        <el-table-column prop="streamType" label="播放类型" width="100" />
        <el-table-column label="管理状态" width="105">
          <template #default="{ row }">
            <el-tag :type="row.adminStatus === 'ONLINE' ? 'success' : 'info'">
              {{ row.adminStatus === 'ONLINE' ? '已上线' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行状态" width="115">
          <template #default="{ row }">
            <span>
              <i
                class="status-dot"
                :class="{
                  online: row.runtimeStatus === 'ONLINE',
                  offline: row.runtimeStatus === 'OFFLINE',
                  error: row.runtimeStatus === 'ERROR'
                }"
              />
              {{ runtimeText(row.runtimeStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="AI分析" width="90">
          <template #default="{ row }">
            <el-tag :type="row.aiEnabled ? 'primary' : 'info'" effect="plain">
              {{ row.aiEnabled ? '启用' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeatTime" label="最近检测" min-width="165" />
        <el-table-column label="操作" width="345" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="preview(row)">预览</el-button>
              <el-button size="small" @click="testStream(row)">检测</el-button>
              <el-button size="small" @click="formDialog.open(row)">编辑</el-button>
              <el-button
                v-if="row.adminStatus === 'OFFLINE'"
                size="small"
                type="success"
                @click="setOnline(row)"
              >
                上线
              </el-button>
              <el-button
                v-else
                size="small"
                type="warning"
                @click="openOffline(row)"
              >
                下线
              </el-button>
              <el-button size="small" type="danger" plain @click="remove(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @change="loadData"
        />
      </div>
    </el-card>

    <CameraFormDialog ref="formDialog" @save="saveCamera" />

    <el-dialog v-model="offlineVisible" title="下线摄像头" width="500px">
      <el-alert
        title="下线后用户无法观看该摄像头，AI截图分析也将被阻止。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-form label-width="90px" style="margin-top: 20px">
        <el-form-item label="摄像头">
          <el-input :model-value="offlineTarget?.cameraName" disabled />
        </el-form-item>
        <el-form-item label="下线原因">
          <el-input
            v-model="offlineReason"
            type="textarea"
            :rows="4"
            placeholder="请输入设备维护、推流异常等原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="offlineVisible = false">取消</el-button>
          <el-button type="warning" @click="confirmOffline">确认下线</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      :title="`${previewTarget?.cameraName || ''} 实时预览`"
      width="860px"
      destroy-on-close
    >
      <div class="preview-area">
        <iframe
          v-if="previewTarget?.streamType === 'IFRAME'"
          :src="previewTarget.streamUrl"
          allow="autoplay; fullscreen"
        />
        <video
          v-else
          :src="previewTarget?.streamUrl"
          controls
          autoplay
          muted
          playsinline
        />
      </div>
      <el-descriptions :column="2" border style="margin-top: 16px">
        <el-descriptions-item label="管理状态">
          {{ previewTarget?.adminStatus === 'ONLINE' ? '已上线' : '已下线' }}
        </el-descriptions-item>
        <el-descriptions-item label="运行状态">
          {{ runtimeText(previewTarget?.runtimeStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="安装位置">
          {{ previewTarget?.location }}
        </el-descriptions-item>
        <el-descriptions-item label="播放类型">
          {{ previewTarget?.streamType }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CameraFormDialog from '@/components/CameraFormDialog.vue'
import {
  createCameraApi,
  deleteCameraApi,
  listCamerasApi,
  setCameraOfflineApi,
  setCameraOnlineApi,
  testCameraStreamApi,
  updateCameraApi
} from '@/api/camera'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const formDialog = ref()
const offlineVisible = ref(false)
const offlineTarget = ref(null)
const offlineReason = ref('')
const previewVisible = ref(false)
const previewTarget = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  adminStatus: '',
  runtimeStatus: ''
})

function runtimeText(status) {
  return {
    ONLINE: '视频在线',
    OFFLINE: '视频离线',
    CONNECTING: '连接中',
    ERROR: '推流异常'
  }[status] || '未知'
}

async function loadData() {
  loading.value = true
  try {
    const data = await listCamerasApi(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    page: 1,
    size: 10,
    keyword: '',
    adminStatus: '',
    runtimeStatus: ''
  })
  loadData()
}

async function saveCamera(payload, resolve, reject) {
  try {
    if (payload.id) {
      await updateCameraApi(payload.id, payload)
      ElMessage.success('摄像头信息已更新')
    } else {
      await createCameraApi(payload)
      ElMessage.success('摄像头创建成功')
    }
    await loadData()
    resolve()
  } catch (error) {
    reject(error)
  }
}

async function setOnline(row) {
  await ElMessageBox.confirm(
    `确认上线 ${row.cameraName} 吗？上线后仍需视频流状态为在线，用户才能观看。`,
    '上线确认',
    { type: 'success' }
  )
  await setCameraOnlineApi(row.id, '管理员手动上线')
  ElMessage.success('摄像头已上线')
  loadData()
}

function openOffline(row) {
  offlineTarget.value = row
  offlineReason.value = ''
  offlineVisible.value = true
}

async function confirmOffline() {
  if (!offlineReason.value.trim()) {
    ElMessage.warning('请输入下线原因')
    return
  }
  await setCameraOfflineApi(offlineTarget.value.id, offlineReason.value.trim())
  offlineVisible.value = false
  ElMessage.success('摄像头已下线')
  loadData()
}

async function testStream(row) {
  const result = await testCameraStreamApi(row.id)
  if (result.success) {
    ElMessage.success(result.message || '视频流连接正常')
  } else {
    ElMessage.warning(result.message || '视频流无法连接')
  }
  loadData()
}

function preview(row) {
  previewTarget.value = row
  previewVisible.value = true
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除摄像头 ${row.cameraName} 吗？`,
    '删除确认',
    { type: 'warning' }
  )
  await deleteCameraApi(row.id)
  ElMessage.success('摄像头已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.status-alert {
  margin-bottom: 18px;
}

.pagination {
  display: flex;
  margin-top: 20px;
  justify-content: flex-end;
}

.preview-area {
  overflow: hidden;
  width: 100%;
  height: 430px;
  border-radius: 12px;
  background: #0b1020;
}

.preview-area iframe,
.preview-area video {
  width: 100%;
  height: 100%;
  border: 0;
  object-fit: contain;
}
</style>
