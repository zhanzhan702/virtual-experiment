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
    <div
      v-if="canWrite"
      class="save-bar-fixed"
      :class="{ saving }"
      @click="saveProgress"
      title="保存进度"
    />

    <div v-if="hasMetadata" class="work-task-btn" @click="showTask = true" title="查看工作任务" />

    <!--
      遮罩：缺元数据或恢复失败时挡住下方的选择界面。
      否则用户可以在空选择状态下继续操作并提交，把已有的存档覆盖掉 ——
      而失败面板上写的是「已有存档不会被覆盖」，必须真的做到。
    -->
    <div v-if="!hasMetadata || restoreFailed" class="blocker" />

    <!-- 缺少实验信息 -->
    <div v-if="!hasMetadata" class="load-panel" role="status">
      <p>缺少实验信息，请从实验选择页进入低压场景。</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </div>

    <!-- 恢复失败：显式提示，避免存档丢失变成无从排查的问题 -->
    <div v-else-if="restoreFailed" class="load-panel" role="status">
      <p>工器具选择进度恢复失败，重试成功后可继续，已有存档不会被覆盖。</p>
      <el-button type="primary" @click="handleRestore">重新加载</el-button>
    </div>

    <PromptModal :visible="showTask" @close="showTask = false">
      <article class="task-notice">
        <h2>低压场景工器具选择</h2>
        <p>本次作业：集中器安装与调试、单相电能表安装。</p>
        <p>请在「个人防护 / 终端 / 工器具 / 线材」四个分类中选齐所需物品。</p>
        <p>线材需区分规格与线型，多选或漏选都无法进入下一步。</p>
      </article>
    </PromptModal>

    <VideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import WizardInventorySelection from '@/components/WizardInventorySelection.vue'
import VideoOverlay from '@/components/VideoOverlay.vue'
import { categories } from '@/constants/low-voltage-tool-selection-config'
import Videos from '@/constants/videos'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()

const wizardRef = ref(null)
const showTask = ref(false)
const showVideo = ref(false)

const experimentId = typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
const stepId = typeof route.query.stepId === 'string' ? route.query.stepId : ''
const hasMetadata = !!experimentId && !!stepId

const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)

const canWrite = computed(
  () => hasMetadata && !restoreFailed.value && !saving.value && !submitting.value
)

const stats = reactive({ duration_seconds: 0, operation_count: 0, error_count: 0 })
let timer = null

/** 恢复草稿：失败时置 restoreFailed，页面给出重试入口而不是静默吞掉 */
async function handleRestore() {
  if (!hasMetadata) return
  restoreFailed.value = false
  try {
    const draft = await getStepDraft(experimentId, stepId)
    if (draft && Object.keys(draft).length > 0 && wizardRef.value) {
      Object.assign(wizardRef.value.selectedMap, draft)
    }
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  }
}

/** 保存进度（全量选择数据） */
async function saveProgress() {
  if (!canWrite.value) return
  saving.value = true
  try {
    const fullData = wizardRef.value
      ? JSON.parse(JSON.stringify(wizardRef.value.selectedMap))
      : {}
    await saveDraft({
      experimentId,
      stepId,
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

const handleOperation = () => {
  stats.operation_count++
}

const handleSubmitError = errorPageCount => {
  stats.error_count += errorPageCount
}

async function handleToolSelectionSubmit(selectedMap) {
  // 提交时冻结计时器，避免等待跳转期间 stats 继续增长导致显示与提交值不一致
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  const score = Math.max(0, 100.0 - stats.error_count * 10)
  submitting.value = true
  try {
    await submitStep({
      experimentId,
      stepId,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score,
      resultData: JSON.stringify(selectedMap),
      startedAt: startedAt.value
    })
    ElMessage.success('工器具选择已完成，即将播放教学视频…')
    showVideo.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

/** 视频播放完毕 → 进入低压场景总览（占位页） */
function onVideoEnded() {
  showVideo.value = false
  router.push({ path: '/LSO', query: { experimentId } })
}

onMounted(() => {
  handleRestore()
  timer = setInterval(() => {
    stats.duration_seconds++
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

/* 盖在选择界面之上、提示面板之下（提示面板 z-index 90） */
.blocker {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(0, 0, 0, 0.35);
}

.load-panel {
  position: fixed;
  z-index: 90;
  top: 38%;
  left: 50%;
  transform: translateX(-50%);
  background: #fffdf5;
  border: 3px solid #078c83;
  border-radius: 12px;
  padding: 1.5em;
  max-width: 85vw;
  text-align: center;
  color: #123f68;
  box-shadow: 0 5px 30px #0003;
}

.load-panel p {
  margin-bottom: 1em;
}

.task-notice {
  width: min(650px, 82vw);
  max-height: 80vh;
  overflow-y: auto;
  box-sizing: border-box;
  border: 6px solid #008d85;
  border-radius: 24px;
  background: #fffdf5;
  color: #123f68;
  padding: 5% 7% 20%;
  font-size: clamp(15px, 1.4vw, 21px);
  line-height: 1.8;
}

.task-notice h2 {
  text-align: center;
  margin-bottom: 0.6em;
  font-size: 1.5em;
}

.task-notice p {
  margin-bottom: 0.8em;
}
</style>
