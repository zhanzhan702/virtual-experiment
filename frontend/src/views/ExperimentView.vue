/* 实验选择页面，选择进行高压场景模拟还是低压场景模拟，以及选择训练模式以及挑战模式的悬浮窗 */

<template>
  <div class="experiment-view">
    <ScenarioSelector @select="onScenarioSelect" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ScenarioSelector from '@/components/ScenarioSelector.vue'
import { startExperiment, getUnfinishedExperiments, deleteExperiment } from '@/api/experiment'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

// 页面加载时检查是否有未完成实验
onMounted(async () => {
  try {
    const list = await getUnfinishedExperiments()
    if (list.length === 0) return
    const exp = list[0]
    try {
      await ElMessageBox.confirm(
        `检测到未完成实验「${exp.templateName}」，已完成 ${exp.completedSteps}/${exp.totalSteps} 步。是否继续？`,
        '恢复实验',
        { confirmButtonText: '继续实验', cancelButtonText: '重新开始', type: 'info' }
      )
      // 继续 → 跳转到当前未完成步骤
      const stepRouteMap = { 1: '/HWT', 2: '/HTS' }
      const path = stepRouteMap[exp.nextStepOrder] || '/HWT'
      router.push({
        path,
        query: { experimentId: exp.experimentId, stepId: exp.nextStepId }
      })
    } catch {
      // 用户点"重新开始" → 删除所有未完成实验
      for (const e of list) {
        try { await deleteExperiment(e.experimentId) } catch (_) { /* ignore */ }
      }
    }
  } catch (_) { /* 忽略查询失败 */ }
})

async function onScenarioSelect(type) {
  // 按模板编码查询（后端需支持按 code 查模板，或前端用 templateCode 参数）
  const templateCode = type == 'high' ? 'HV_TRAIN_V1' : 'LV_TRAIN_V1'
  try {
    const res = await startExperiment(templateCode)
    // 存储全部步骤供后续页面使用
    sessionStorage.setItem('experimentSteps', JSON.stringify(res.steps))
    ElMessage.success('实验已启动！')
    router.push({
      path: type == 'high' ? '/HWT' : '/LWT',
      query: {
        experimentId: res.experimentId,
        stepId: res.steps[0].stepId
      }
    })
  } catch (err) {
    ElMessage.error('启动实验失败：' + (err.response?.data?.message || err.message))
  }
}
</script>

<style scoped>
.experiment-view {
  width: 100vw;
  height: 100vh;
  padding: 10vh 10vw;
  background-image: url(@/assets/images/ExperimentViewBackgroundImage.jpg);
  background-size: contain;
  /* 让图片等比缩放铺满容器，多余部分裁剪 */
  background-position: center;
  /* 居中显示 */
  background-repeat: no-repeat;
  /* 不重复 */
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
