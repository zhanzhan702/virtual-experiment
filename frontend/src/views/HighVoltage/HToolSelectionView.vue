<template>
  <div class="tool-selection-view">
    <!-- 页面标题 -->
    <div class="view-header">
      <div class="view-title">
        <el-icon :size="24">
          <Suitcase />
        </el-icon>
        <span>选择工器具</span>
      </div>
      <div class="view-subtitle">
        请依次选择正确的个人防护用具、终端设备、工器具和线材，确保作业安全
      </div>
    </div>

    <WizardInventorySelection :categories="categories" @finish="handleToolSelectionSubmit" @operation="handleOperation"
      @submit-error="handleSubmitError" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Suitcase } from '@element-plus/icons-vue'
import WizardInventorySelection from '@/components/HighVoltage/HWizardInventorySelection.vue'
import { categories } from '@/constants/tool-selection-config'
import { submitStep } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()

// 从路由 query 获取实验元数据
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')

// 页面加载时记录步骤开始时间（非提交时）
const startedAt = ref(formatLocalTime(new Date()))

// 操作统计
const stats = reactive({
  duration_seconds: 0,
  operation_count: 0,
  error_count: 0
})

let timer = null
onMounted(() => { timer = setInterval(() => { stats.duration_seconds++ }, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })

// 统计操作次数（每次点击/选择工器具）
const handleOperation = () => { stats.operation_count++ }

// 统计提交错误次数
const handleSubmitError = (errorPageCount) => { stats.error_count += errorPageCount }

const handleToolSelectionSubmit = async (selectedMap) => {
  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: stats.duration_seconds,
    operationCount: stats.operation_count,
    errorCount: stats.error_count,
    score: 100.00 - (stats.error_count * 10) > 0 ? 100.00 - (stats.error_count * 10) : 0,//最低得分为0分
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('工器具选择已完成，即将进入下一步...')
    setTimeout(() => {
      router.push({
        path: '/',
        query: { experimentId: experimentId.value }
      })
    }, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}
</script>

<style scoped>
.tool-selection-view {
  position: relative;
  padding: 24px;
  height: 100%;
  min-height: 100vh;
  /* 半透明背景色叠加，让内容区域保持可读 */
  background: linear-gradient(180deg, rgba(240, 245, 255, 0.82) 0%, rgba(245, 247, 250, 0.82) 100%);
}

/* 背景图伪元素 */
.tool-selection-view::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url('@/assets/images/selection.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
}

/* 确保内容在背景之上 */
.tool-selection-view>* {
  position: relative;
  z-index: 1;
}

.view-header {
  text-align: center;
  margin-bottom: 24px;
}

.view-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.view-subtitle {
  font-size: 14px;
  color: #909399;
}
</style>
