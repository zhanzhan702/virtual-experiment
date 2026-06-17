/* 实验选择页面，选择进行高压场景模拟还是低压场景模拟，以及选择训练模式以及挑战模式的悬浮窗 */

<template>
  <div class="experiment-view">
    <ScenarioSelector @select="onScenarioSelect" />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ScenarioSelector from '@/components/ScenarioSelector.vue'
import { startExperiment } from '@/api/experiment'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

async function onScenarioSelect(type) {
  // 模板ID（从 DataInitializer 预置）
  const templateId = 'a1b2c3d4e5f67890abcdef1234567890'
  try {
    const res = await startExperiment(templateId)
    ElMessage.success('实验已启动！')
    // 跳转到第一步（填写工作票），带 experimentId
    router.push({
      path: '/HWT',
      query: {
        experimentId: res.experimentId,
        stepId: res.steps[0].stepId  // 第一步
      }
    })
  } catch (err) {
    ElMessage.error('启动实验失败：' + (err.response?.data?.message || err.message))
  }
}
</script>

<style scoped>
.experiment-view{
    width: 100vw;
    height: 100vh;
    padding: 10vh 10vw;
    background-image:url(@/assets/images/ExperimentViewBackgroundImage.jpg) ;
    background-size: contain;          /* 让图片等比缩放铺满容器，多余部分裁剪 */
    background-position: center;     /* 居中显示 */
    background-repeat: no-repeat;    /* 不重复 */
    display: flex;
    flex-direction: column;
    align-items: center;
}
</style>
