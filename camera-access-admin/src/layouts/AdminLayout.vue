<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '72px' : '230px'" class="sidebar">
      <div class="brand" :class="{ collapsed }">
        <div class="brand-icon">
          <el-icon><VideoCameraFilled /></el-icon>
        </div>
        <div v-show="!collapsed" class="brand-copy">
          <strong>智慧考试系统</strong>
          <span>后台管理中心</span>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        router
        :collapse="collapsed"
        :collapse-transition="false"
        class="side-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>

      <button class="collapse-button" type="button" @click="collapsed = !collapsed">
        <el-icon>
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <span v-if="!collapsed">收起菜单</span>
      </button>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h2>{{ route.meta.title }}</h2>
          <p>管理用户、摄像头状态与访问权限</p>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <div class="admin-user">
            <el-avatar :size="38">
              {{ authStore.user?.displayName?.slice(0, 1) || '管' }}
            </el-avatar>
            <div class="user-copy">
              <strong>{{ authStore.user?.displayName || '系统管理员' }}</strong>
              <span>{{ authStore.user?.role || 'ADMIN' }}</span>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)

const menuItems = computed(() => [
  { path: '/dashboard', title: '控制台', icon: 'DataBoard' },
  { path: '/users', title: '用户管理', icon: 'User' },
  { path: '/cameras', title: '摄像头管理', icon: 'VideoCamera' },
  { path: '/permissions', title: '访问权限', icon: 'Key' }
])

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  position: relative;
  overflow: hidden;
  background: #0f172a;
  transition: width 0.2s ease;
}

.brand {
  display: flex;
  align-items: center;
  height: 78px;
  padding: 0 18px;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
  color: white;
}

.brand.collapsed {
  justify-content: center;
  padding: 0;
}

.brand-icon {
  display: grid;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  font-size: 21px;
}

.brand-copy {
  display: flex;
  min-width: 0;
  margin-left: 12px;
  flex-direction: column;
}

.brand-copy strong {
  white-space: nowrap;
  font-size: 16px;
}

.brand-copy span {
  margin-top: 3px;
  color: #94a3b8;
  white-space: nowrap;
  font-size: 12px;
}

.side-menu {
  border: 0;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 48px;
  margin: 7px 10px;
  border-radius: 9px;
  color: #a9b4c6;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgb(255 255 255 / 7%);
  color: white;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgb(59 130 246 / 28%), rgb(99 102 241 / 18%));
  color: #fff;
}

.collapse-button {
  position: absolute;
  right: 10px;
  bottom: 18px;
  left: 10px;
  display: flex;
  height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 9px;
  background: rgb(255 255 255 / 6%);
  color: #a9b4c6;
  cursor: pointer;
}

.topbar {
  display: flex;
  height: 78px;
  padding: 0 26px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8edf4;
  background: white;
}

.topbar h2 {
  margin: 0;
  color: #1e293b;
  font-size: 20px;
}

.topbar p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  outline: none;
  cursor: pointer;
}

.user-copy {
  display: flex;
  min-width: 90px;
  flex-direction: column;
}

.user-copy strong {
  color: #1e293b;
  font-size: 14px;
}

.user-copy span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 11px;
}

.main-content {
  padding: 0;
  background: #f3f6fb;
}
</style>
