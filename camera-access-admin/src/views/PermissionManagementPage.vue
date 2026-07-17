<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">访问权限</h1>
        <p class="page-subtitle">
          设置用户可以访问的摄像头、每日访问时间和授权有效日期。
        </p>
      </div>
      <el-button type="primary" @click="formDialog.open()">
        <el-icon><Plus /></el-icon>
        新建权限
      </el-button>
    </div>

    <el-card class="panel-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户或摄像头"
          clearable
          style="width: 260px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <strong>{{ row.displayName }}</strong>
            <div class="secondary-text">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="cameraName" label="摄像头" min-width="140" />
        <el-table-column label="每日访问时间" min-width="160">
          <template #default="{ row }">
            {{ row.accessStartTime }} - {{ row.accessEndTime }}
          </template>
        </el-table-column>
        <el-table-column label="授权日期" min-width="200">
          <template #default="{ row }">
            {{ row.validFrom }} 至 {{ row.validUntil }}
          </template>
        </el-table-column>
        <el-table-column label="AI分析" width="95">
          <template #default="{ row }">
            <el-tag :type="row.aiAnalysisAllowed ? 'success' : 'info'">
              {{ row.aiAnalysisAllowed ? '允许' : '禁止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '生效中' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="formDialog.open(row)">编辑</el-button>
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

    <PermissionFormDialog
      ref="formDialog"
      :users="users"
      :cameras="cameras"
      @save="savePermission"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PermissionFormDialog from '@/components/PermissionFormDialog.vue'
import {
  createPermissionApi,
  deletePermissionApi,
  listPermissionsApi,
  updatePermissionApi
} from '@/api/permission'
import { listUsersApi } from '@/api/user'
import { listCamerasApi } from '@/api/camera'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const users = ref([])
const cameras = ref([])
const formDialog = ref()

const query = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

async function loadOptions() {
  const [userData, cameraData] = await Promise.all([
    listUsersApi({ page: 1, size: 100 }),
    listCamerasApi({ page: 1, size: 100 })
  ])
  users.value = userData.records
  cameras.value = cameraData.records
}

async function loadData() {
  loading.value = true
  try {
    const data = await listPermissionsApi(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, size: 10, keyword: '' })
  loadData()
}

async function savePermission(payload, resolve, reject) {
  try {
    if (payload.id) {
      await updatePermissionApi(payload.id, payload)
      ElMessage.success('访问权限已更新')
    } else {
      await createPermissionApi(payload)
      ElMessage.success('访问权限已创建')
    }
    await loadData()
    resolve()
  } catch (error) {
    reject(error)
  }
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除 ${row.displayName} 对 ${row.cameraName} 的访问权限吗？`,
    '删除确认',
    { type: 'warning' }
  )
  await deletePermissionApi(row.id)
  ElMessage.success('权限已删除')
  loadData()
}

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<style scoped>
.secondary-text {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
}

.pagination {
  display: flex;
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
