<template>
  <div class="admin-layout">
    <!-- 顶栏：品牌 + 功能入口 + 当前用户 -->
    <header class="admin-header">
      <div class="admin-brand">管理后台</div>

      <el-menu class="admin-menu" mode="horizontal" :default-active="activeMenu" router>
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          :disabled="item.disabled"
        >
          <el-tooltip v-if="item.disabled" content="待开发" placement="bottom">
            <span>{{ item.label }}</span>
          </el-tooltip>
          <span v-else>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <el-dropdown class="admin-user" @command="onUserCommand">
        <span class="admin-user-trigger">
          {{ authStore.user?.name || authStore.user?.username || '未登录' }}
          <span class="admin-user-caret" />
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <!-- 内容区 -->
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

/**
 * 主题类挂在 body 上，使 teleport 出去的弹窗/下拉/气泡也能继承青绿主题
 * （见 assets/styles/admin.css）。离开后台时移除，避免影响学生端页面。
 */
onMounted(() => document.body.classList.add('admin-theme'))
onUnmounted(() => document.body.classList.remove('admin-theme'))

/**
 * 顶栏入口。后 4 个功能尚未开发，先占位渲染并置灰，
 * 避免后续每加一个页面都要重排版；上线时只需把对应 disabled 改为 false。
 */
const menuItems = [
  { path: '/admin/users', label: '用户管理', disabled: false },
  { path: '/admin/grades', label: '查看学生成绩', disabled: false },
  { path: '/admin/org', label: '专业班级管理', disabled: true },
  { path: '/admin/stats', label: '统计分析', disabled: true },
  { path: '/admin/profile', label: '个人消息管理', disabled: true }
]

const activeMenu = computed(() => router.currentRoute.value.path)

async function onUserCommand(command) {
  if (command !== 'logout') return

  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }

  authStore.logout()
  router.push('/')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-header {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid var(--admin-border);
}

/* 品牌块：青绿实心，对应原站左上角的「管理后台」色块 */
.admin-brand {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  height: 30px;
  margin-right: 32px;
  padding: 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: var(--admin-primary);
  border-radius: 4px;
}

.admin-menu {
  flex: 1;
  background: transparent;
  border-bottom: none;
}

.admin-user {
  flex-shrink: 0;
  margin-left: 24px;
}

.admin-user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--admin-text-secondary);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: color 0.2s;
}

.admin-user-trigger:hover {
  color: var(--admin-primary);
}

/* 下拉箭头：用边框拼三角，避免为一个图标引入 @element-plus/icons-vue 依赖 */
.admin-user-caret {
  width: 0;
  height: 0;
  border-top: 5px solid currentColor;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
}

.admin-main {
  padding: 20px 24px;
}

/* ── 覆盖 Element Plus 菜单默认样式 ─────────────────────── */
:deep(.el-menu--horizontal) {
  border-bottom: none;
}

:deep(.el-menu--horizontal > .el-menu-item) {
  height: 56px;
  line-height: 56px;
  color: var(--admin-text-secondary);
  font-size: 14px;
  border-bottom: 2px solid transparent;
}

:deep(.el-menu--horizontal > .el-menu-item:hover),
:deep(.el-menu--horizontal > .el-menu-item:focus) {
  color: var(--admin-primary);
  background: transparent;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: var(--admin-primary);
  font-weight: 600;
  border-bottom-color: var(--admin-primary);
  background: transparent;
}

:deep(.el-menu--horizontal > .el-menu-item.is-disabled) {
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 1;
}
</style>
