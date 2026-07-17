<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">创建用户、修改状态并重置登录密码。</p>
      </div>
      <el-button type="primary" @click="formDialog.open()">
        <el-icon><Plus /></el-icon>
        新建用户
      </el-button>
    </div>

    <el-card class="panel-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名、姓名或邮箱"
          clearable
          style="width: 270px"
          @keyup.enter="loadData"
        />
        <el-select
          v-model="query.status"
          placeholder="账号状态"
          clearable
          style="width: 150px"
        >
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="displayName" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="190" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cameraCount" label="摄像头" width="90" />
        <el-table-column label="授权有效期" min-width="190">
          <template #default="{ row }">
            {{ row.validFrom || '-' }} 至 {{ row.validUntil || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="formDialog.open(row)">编辑</el-button>
              <el-button size="small" @click="openPasswordDialog(row)">
                重置密码
              </el-button>
              <el-button
                size="small"
                :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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

    <UserFormDialog ref="formDialog" @save="saveUser" />

    <el-dialog v-model="passwordVisible" title="重置密码" width="430px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :model-value="passwordTarget?.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" @click="resetPassword">确认重置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserFormDialog from '@/components/UserFormDialog.vue'
import {
  createUserApi,
  deleteUserApi,
  listUsersApi,
  resetUserPasswordApi,
  updateUserApi,
  updateUserStatusApi
} from '@/api/user'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const formDialog = ref()
const passwordVisible = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

async function loadData() {
  loading.value = true
  try {
    const data = await listUsersApi(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, size: 10, keyword: '', status: '' })
  loadData()
}

async function saveUser(payload, resolve, reject) {
  try {
    if (payload.id) {
      await updateUserApi(payload.id, payload)
      ElMessage.success('用户信息已更新')
    } else {
      await createUserApi(payload)
      ElMessage.success('用户创建成功')
    }
    await loadData()
    resolve()
  } catch (error) {
    reject(error)
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateUserStatusApi(row.id, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '用户已启用' : '用户已禁用')
  loadData()
}

function openPasswordDialog(row) {
  passwordTarget.value = row
  newPassword.value = ''
  passwordVisible.value = true
}

async function resetPassword() {
  if (newPassword.value.length < 6) {
    ElMessage.warning('新密码至少为6位')
    return
  }
  await resetUserPasswordApi(passwordTarget.value.id, newPassword.value)
  passwordVisible.value = false
  ElMessage.success('密码重置成功')
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除用户 ${row.username} 吗？删除后相关权限也应由后端同步清理。`,
    '删除确认',
    { type: 'warning' }
  )
  await deleteUserApi(row.id)
  ElMessage.success('用户已删除')
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
</style>
