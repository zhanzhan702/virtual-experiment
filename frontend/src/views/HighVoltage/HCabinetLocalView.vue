<!-- 柜体局部操作：设围栏 + 挂告示牌(步骤3) + 三步验电(步骤4) -->
<template>
  <div class="cabinet-local-page" @mousemove="onPageMouseMove" @click="onPageClick">
    <!-- 左侧物品栏 -->
    <HLeftToolBar
      :items="leftTools"
      :placed-mask="middleRef?.itemPlaced || []"
      :active-idx="middleRef?.followingToolIdx"
      @select="onLeftToolSelect"
    />

    <!-- 中间交互区域（步骤3/4：围栏/告示牌放置 + 三步验电） -->
    <HMiddleArea
      v-if="!isMeteringStep"
      ref="middleRef"
      :step-order="currentStepOrder"
      @operation="onOperation"
      @error="onError"
      @fences-done="handleFencesDone"
      @voltage-check-done="submitVoltageCheck"
    />

    <!-- 计量小室操作画布（步骤5+：挂电表 / 接线盒） -->
    <HMeteringRoomCanvas
      v-if="isMeteringStep"
      ref="meteringRef"
      :step-order="currentStepOrder"
      :experiment-id="experimentId"
      :step-id="stepId"
      @operation="onOperation"
      @error="onError"
      @step-completed="handleMeteringStepCompleted"
    />

    <!-- 右侧物品栏（终端/工器具/线材，第5个为验电笔） -->
    <HRightToolBar
      :items="rightTools"
      :vt-active="middleRef?.vtActive || false"
      :vt-done="middleRef?.vtDone || false"
      :active-idxs="rightToolActiveIdxs"
      @click="onRightToolClick"
    />

    <!-- 视频占位 -->
    <div v-if="showVideo" class="video-overlay">
      <div class="video-placeholder">
        <span>教学视频（待制作）</span>
        <el-button type="primary" size="large" @click="closeVideo">关闭，进入下一步</el-button>
      </div>
    </div>

    <!-- 按钮 -->
    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="stats.duration_seconds" />
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgress" title="保存进度" />
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <!-- 验电完成提示弹窗（提交后展示，确认后进入下一步） -->
    <PromptModal
      :visible="showElectrifyNotice"
      @close="onElectrifyNoticeClose"
      :button-bottom="'18%'"
    >
      <img :src="Images.electrifyCompleteNotice" alt="验电完成提示" class="work-bg-img" />
    </PromptModal>

    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import PromptModal from '@/components/PromptModal.vue'
import HLeftToolBar from '@/components/HighVoltage/HLeftToolBar.vue'
import HMiddleArea from '@/components/HighVoltage/HMiddleArea.vue'
import HRightToolBar from '@/components/HighVoltage/HRightToolBar.vue'
import HMeteringRoomCanvas from '@/components/HighVoltage/HMeteringRoomCanvas.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import Images from '@/constants/images'

const route = useRoute()
const router = useRouter()
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
const startedAt = ref(formatLocalTime(new Date()))
const showWorkBg = ref(false)
const showVideo = ref(false)
const showElectrifyNotice = ref(false)
const hasSubmitted = ref(false)
const saving = ref(false)

// ─── 步骤模式判定（步骤3=放置物品+验电，步骤4=仅验电） ───
function getStepsFromStore() {
  return JSON.parse(sessionStorage.getItem('experimentSteps') || '[]')
}
const currentStepOrder = computed(() => {
  const fromQuery = Number(route.query.stepOrder)
  if (fromQuery >= 3) return fromQuery
  if (!stepId.value) return 3
  const s = getStepsFromStore().find(s => s.stepId === stepId.value)
  return s ? s.stepOrder : 3
})
const isStep4 = computed(() => currentStepOrder.value === 4)
const isMeteringStep = computed(() => currentStepOrder.value >= 5)
// 计量小室步骤右栏工具高亮（接线状态机激活的工具：剥线钳+当前导线持续高亮）
const rightToolActiveIdxs = computed(() => {
  if (!isMeteringStep.value) return []
  return meteringRef.value?.activeToolIdxs ?? []
})

// ─── 4 物品：[围栏, 高压警示牌, 工作牌, 安全须知] ───
const leftTools = [
  { img: Images.barLeftFence },
  { img: Images.barSignStopHighVoltage },
  { img: Images.barSignPersonWorking },
  { img: Images.barSafetyNotice }
]

