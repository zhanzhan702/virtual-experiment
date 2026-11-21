<!-- 杆上变场景（步骤 8 起）：集中器安装，待后续开发 -->
<template>
  <main class="pole-transformer-view">
    <p class="title">杆上变场景 · 集中器安装</p>
    <p class="hint">该场景将在下一阶段开发</p>
    <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.pole-transformer-view {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.2em;
  background: #f5f7f9;
  color: #123f68;
}

.title {
  font-size: clamp(22px, 2.4vw, 36px);
  letter-spacing: 0.08em;
}

.hint {
  font-size: clamp(14px, 1.2vw, 20px);
  color: #6b7c8d;
}
</style>
