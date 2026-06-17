<template>
  <div class="experiment-scene">
    <div class="scroll-wrapper">
      <WorkTicketForm @submit-ticket="handleTicketSubmit" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitStep } from '@/api/experiment'
import WorkTicketForm from '@/components/HighVoltage/HWorkTicketForm.vue'

const route = useRoute()
const router = useRouter()

// 从路由 query 获取实验元数据
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')

// 接收子组件抛出的提交事件
const handleTicketSubmit = async (result) => {
  if (!result.success) {
    if (result.errors && Object.keys(result.errors).length > 0) {
      const msgs = Object.values(result.errors).join('；')
      ElMessage.error(msgs)
    } else {
      ElMessage.error(`内容填写有误，请核对操作手册！（当前错误次数: ${result.errorCount}）`)
    }
    return
  }

  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: result.stats.duration_seconds,
    operationCount: result.stats.operation_count,
    errorCount: result.stats.error_count,
    score: 100.00,
    resultData: JSON.stringify(result.data),
    startedAt: new Date().toISOString()
  }

  try {
    const submitRes = await submitStep(payload)
    ElMessage.success('提交成功！即将进入工器具选择...')
    // 跳转到下一步（工器具选择），传递 experimentId
    setTimeout(() => {
      router.push({
        path: '/WIS',
        query: { experimentId: experimentId.value }
      })
    }, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}
</script>

<style scoped>
/* 整个实验场景外层，通常铺满屏幕 */
.experiment-scene {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/images/HWTBackgroundImage.jpg');
  /* 可以替换成你的高压场景背景图 */
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

/* 核心要求：限制区域大小，其他内容通过滚动显示 */
.scroll-wrapper {
  width: 900px;
  height: 650px;
  overflow-y: auto;
  background-color: rgba(255, 255, 255, 0.9);
  /* 半透明背景增加景深感 */
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* 自定义滚动条，使其风格契合仿真平台 */
.scroll-wrapper::-webkit-scrollbar {
  width: 8px;
}

.scroll-wrapper::-webkit-scrollbar-thumb {
  background: #a0a5aa;
  border-radius: 4px;
}

.scroll-wrapper::-webkit-scrollbar-thumb:hover {
  background: #7a8085;
}
</style>
