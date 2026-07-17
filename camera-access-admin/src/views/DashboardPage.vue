<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">控制台</h1>
        <p class="page-subtitle">查看系统用户、摄像头与授权状态。</p>
      </div>
      <el-button :loading="loading" @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <div class="stats-grid">
      <el-card v-for="item in stats" :key="item.label" class="stat-card panel-card">
        <div class="stat-content">
          <div class="stat-icon" :class="item.className">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </el-card>
    </div>

    <div class="content-grid">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <strong>摄像头状态</strong>
              <span>状态由 status 字段统一控制</span>
            </div>
            <el-button text type="primary" @click="$router.push('/cameras')">
              查看全部
            </el-button>
          </div>
        </template>

        <el-table :data="data.cameras" stripe>
          <el-table-column prop="cameraName" label="摄像头" min-width="130" />
          <el-table-column prop="location" label="位置" min-width="130" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag
                :type="
                  row.status === 'ONLINE'
                    ? 'success'
                    : row.status === 'MAINTENANCE'
                      ? 'warning'
                      : 'info'
                "
              >
                {{
                  row.status === 'ONLINE'
                    ? '已上线'
                    : row.status === 'MAINTENANCE'
                      ? '维护中'
                      : '已下线'
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="180">
            <template #default="{ row }">
              {{ row.description || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="panel-card operation-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <strong>最近操作</strong>
              <span>后台重要状态变更</span>
            </div>
          </div>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="item in data.operations"
            :key="item.id"
            :timestamp="item.time"
            :type="item.type"
          >
            {{ item.title }}
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getDashboardSummaryApi } from '@/api/dashboard'

const loading = ref(false)
const data = reactive({
  userCount: 0,
  cameraCount: 0,
  onlineCameraCount: 0,
  offlineCameraCount: 0,
  maintenanceCameraCount: 0,
  activePermissionCount: 0,
  todayAnalysisCount: 0,
  cameras: [],
  operations: []
})

const stats = computed(() => [
  {
    label: '用户总数',
    value: data.userCount,
    icon: 'User',
    className: 'blue'
  },
  {
    label: '摄像头总数',
    value: data.cameraCount,
    icon: 'VideoCamera',
    className: 'purple'
  },
  {
    label: '已上线设备',
    value: data.onlineCameraCount,
    icon: 'Connection',
    className: 'green'
  },
  {
    label: '已下线设备',
    value: data.offlineCameraCount,
    icon: 'CircleClose',
    className: 'cyan'
  },
  {
    label: '有效授权',
    value: data.activePermissionCount,
    icon: 'Key',
    className: 'orange'
  },
  {
    label: '今日 AI 分析',
    value: data.todayAnalysisCount,
    icon: 'MagicStick',
    className: 'pink'
  }
])

async function loadData() {
  loading.value = true
  try {
    Object.assign(data, await getDashboardSummaryApi())
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.stats-grid {
  display: grid;
  margin-bottom: 20px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  min-width: 0;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 13px;
}

.stat-icon {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  font-size: 20px;
}

.stat-icon.blue {
  background: #eaf2ff;
  color: #2563eb;
}

.stat-icon.purple {
  background: #f0edff;
  color: #7c3aed;
}

.stat-icon.green {
  background: #e9f9ef;
  color: #16a34a;
}

.stat-icon.cyan {
  background: #e6fafa;
  color: #0891b2;
}

.stat-icon.orange {
  background: #fff5e5;
  color: #ea580c;
}

.stat-icon.pink {
  background: #fcecf5;
  color: #db2777;
}

.stat-content span {
  display: block;
  color: #8793a5;
  white-space: nowrap;
  font-size: 12px;
}

.stat-content strong {
  display: block;
  margin-top: 4px;
  color: #172033;
  font-size: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(300px, 0.75fr);
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header strong,
.card-header span {
  display: block;
}

.card-header strong {
  color: #1e293b;
}

.card-header span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.operation-card :deep(.el-card__body) {
  padding-top: 24px;
}
</style>
