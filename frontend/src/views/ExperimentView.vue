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
import { startExperiment, getUnfinishedExperiments, deleteExperiment } from '@/api/experiment'
import { getHighVoltageStepPath } from '@/constants/experiment-step-routes'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

async function onScenarioSelect(type) {
  // 先检查是否有未完成实验（按场景类型过滤）
  try {
    const category = type === 'high' ? 'high_voltage' : 'low_voltage'
    const allList = await getUnfinishedExperiments()
    const list = allList.filter(e => e.category === category)
    if (list.length > 0) {
      const exp = list[0]
      try {
        await ElMessageBox.confirm(
          `检测到未完成实验「${exp.templateName}」，已完成 ${exp.completedSteps}/${exp.totalSteps} 步。是否继续？`,
          '恢复实验',
          { confirmButtonText: '继续实验', cancelButtonText: '重新开始', type: 'info',
            distinguishCancelAndClose: true }
        )
        // 继续 → 跳转到当前未完成步骤
        sessionStorage.setItem('experimentId', exp.experimentId)
        const path = type === 'high'
          ? getHighVoltageStepPath(exp.nextStepOrder)
          : ({ 1: '/LWT', 2: '/LTS' }[exp.nextStepOrder] || '/LWT')
        router.push({ path, query: { experimentId: exp.experimentId, stepId: exp.nextStepId } })
        return
      } catch (action) {
        // 点 × 或遮罩 → 关闭弹窗，不做任何操作
        if (action !== 'cancel') return
        // 点"重新开始" → 删除所有未完成实验
        for (const e of list) {
          try { await deleteExperiment(e.experimentId) } catch (_) { /* ignore */ }
        }
      }
    }
    // 无未完成记录 或 重新开始 → 启动实验
    await doStartExperiment(type)
  } catch (_) {
    ElMessage.error('查询实验记录失败，请稍后重试')
  }
}

async function doStartExperiment(type) {
  const templateCode = type == 'high' ? 'HV_TRAIN_V1' : 'LV_TRAIN_V1'
  try {
    const res = await startExperiment(templateCode)
    sessionStorage.setItem('experimentSteps', JSON.stringify(res.steps))
    ElMessage.success('实验已启动！')
    router.push({
      path: type == 'high' ? '/HWT' : '/LWT',
      query: { experimentId: res.experimentId, stepId: res.steps[0].stepId }
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
