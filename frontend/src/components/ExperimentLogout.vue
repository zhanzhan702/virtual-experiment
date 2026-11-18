<!-- 实验场景退出登录按钮：固定右上角（计时器下方），确认后清空登录态并回到登录页 -->
<template>
  <Teleport to="body">
    <button class="experiment-logout" type="button" @click="handleLogout">
      <span class="logout-icon">←</span>
      <span>退出登录</span>
    </button>
  </Teleport>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？退出后需重新登录才能继续实验。', '提示', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }

  authStore.logout()
  // 清除本次实验的临时标记，避免影响下一个登录用户
  sessionStorage.removeItem('experimentId')
  router.push('/')
}
</script>

<style scoped>
/* 固定左上角：避开右上角计时器与两侧 10vh 起始的工具栏 */
.experiment-logout {
  position: fixed;
  top: 1.5rem;
  left: 1.5rem;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: clamp(32px, 5vh, 40px);
  padding: 0 clamp(12px, 1.4vw, 18px);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 2px solid #3395e0;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  color: #303133;
  font-size: clamp(13px, 1.4vw, 16px);
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  user-select: none;
  transition:
    border-color 0.25s,
    color 0.25s,
    background 0.25s;
}

.experiment-logout:hover {
  border-color: #e05a4f;
  color: #e05a4f;
  background: rgba(255, 245, 244, 0.95);
}

.logout-icon {
  font-size: clamp(15px, 1.6vw, 18px);
  line-height: 1;
}
</style>
