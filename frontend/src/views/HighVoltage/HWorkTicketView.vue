<template>
  <div class="experiment-scene">
    <WorkTicketForm ref="formRef" :finalize="isFinalize" @submit-ticket="handleTicketSubmit" />
    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="currentStepSeconds" />
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgress" title="保存进度" />

    <!-- 查看工作任务按钮（左下角） -->
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>

    <!-- 视频1：工作票填写完（播放完毕自动进入工器具选择） -->
    <HVideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitStep, saveDraft, getStepDraft, completeExperiment } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import WorkTicketForm from '@/components/HighVoltage/HWorkTicketForm.vue'
import HVideoOverlay from '@/components/HighVoltage/HVideoOverlay.vue'
import Images from '@/constants/images'
import Videos from '@/constants/videos'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const showWorkBg = ref(false)
// 视频1：工作票填写完播放（播毕进工器具选择）
const showVideo = ref(false)
const nextStepId = ref('')

// 从路由 query 获取实验元数据
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
// 步骤24 办理工作终结模式：跳转后补全终结内容并结束实验
const isFinalize = computed(() => route.query.finalize === '1')

// 当前步骤实时秒数（来自子组件 WorkTicketForm 的 stats）
const currentStepSeconds = computed(() => formRef.value?.stats?.duration_seconds ?? 0)
// 页面加载时记录步骤开始时间
const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)

// 恢复草稿数据到表单
onMounted(async () => {
  if (!experimentId.value || !stepId.value) return
  try {
    const draft = await getStepDraft(experimentId.value, stepId.value)
    if (draft && Object.keys(draft).length > 0 && formRef.value) {
      Object.assign(formRef.value.formData, draft)
    }
  } catch (_) {
    /* ignore */
  }
})

// 保存进度（全量表单数据）
const saveProgress = async () => {
  saving.value = true
  try {
    const fullData = formRef.value ? JSON.parse(JSON.stringify(formRef.value.formData)) : {}
    await saveDraft({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 0,
      durationSeconds: formRef.value?.stats?.duration_seconds ?? 0,
      resultData: JSON.stringify(fullData),
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

// 接收子组件抛出的提交事件
const handleTicketSubmit = async result => {
  if (!result.success) {
    if (result.errors && Object.keys(result.errors).length > 0) {
      const msgs = Object.values(result.errors).join('；')
      ElMessage.error(msgs)
    } else {
      ElMessage.error(`内容填写有误，请核对操作手册！（当前错误次数: ${result.errorCount}）`)
    }
    return
  }

  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: result.stats.duration_seconds,
    operationCount: result.stats.operation_count,
    errorCount: result.stats.error_count,
    score: 100.0 - result.stats.error_count * 10 > 0 ? 100.0 - result.stats.error_count * 10 : 0, //最低得分为0分
    resultData: JSON.stringify(result.data),
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    // 步骤24 办理工作终结：补全并提交后更新后端并结束实验
    if (isFinalize.value) {
      try {
        await completeExperiment(experimentId.value)
      } catch (_) {
        /* 完成标记失败不阻断流程 */
      }
      ElMessage.success('工作票终结办理完成，实验结束！')
      setTimeout(() => {
        router.push({
          path: '/experiment',
          query: { experimentId: experimentId.value }
        })
      }, 1000)
      return
    }
    // 从 localStorage 获取下一步 stepId（按实验 ID 区分，避免步骤映射错位）
    const steps = JSON.parse(localStorage.getItem('experimentSteps_' + experimentId.value) || '[]')
    const nextStep = steps.find(s => s.stepOrder === 2)
    nextStepId.value = nextStep ? nextStep.stepId : ''
    ElMessage.success('提交成功，即将播放工作票教学视频...')
    // 视频1：工作票填写完播放，播毕进入工器具选择
    showVideo.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}

/** 视频1 播放完毕 → 进入工器具选择（步骤2） */
function onVideoEnded() {
  showVideo.value = false
  router.push({
    path: '/HTS',
    query: {
      experimentId: experimentId.value,
      stepId: nextStepId.value
    }
  })
}
</script>

<style scoped>
.experiment-scene {
  overflow-x: clip;
  width: 100vw;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10vh 0 9vh;
  box-sizing: border-box;
  background: var(--img-hwt-bg) center / cover no-repeat;
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