// ─── 右侧工具栏（顺序与工器具选择页一致，验电笔在 index 4） ───
const rightTools = [
  { name: '智能电表', img: Images.barThreePhaseThreeWireMeter },
  { name: '三相三线专变终端', img: Images.barThreePhaseThreeWireTerminal },
  { name: '十字螺丝刀', img: Images.barCrossScrewdriver },
  { name: '剥线钳', img: Images.barWireStripper },
  { name: '验电笔', img: Images.barVoltageTesterNormal },
  { name: '铅封', img: Images.barSeal },
  // ── 2.5MM² 导线（按工器具选择页顺序） ──
  { name: '2.5MM²黄色导线', img: Images.barWire25mm2Yellow },
  { name: '2.5MM²绿色导线', img: Images.barWire25mm2Green },
  { name: '2.5MM²红色导线', img: Images.barWire25mm2Red },
  // ── 4.0MM² 导线 ──
  { name: '4.0MM²黄色导线', img: Images.barWire4mm2Yellow },
  { name: '4.0MM²黄黑色导线', img: Images.barWire4mm2YellowBlack },
  { name: '4.0MM²红色导线', img: Images.barWire4mm2Red },
  { name: '4.0MM²红黑色导线', img: Images.barWire4mm2RedBlack },
  { name: '扎带标识牌', img: Images.barCableTieLabel },
  { name: '2芯遥控线', img: Images.barRemoteControlCable2Core },
  { name: '2芯遥信线', img: Images.barRemoteSignalCable2Core },
  { name: '6芯信号线', img: Images.barSignalCable6Core },
  { name: '8芯信号线', img: Images.barSignalCable8Core },
  { name: '通信模块', img: Images.barCommunicationModule },
  { name: 'SIM卡', img: Images.barSimCard },
  { name: '天线', img: Images.barAntenna }
]

// ─── 操作统计 ───
const stats = reactive({ duration_seconds: 0, operation_count: 0, error_count: 0 })
let timer = null
onMounted(() => {
  timer = setInterval(() => {
    stats.duration_seconds++
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// ============== 方法 ==============

// 中间栏子组件引用（步骤3/4 交互逻辑在 HMiddleArea 内）
const middleRef = ref(null)
// 计量小室画布组件引用（步骤5+ 交互逻辑在 HMeteringRoomCanvas 内）
const meteringRef = ref(null)

function onOperation() {
  stats.operation_count++
}
function onError() {
  stats.error_count++
}

/** 左侧工具栏选择 → 转发给中间栏 */
function onLeftToolSelect(idx, e) {
  middleRef.value?.selectTool?.(idx, e)
}

// 右侧工具栏点击：计量小室步骤转发画布组件，步骤3/4 验电笔走专用逻辑
function onRightToolClick(idx, e) {
  if (isMeteringStep.value) {
    meteringRef.value?.onRightToolClick?.(idx, e)
    return
  }
  if (idx === 4) {
    middleRef.value?.toggleVoltageTester?.(e)
    return
  }
  stats.operation_count++
  ElMessage.info('「' + rightTools[idx].name + '」将在计量小室操作中使用')
}

function onPageMouseMove(e) {
  middleRef.value?.onPageMouseMove?.(e)
  meteringRef.value?.onPageMouseMove?.(e)
}

/** 4 物品全部放置 → 提交步骤3并显示视频 */
async function handleFencesDone() {
  if (hasSubmitted.value) return
  hasSubmitted.value = true
  try {
    await submitStep({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score: Math.max(0, 100 - stats.error_count * 10),
      resultData: JSON.stringify({ itemPlaced: [...(middleRef.value?.itemPlaced || [])] }),
      startedAt: startedAt.value
    })
    ElMessage.success('围栏与标示牌放置完成')
    showVideo.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
    hasSubmitted.value = false
  }
}

function closeVideo() {
  showVideo.value = false
  const next = getStepsFromStore().find(s => s.stepOrder === 4)
  if (next) {
    sessionStorage.setItem('_hcl_step4_skip_placement', 'true')
    router.push({
      path: '/HCL',
      query: { experimentId: experimentId.value, stepId: next.stepId, stepOrder: 4 }
    })
  } else {
    sessionStorage.setItem('_hcl_step4_skip_placement', 'true')
    router.push({
      path: '/HCL',
      query: { experimentId: experimentId.value, stepId: stepId.value, stepOrder: 4 }
    })
  }
}

// ─── 验电笔交互（转发给中间栏） ───
function onPageClick(e) {
  middleRef.value?.onPageClick?.(e)
}

/** 验电完成 → 提交当前步骤并跳转下一步 */
async function submitVoltageCheck() {
  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: stats.duration_seconds,
    operationCount: stats.operation_count,
    errorCount: stats.error_count,
    score: 100.0 - stats.error_count * 10 > 0 ? 100.0 - stats.error_count * 10 : 0, //最低得分为0分
    resultData: JSON.stringify({ vtDone: true, vtStep: 3 }),
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('验电操作完成！')
    showElectrifyNotice.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}

// 验电完成弹窗确认 → 进入计量小室挂电表（步骤5）
function onElectrifyNoticeClose() {
  showElectrifyNotice.value = false
  setTimeout(() => {
    const next = getStepsFromStore().find(s => s.stepOrder === 5)
    router.replace({
      path: '/HCL',
      query: {
        experimentId: experimentId.value,
        stepId: next?.stepId || stepId.value,
        stepOrder: 5
      }
    })
  }, 500)
}

/** 计量小室子步骤完成（挂电表/接线盒） → 提交当前步骤并跳下一步 */
async function handleMeteringStepCompleted(stepOrder) {
  if (hasSubmitted.value) return
  hasSubmitted.value = true
  try {
    const resultData = meteringRef.value?.getDraftState?.() || {}
    await submitStep({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score: Math.max(0, 100 - stats.error_count * 10),
      resultData: JSON.stringify(resultData),
      startedAt: startedAt.value
    })
    if (stepOrder === 5) {
      ElMessage.success('挂表成功')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 6)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 6
        }
      })
    } else if (stepOrder === 6) {
      ElMessage.success('接线盒处理完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 7)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 7
        }
      })
    } else {
      ElMessage.success('接线完成')
      hasSubmitted.value = false
      setTimeout(() => {
        router.replace({
          path: '/experiment',
          query: { experimentId: experimentId.value }
        })
      }, 800)
    }
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
    hasSubmitted.value = false
  }
}

