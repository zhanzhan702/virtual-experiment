<template>
  <main class="resident-box-view">
    <LResidentBoxScene
      ref="sceneRef"
      :stage="stage"
      :disabled="!canInteract || (stage >= 5 && !boxInnerDone)"
      :door-ready="stage === 7 && boxInnerDone"
      :right-tool="canvasRightTool"
      @complete="onStageComplete"
      @operation="operationCount++"
      @error="errorCount++"
      @left-tool-select="canvasLeftTool = $event"
      @right-tool-select="onCanvasRightTool"
      @door-sealed="onDoorSealed"
    />

    <!-- 步骤 5~7：居民表箱内部 Leafer 画布叠加在外部场景之上；表体铅封完成后卸载 -->
    <LResidentBoxCabinetCanvas
      v-if="stage >= 5 && !boxInnerDone"
      ref="canvasRef"
      :stage="stage"
      :disabled="!canInteract"
      :left-tool="canvasLeftTool"
      :right-tool="canvasRightTool"
      :experiment-id="experimentId"
      :step-id="stepId"
      @operation="operationCount++"
      @error="errorCount++"
      @complete="onStageComplete"
      @box-done="onBoxInnerDone"
      @tool-used="onCanvasToolUsed"
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

    <!-- 三步验电完成提示：低压柜门验电完成浮窗 -->
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
import LResidentBoxScene from '@/components/LowVoltage/LResidentBoxScene.vue'
import LResidentBoxCabinetCanvas from '@/components/LowVoltage/LResidentBoxCabinetCanvas.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentLogout from '@/components/ExperimentLogout.vue'
import Images from '@/constants/images'
import { getStepDraft, getExperimentSteps, saveDraft, submitStep } from '@/api/experiment'

const route = useRoute()
const router = useRouter()

// 必须是 computed：步骤 3 完成后会用 router.replace 换掉 stepId，
// 用普通常量的话 stage 不会更新，页面会一直停在步骤 3 的逻辑里
const experimentId = computed(() =>
  typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
)
const stepId = computed(() => (typeof route.query.stepId === 'string' ? route.query.stepId : ''))
const hasMetadata = computed(() => !!experimentId.value && !!stepId.value)

const sceneRef = ref(null)
const canvasRef = ref(null)
const showTask = ref(false)
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)
// 三步验电已提交（弹窗展示期间锁定交互，避免重复提交）
const electrifyDone = ref(false)
// 三步验电完成提示弹窗（与高压一致，确认后进入表箱内部画布）
const showElectrifyNotice = ref(false)
const durationSeconds = ref(0)
const operationCount = ref(0)
const errorCount = ref(0)

// 表箱内部画布当前选中的工具语义：sign / meter / seal / null
const canvasLeftTool = ref(null)
const canvasRightTool = ref(null)
// 8 处表体铅封完成（画布已卸载），等待主场景门把铅封
const boxInnerDone = ref(false)
// 门把铅封提交中 / 已提交
const doorPending = ref(false)
const doorSealed = ref(false)

function onCanvasRightTool(idx, name) {
  operationCount.value++
  if (name === '单相智能电表') canvasRightTool.value = 'meter'
  else if (name === '铅封') canvasRightTool.value = 'seal'
  else {
    canvasRightTool.value = null
    ElMessage.info('该工具将在后续步骤中使用')
  }
}

/** 画布用完一次工具（挂一块表）后取消右栏选中，跟随图随之消失 */
function onCanvasToolUsed() {
  canvasRightTool.value = null
  sceneRef.value?.clearRightTool?.()
}

/** 8 处表体铅封完成：卸载画布回到主场景，等待门把铅封 */
function onBoxInnerDone() {
  boxInnerDone.value = true
  // 退回主场景后清空工具选中：铅封跟随消失，门把铅封需要重新点击铅封图标
  canvasRightTool.value = null
  sceneRef.value?.clearRightTool?.()
  ElMessage.success('表箱内部操作完成，请给表箱门把加铅封')
}

/** 主场景门把命中：必须先在右栏选中「铅封」 */
async function onDoorSealed() {
  if (stage.value !== 7 || !boxInnerDone.value || doorPending.value) return
  if (canvasRightTool.value !== 'seal') {
    ElMessage.warning('请先从右侧选择「铅封」')
    errorCount.value++
    return
  }
  doorPending.value = true
  doorSealed.value = true
  ElMessage.success('加铅封完成')
  await onFinalStepComplete()
}

