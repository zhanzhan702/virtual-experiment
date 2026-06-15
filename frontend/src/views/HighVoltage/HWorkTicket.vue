<template>
  <div class="experiment-scene" >
    <div class="scroll-wrapper">
      <WorkTicketForm @submit-ticket="handleTicketSubmit" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import WorkTicketForm from '@/components/HighVoltage/HWorkTicketForm.vue' // 确保路径对应你的项目结构

// 从路由或全局状态获取实验元数据
const experimentId = "123e4567-e89b-12d3-a456-426614174000" // 需替换为实际的 UUID
const stepId = "987f6543e21b73d3-b456426614174111"       // 需替换为实际的 UUID

// 接收子组件抛出的提交事件
const handleTicketSubmit = async (result) => {
  if (!result.success) {
    // 显示具体字段错误
    if (result.errors && Object.keys(result.errors).length > 0) {
      const msgs = Object.values(result.errors).join('；')
      ElMessage.error(msgs)
    } else {
      ElMessage.error(`内容填写有误，请核对操作手册！（当前错误次数: ${result.errorCount}）`)
    }
    return
  }

  // 组装发送给后端的 Payload，契合你的 MySQL 数据库字段设计
  const payload = {
    experiment_id: experimentId,
    step_id: stepId,
    status: 1,
    duration_seconds: result.stats.duration_seconds,
    operation_count: result.stats.operation_count,
    error_count: result.stats.error_count,
    score: 100.00,
    result_data: JSON.stringify(result.data) // 将填写的表单内容序列化存储在 JSON 列中
  }

  try {
    const response = await axios.post('/api/experiment/step/submit', payload)

    if (response.status === 200) {
      ElMessage.success('提交成功！即将进入“工作负责人布置工作任务”环节...')

      // TODO: 在此调用 Vue Router 进行下一步跳转
      // router.push({ name: 'NextTaskScene' })
    }
  } catch (error) {
    ElMessage.error('服务器提交失败，请检查网络或后端接口。')
    console.error(error)
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
  background-image: url('@/assets/images/HWTBackgroundImage.jpg'); /* 可以替换成你的高压场景背景图 */
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

/* 核心要求：限制区域大小，其他内容通过滚动显示 */
.scroll-wrapper {
  width: 900px;
  height: 650px;
  overflow-y: auto;
  background-color: rgba(255, 255, 255, 0.9); /* 半透明背景增加景深感 */
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