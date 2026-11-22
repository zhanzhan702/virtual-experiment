<template>
  <main class="pole-transformer-view">
    <LPoleTransformerScene
      ref="sceneRef"
      :stage="stage"
      :disabled="!canInteract"
      @complete="onStageComplete"
      @operation="operationCount++"
      @error="errorCount++"
    />

    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="durationSeconds" />

    <div v-if="canInteract" class="save-bar-fixed" :class="{ saving }" @click="saveProgress" />

    <div v-if="hasMetadata" class="work-task-btn" @click="showTask = true" title="查看工作任务" />

    <!-- 缺元数据 / 恢复失败：遮罩挡住场景，避免空状态下误操作覆盖存档 -->
    <div v-if="!hasMetadata || restoreFailed" class="blocker" />

    <div v-if="!hasMetadata" class="load-panel" role="status">
      <p>缺少实验信息，请从实验选择页进入低压场景。</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </div>

    <div v-else-if="restoreFailed" class="load-panel" role="status">
      <p>场景进度恢复失败，重试成功后可继续，已有存档不会被覆盖。</p>
      <el-button type="primary" @click="handleRestore">重新加载</el-button>
    </div>

    <!-- 三步验电完成提示：与居民表箱使用同一张弹窗图 -->
    <PromptModal
      :visible="showElectrifyNotice"
      @close="onElectrifyNoticeClose"
      :button-bottom="'18%'"
    >
      <img :src="Images.lowElectrifyCompleteNotice" alt="验电完成提示" class="work-bg-img" />
    </PromptModal>

    <!-- 工作任务浮窗（低压工作背景图） -->
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
import LPoleTransformerScene from '@/components/LowVoltage/LPoleTransformerScene.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentLogout from '@/components/ExperimentLogout.vue'
import Images from '@/constants/images'
import { getStepDraft, getExperimentSteps, saveDraft, submitStep } from '@/api/experiment'

const route = useRoute()
const router = useRouter()

// 必须是 computed：步骤推进时会用 router.replace 换掉 stepId，
// 用普通常量的话 stage 不会更新，页面会一直停在上一阶段的逻辑里
const experimentId = computed(() =>
  typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
)
const stepId = computed(() => (typeof route.query.stepId === 'string' ? route.query.stepId : ''))
const hasMetadata = computed(() => !!experimentId.value && !!stepId.value)

const sceneRef = ref(null)
const showTask = ref(false)
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)
// 三步验电已提交（弹窗展示期间锁定交互，避免重复提交）
const electrifyDone = ref(false)
const showElectrifyNotice = ref(false)
const durationSeconds = ref(0)
const operationCount = ref(0)
const errorCount = ref(0)

let timer = null

/** 步骤映射表：实验启动时写入 localStorage（与居民表箱一致） */
const steps = ref([])

function loadStepsFromStorage() {
  try {
    steps.value = JSON.parse(localStorage.getItem('experimentSteps_' + experimentId.value) || '[]')
  } catch {
    steps.value = []
  }
}

// 先在 setup 阶段同步读一次，保证首屏就按正确的阶段渲染
loadStepsFromStorage()

/**
 * 当前阶段：8 = 架设围栏并悬挂标识牌，9 = 三步式验电。
 *
 * 优先读 URL 的 stepOrder（与居民表箱 /LRB 一致）：从存档返回时
 * localStorage 的步骤映射可能缺失，只靠映射会把第 9 步误判成第 8 步。
 */
const stage = computed(() => {
  const fromQuery = Number(route.query.stepOrder)
  if (fromQuery >= 8) return fromQuery
  return steps.value.find(s => s.stepId === stepId.value)?.stepOrder ?? 8
})

const canInteract = computed(
  () =>
    hasMetadata.value &&
    !restoreFailed.value &&
    !saving.value &&
    !submitting.value &&
    !electrifyDone.value
)

async function handleRestore() {
  if (!hasMetadata.value) return
  restoreFailed.value = false
  try {
    // 步骤映射丢失时（新标签页 / 清过缓存）从后端重建
    loadStepsFromStorage()
    if (steps.value.length < 2) {
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
    if (draft && Object.keys(draft).length > 0) sceneRef.value?.restore(draft)
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
    resultData: JSON.stringify({ stage: stage.value, ...(sceneRef.value?.getState() || {}) })
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

/** 本阶段完成：提交当前步骤，推进到下一步 */
async function onStageComplete() {
  if (!canInteract.value) return
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  submitting.value = true
  try {
    await submitStep(buildPayload(1))

    // 步骤 9 验电完成：弹出验电完成提示；柜内集中器安装留待下一阶段开发
    if (stage.value === 9) {
      electrifyDone.value = true
      showElectrifyNotice.value = true
      ElMessage.success('验电操作完成！')
      return
    }

    const next = steps.value.find(s => s.stepOrder === stage.value + 1)
    if (!next) {
      ElMessage.warning('未找到下一步骤，请确认数据库已执行第 9 步脚本')
      return
    }
    await router.replace({
      query: { experimentId: experimentId.value, stepId: next.stepId, stepOrder: next.stepOrder }
    })
    await handleRestore()
    timer = setInterval(() => durationSeconds.value++, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

/** 验电完成提示关窗：柜内场景尚未开发，先给出提示并停留在完成态 */
function onElectrifyNoticeClose() {
  showElectrifyNotice.value = false
  ElMessage.success('杆上变外部操作完成，柜内集中器安装将在下一阶段开发')
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
.pole-transformer-view {
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

/* 提示弹窗图片尺寸与居民表箱页保持一致 */
.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