/** 提交步骤 7 后跳转杆上变占位页 */
async function onFinalStepComplete() {
  submitting.value = true
  try {
    await submitStep(buildPayload(1))
    ElMessage.success('低压实验完成')
    setTimeout(() => router.push('/LPT'), 800)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
    doorPending.value = false
  } finally {
    submitting.value = false
  }
}

let timer = null

/**
 * 步骤映射表：实验启动时写入 localStorage。
 *
 * 用 ref 而不是从 localStorage 现读的 computed —— 恢复流程里可能从后端补拉一份再写回
 * localStorage，computed 感知不到 localStorage 的变化，stage 会一直停在旧值。
 */
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
 * 当前步骤属于第几阶段：3 = 围栏挂牌，4 = 三步验电。
 *
 * 优先读 URL 的 stepOrder（与高压 HCabinetLocalView 一致）：从存档返回时
 * localStorage 的步骤映射可能缺失或尚未补拉，只靠映射会把第 4 步误判成第 3 步。
 */
const stage = computed(() => {
  const fromQuery = Number(route.query.stepOrder)
  if (fromQuery >= 3) return fromQuery
  return steps.value.find(s => s.stepId === stepId.value)?.stepOrder ?? 3
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
    // 步骤映射丢失时（新标签页 / 清过缓存）从后端重建，
    // 否则 stage 会退化成 3，页面按错误的阶段跑。与高压 /HCL 的处理一致。
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
    if (draft && Object.keys(draft).length > 0) {
      // 表体铅封已完成：恢复为门把阶段，不再重建已卸载的内部画布
      if (draft.boxInnerDone) boxInnerDone.value = true
      if (stage.value >= 5 && !boxInnerDone.value) canvasRef.value?.restore(draft)
      else if (stage.value < 5) sceneRef.value?.restore(draft)
    }
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  }
}

/** 当前阶段状态：步骤 5+ 取画布，3/4 取外部场景 */
function currentState() {
  if (stage.value >= 7) {
    // 表体铅封完成后画布已卸载，canvasRef 可能为 null，boxInnerDone 仍需写入存档
    return { ...(canvasRef.value?.getState() || {}), boxInnerDone: boxInnerDone.value }
  }
  if (stage.value >= 5) return canvasRef.value?.getState() || {}
  return sceneRef.value?.getState() || {}
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
    resultData: JSON.stringify({ stage: stage.value, ...currentState() })
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

    // 三步验电完成：先弹提示窗，关窗后再进入表箱内部（步骤 5）
    if (stage.value === 4) {
      electrifyDone.value = true
      showElectrifyNotice.value = true
      ElMessage.success('验电操作完成！')
      return
    }

    const next = steps.value.find(s => s.stepOrder === stage.value + 1)
    if (!next) {
      ElMessage.warning('未找到下一步骤，请确认数据库已执行第 5~7 步脚本')
      return
    }
    // 步骤 3 → 4 仍在本页；步骤 5/6 → 6/7 由画布驱动
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

/** 验电完成提示关窗 → 进入第 5 步（居民表箱内部 Leafer 画布） */
async function onElectrifyNoticeClose() {
  showElectrifyNotice.value = false
  let next = steps.value.find(s => s.stepOrder === 5)
  // 第 5 步是后加的：老实验的 localStorage 映射里没有，需要从后端补拉一次
  if (!next) {
    try {
      const all = await getExperimentSteps(experimentId.value)
      if (all?.length) {
        localStorage.setItem('experimentSteps_' + experimentId.value, JSON.stringify(all))
        steps.value = all
        next = all.find(s => s.stepOrder === 5)
      }
    } catch {
      /* 拉不到按下面的提示处理 */
    }
  }
  if (!next) {
    ElMessage.warning('未找到下一步骤，请确认数据库已执行第 5 步脚本')
    return
  }
  // 与高压一致：关窗后短暂延时再切步骤。
  // 表箱内部已并入 /LRB，这里只换 query（保留当前路径），不再跳 /LBI
  setTimeout(() => {
    router.replace({
      query: { experimentId: experimentId.value, stepId: next.stepId, stepOrder: next.stepOrder }
    })
  }, 500)
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
.resident-box-view {
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

/* 提示弹窗图片尺寸与高压柜体局部页保持一致 */
.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