// ─── 存档 ───
const saveProgress = async () => {
  saving.value = true
  try {
    const m = middleRef.value
    const metering = meteringRef.value
    const base = isMeteringStep.value
      ? { ...(metering?.getDraftState?.() || {}) }
      : {
          itemPlaced: [...(m?.itemPlaced || [])],
          vtDone: m?.vtDone || false,
          vtStep: m?.vtStep ?? 0
        }
    await saveDraft({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 0,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      resultData: JSON.stringify(base),
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

// ─── 恢复草稿（状态按步骤分发给对应子组件恢复） ───
onMounted(async () => {
  if (experimentId.value && stepId.value) {
    try {
      const d = await getStepDraft(experimentId.value, stepId.value)
      if (d) {
        if (isMeteringStep.value) {
          meteringRef.value?.restoreDraft?.(d)
        } else {
          middleRef.value?.restoreDraft?.(d)
        }
      }
    } catch (_) {}
  }
  // 步骤4无草稿时确保物品显示为已放置
  if (isStep4.value && !middleRef.value?.itemPlaced?.some(v => v)) {
    middleRef.value?.markPlacedForStep4?.()
  }
})
</script>

<style scoped>
.cabinet-local-page {
  width: 100vw;
  height: 100vh;
  background: #fff;
  position: relative;
  overflow: hidden;
}

/* 视频占位 */
.video-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-placeholder {
  color: #fff;
  font-size: 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
}

/* 固定按钮 */
.save-bar-fixed {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
  cursor: pointer;
  background-image: var(--img-save-icon);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
}

.save-bar-fixed:hover {
  background-image: var(--img-save-icon-hover);
  transform: scale(1.05);
}

.save-bar-fixed.saving {
  opacity: 0.6;
  pointer-events: none;
}

.save-bar-fixed.disabled {
  opacity: 0.4;
  pointer-events: none;
}

.work-task-btn {
  position: fixed;
  bottom: 1.5rem;
  left: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
  cursor: pointer;
  background-image: var(--img-work-task);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
}

.work-task-btn:hover {
  background-image: var(--img-work-task-hover);
  transform: scale(1.05);
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
