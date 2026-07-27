/* 实验选择页面，选择进行高压场景模拟还是低压场景模拟，以及选择训练模式以及挑战模式的悬浮窗 */

<template>
  <div class="experiment-view">
    <ScenarioSelector @select="onScenarioSelect" />

    <!-- 差异介绍须知弹窗 -->
    <PromptModal :visible="showNotice" @close="onNoticeClose" :button-bottom="'10%'">
      <img src="@/assets/images/SimulationDisclaimerNotice.png" alt="差异介绍须知" class="work-bg-img" />
    </PromptModal>

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="onWorkBgClose">
      <img src="@/assets/images/HighWorkBackground.png" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ScenarioSelector from '@/components/ScenarioSelector.vue'
import PromptModal from '@/components/PromptModal.vue'
import { startExperiment, getUnfinishedExperiments, deleteExperiment } from '@/api/experiment'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const showNotice = ref(false)
const showWorkBg = ref(false)
let pendingType = null  // 暂存场景类型，供弹窗关闭后使用

async function onScenarioSelect(type) {
  // 直接启动（内部会检查未完成实验，从存档恢复则跳过提示弹窗）
  await doStartExperiment(type)
}

async function doStartExperiment(type) {
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
          {
            confirmButtonText: '继续实验', cancelButtonText: '重新开始', type: 'info',
            distinguishCancelAndClose: true
          }
        )
        // 继续 → 跳转到当前未完成步骤（不展示提示弹窗）
        sessionStorage.setItem('experimentId', exp.experimentId)
        const stepRouteMap = type === 'high'
          ? { 1: '/HWT', 2: '/HTS', 3: '/HCL', 4: '/HCL', 5: '/HCL' }
          : { 1: '/LWT', 2: '/LTS' }
        const path = stepRouteMap[exp.nextStepOrder] || (type === 'high' ? '/HWT' : '/LWT')
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
    // 无未完成记录 或 重新开始 → 展示提示弹窗，再启动实验
    pendingType = type
    showNotice.value = true
  } catch (_) {
    ElMessage.error('查询实验记录失败，请稍后重试')
  }
}

// 关闭须知 → 展示工作背景
function onNoticeClose() {
  showNotice.value = false
  if (pendingType === 'high') {
    showWorkBg.value = true
  } else {
    // 低压暂不展示工作背景，直接启动
    startNewExperiment(pendingType)
  }
}

// 关闭工作背景 → 启动实验
function onWorkBgClose() {
  showWorkBg.value = false
  startNewExperiment(pendingType)
}

async function startNewExperiment(type) {
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
  background-image: url(@/assets/images/ExperimentViewBackground.jpg);
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
