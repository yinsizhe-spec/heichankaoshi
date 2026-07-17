<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">摄像头管理</h1>
        <p class="page-subtitle">
          管理摄像头资料，并通过 status 字段控制上线、下线和维护状态。
        </p>
      </div>
      <el-button type="primary" @click="formDialog.open()">
        <el-icon><Plus /></el-icon>
        新建摄像头
      </el-button>
    </div>

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
          v-model="query.status"
          placeholder="摄像头状态"
          clearable
          style="width: 150px"
        >
          <el-option label="已上线" value="ONLINE" />
          <el-option label="已下线" value="OFFLINE" />
          <el-option label="维护中" value="MAINTENANCE" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="cameraCode" label="编码" width="110" />
        <el-table-column prop="cameraName" label="摄像头名称" min-width="140" />
        <el-table-column prop="location" label="安装位置" min-width="140" />
        <el-table-column prop="streamType" label="播放类型" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="160">
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="preview(row)">预览</el-button>
              <el-button size="small" @click="formDialog.open(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'ONLINE'"
                size="small"
                type="success"
                @click="changeStatus(row, 'ONLINE')"
              >
                上线
              </el-button>
              <el-button
                v-if="row.status !== 'OFFLINE'"
                size="small"
                type="warning"
                @click="changeStatus(row, 'OFFLINE')"
              >
                下线
              </el-button>
              <el-button
                v-if="row.status !== 'MAINTENANCE'"
                size="small"
                @click="changeStatus(row, 'MAINTENANCE')"
              >
                维护
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

    <el-dialog
      v-model="previewVisible"
      :title="`${previewTarget?.cameraName || ''} 实时预览`"
      width="860px"
      destroy-on-close
    >
      <div class="preview-area">
        <iframe
          v-if="previewTarget?.streamType === 'IFRAME'"
          :src="previewTarget.streamSourceUrl"
          allow="autoplay; fullscreen"
        />
        <video
          v-else
          :src="previewTarget?.streamSourceUrl"
          controls
          autoplay
          muted
          playsinline
        />
      </div>
      <el-descriptions :column="2" border style="margin-top: 16px">
        <el-descriptions-item label="摄像头状态">
          {{ statusText(previewTarget?.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="安装位置">
          {{ previewTarget?.location || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="播放类型">
          {{ previewTarget?.streamType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="摄像头编码">
          {{ previewTarget?.cameraCode || '-' }}
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
  updateCameraApi,
  updateCameraStatusApi
} from '@/api/camera'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const formDialog = ref()
const previewVisible = ref(false)
const previewTarget = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

function statusText(status) {
  return {
    ONLINE: '已上线',
    OFFLINE: '已下线',
    MAINTENANCE: '维护中'
  }[status] || '未知'
}

function statusTagType(status) {
  return {
    ONLINE: 'success',
    OFFLINE: 'info',
    MAINTENANCE: 'warning'
  }[status] || 'info'
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
    status: ''
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

async function changeStatus(row, status) {
  const actionText = statusText(status)
  await ElMessageBox.confirm(
    `确定将 ${row.cameraName} 设置为“${actionText}”吗？`,
    '状态修改确认',
    {
      type: status === 'ONLINE' ? 'success' : 'warning'
    }
  )

  await updateCameraStatusApi(row.id, status)
  ElMessage.success(`摄像头已设置为${actionText}`)
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
