<!-- 居民表箱内部操作：三步验电完成后进入，使用 Leafer UI 画布 -->
<template>
  <main class="resident-box-inner-view">
    <LResidentBoxInnerCanvas
      ref="canvasRef"
      :step-order="stage"
      :experiment-id="experimentId"
      :step-id="stepId"
      :disabled="!canInteract"
      @operation="operationCount++"
      @error="errorCount++"
      @complete="onStepComplete"
    />

    <!-- 左侧工具栏：与高压柜体局部一致，常驻 4 个物品；
         「请勿合闸」标识牌将在表箱内部操作中使用 -->
    <ToolBarLeft
      :items="LEFT_TOOLS"
      :placed-mask="placedMask"
      :active-idx="activeTool"
      @select="onSelectTool"
    />

    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="durationSeconds" />

    <div v-if="canInteract" class="save-bar-fixed" :class="{ saving }" @click="saveProgress" />

    <div v-if="hasMetadata" class="work-task-btn" @click="showTask = true" title="查看工作任务" />

    <!-- 缺元数据 / 恢复失败：遮罩挡住画布，避免空状态下误操作覆盖存档 -->
    <div v-if="!hasMetadata || restoreFailed" class="blocker" />

    <div v-if="!hasMetadata" class="load-panel" role="status">
      <p>缺少实验信息，请从实验选择页进入低压场景。</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </div>

    <div v-else-if="restoreFailed" class="load-panel" role="status">
      <p>场景进度恢复失败，重试成功后可继续，已有存档不会被覆盖。</p>
      <el-button type="primary" @click="handleRestore">重新加载</el-button>
    </div>

    <div v-if="stepDone" class="completion-banner" role="status">✓ 居民表箱内部操作已完成</div>

    <!-- 工作任务浮窗（低压工作背景图，与表箱外部场景一致） -->
    <PromptModal :visible="showTask" @close="showTask = false" :button-bottom="'12%'">
      <img :src="Images.lowWorkTaskNotice" alt="低压工作背景" class="work-bg-img" />
    </PromptModal>
  </main>
  <ExperimentLogout />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LResidentBoxInnerCanvas from '@/components/LowVoltage/LResidentBoxInnerCanvas.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentLogout from '@/components/ExperimentLogout.vue'
import ToolBarLeft from '@/components/ToolBarLeft.vue'
import Images from '@/constants/images'
import { getStepDraft, getExperimentSteps, saveDraft, submitStep } from '@/api/experiment'

const route = useRoute()
const router = useRouter()

const experimentId = computed(() =>
  typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
)
const stepId = computed(() => (typeof route.query.stepId === 'string' ? route.query.stepId : ''))
const hasMetadata = computed(() => !!experimentId.value && !!stepId.value)

const canvasRef = ref(null)
const showTask = ref(false)
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)
const stepDone = ref(false)
const durationSeconds = ref(0)
const operationCount = ref(0)
const errorCount = ref(0)

// ── 左侧工具栏 ────────────────────────────────────────────
// 顺序与高压柜体局部一致：围栏、止步高压危险、在此工作、请勿合闸
const LEFT_TOOLS = [
  { name: '围栏', img: Images.barLeftFence },
  { name: '有电危险标识牌', img: Images.barSignStopHighVoltage },
  { name: '有人工作标识牌', img: Images.barSignPersonWorking },
  { name: '请勿合闸标识牌', img: Images.barSafetyNotice }
]
// 本页面暂无已放置物品；后续放置逻辑补上后再由画布状态驱动
const placedMask = [false, false, false, false]
const activeTool = ref(null)

function onSelectTool(idx) {
  if (!canInteract.value) return
  operationCount.value++
  // 放置逻辑待画布内容补充，先提供选中反馈
  activeTool.value = activeTool.value === idx ? null : idx
}

let timer = null

/**
 * 步骤映射表：实验启动时写入 localStorage。
 * 与居民表箱外部场景一致，用 ref 以便恢复流程从后端补拉后同步更新。
 */
const steps = ref([])

function loadStepsFromStorage() {
  try {
    steps.value = JSON.parse(localStorage.getItem('experimentSteps_' + experimentId.value) || '[]')
  } catch {
    steps.value = []
  }
}

loadStepsFromStorage()

/**
 * 当前步骤序号；优先读 URL 的 stepOrder（与高压一致），
 * 避免存档返回时 localStorage 映射缺失导致阶段误判
 */
const stage = computed(() => {
  const fromQuery = Number(route.query.stepOrder)
  if (fromQuery >= 5) return fromQuery
  return steps.value.find(s => s.stepId === stepId.value)?.stepOrder ?? 5
})

const canInteract = computed(
  () =>
    hasMetadata.value &&
    !restoreFailed.value &&
    !saving.value &&
    !submitting.value &&
    !stepDone.value
)

async function handleRestore() {
  if (!hasMetadata.value) return
  restoreFailed.value = false
  try {
    loadStepsFromStorage()
    if (!steps.value.length) {
      try {
        const all = await getExperimentSteps(experimentId.value)
        if (all?.length) {
          localStorage.setItem('experimentSteps_' + experimentId.value, JSON.stringify(all))
          steps.value = all
        }
      } catch {
        /* 拉不到就按 URL 里的 stepId 原样尝试 */
      }
    }
    const draft = await getStepDraft(experimentId.value, stepId.value)
    if (draft && Object.keys(draft).length > 0) canvasRef.value?.restore(draft)
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  }
}

function buildPayload(status) {
  return {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status,
    durationSeconds: durationSeconds.value,
    operationCount: operationCount.value,
    errorCount: errorCount.value,
    ...(status === 1 ? { score: Math.max(0, 100 - errorCount.value * 10) } : {}),
    resultData: JSON.stringify({ stage: stage.value, ...(canvasRef.value?.getState() || {}) })
  }
}

async function saveProgress() {
  if (!canInteract.value) return
  saving.value = true
  try {
    await saveDraft(buildPayload(0))
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

/** 画布内容完成后提交本步骤（后续内容补充时由画布 emit('complete') 触发） */
async function onStepComplete() {
  if (!canInteract.value) return
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  submitting.value = true
  try {
    await submitStep(buildPayload(1))
    stepDone.value = true
    ElMessage.success('表箱内部操作完成')
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  handleRestore()
  timer = setInterval(() => durationSeconds.value++, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.resident-box-inner-view {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

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

.completion-banner {
  position: fixed;
  top: 2vh;
  left: 50%;
  transform: translateX(-50%);
  color: #08766a;
  background: #f0fff8;
  border: 2px solid #078c83;
  border-radius: 20px;
  padding: 0.4em 1em;
  z-index: 90;
  font-size: clamp(14px, 1.4vw, 22px);
}

/* 提示弹窗图片尺寸与表箱外部场景保持一致 */
.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
