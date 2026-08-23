<template>
  <div class="tool-selection-view">
    <WizardInventorySelection
      ref="wizardRef"
      :categories="categories"
      @finish="handleToolSelectionSubmit"
      @operation="handleOperation"
      @submit-error="handleSubmitError"
    />

    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="stats.duration_seconds" />
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgress" title="保存进度" />

    <!-- 查看工作任务按钮（左下角） -->
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>

    <!-- 视频2：工器具选择完（播放完毕自动进入下一步） -->
    <HVideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import WizardInventorySelection from '@/components/HighVoltage/HWizardInventorySelection.vue'
import HVideoOverlay from '@/components/HighVoltage/HVideoOverlay.vue'
import { categories } from '@/constants/tool-selection-config'
import Images from '@/constants/images'
import Videos from '@/constants/videos'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()

const wizardRef = ref(null)
const showWorkBg = ref(false)
// 视频2：工器具选择完播放（播毕进入下一步）
const showVideo = ref(false)

// 从路由 query 获取实验元数据
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')

// 页面加载时记录步骤开始时间
const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)

// 恢复草稿数据
onMounted(async () => {
  if (!experimentId.value || !stepId.value) return
  try {
    const draft = await getStepDraft(experimentId.value, stepId.value)
    if (draft && Object.keys(draft).length > 0 && wizardRef.value) {
      Object.assign(wizardRef.value.selectedMap, draft)
    }
  } catch (_) {
    /* ignore */
  }
})

// 保存进度（全量选择数据）
const saveProgress = async () => {
  saving.value = true
  try {
    const fullData = wizardRef.value ? JSON.parse(JSON.stringify(wizardRef.value.selectedMap)) : {}
    await saveDraft({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 0,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
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

// 操作统计
const stats = reactive({
  duration_seconds: 0,
  operation_count: 0,
  error_count: 0
})

let timer = null
onMounted(() => {
  timer = setInterval(() => {
    stats.duration_seconds++
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 统计操作次数（每次点击/选择工器具）
const handleOperation = () => {
  stats.operation_count++
}

// 统计提交错误次数
const handleSubmitError = errorPageCount => {
  stats.error_count += errorPageCount
}

const handleToolSelectionSubmit = async selectedMap => {
  // 提交时立即冻结计时器，避免等待跳转期间 stats 继续增长导致显示与提交值不一致
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: stats.duration_seconds,
    operationCount: stats.operation_count,
    errorCount: stats.error_count,
    score: 100.0 - stats.error_count * 10 > 0 ? 100.0 - stats.error_count * 10 : 0, //最低得分为0分
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('工器具选择已完成，即将播放教学视频...')
    // 视频2：工器具选择完播放，播毕进入下一步
    showVideo.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}

/** 视频2 播放完毕 → 进入配电室总览/下一步 */
function onVideoEnded() {
  showVideo.value = false
  router.push({
    path: '/HSO',
    query: { experimentId: experimentId.value }
  })
}
</script>

<style scoped>
.tool-selection-view {
  position: relative;
  padding: 28px;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: linear-gradient(180deg, rgba(240, 245, 255, 0.82) 0%, rgba(245, 247, 250, 0.82) 100%);
}

.tool-selection-view::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: var(--img-tool-selection-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
}

/* 内容元素盖在背景之上；固定定位按钮（保存进度/查看工作任务）除外，保持 main.css 的 fixed 定位 */
.tool-selection-view > :not(.save-bar-fixed):not(.work-task-btn) {
  position: relative;
  z-index: 1;
}

/* 保存进度/查看工作任务按钮样式见 assets/styles/main.css */

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
