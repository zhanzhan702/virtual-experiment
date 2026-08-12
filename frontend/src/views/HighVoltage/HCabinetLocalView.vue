<!-- 柜体局部操作：设围栏 + 挂告示牌(步骤3) + 三步验电(步骤4) -->
<template>
  <div class="cabinet-local-page" @mousemove="onPageMouseMove" @mousedown="onPageMouseDown" @mouseup="onPageMouseUp"
    @click="onPageClick">
    <!-- 左侧物品栏 -->
    <HLeftToolBar :items="leftTools" :placed-mask="middleRef?.itemPlaced || []"
      :active-idx="middleRef?.followingToolIdx" @select="onLeftToolSelect" />

    <!-- 中间交互区域（步骤3/4/12 围栏/告示牌+三步验电；步骤21 上电后回柜体局部+合闸热区） -->
    <HMiddleArea v-if="!isMeteringStep && (!isTerminalStep || showCabinetAfterPowerOn)" ref="middleRef"
      :step-order="currentStepOrder" @operation="onOperation" @error="onError" @fences-done="handleFencesDone"
      @voltage-check-done="submitVoltageCheck" />

    <!-- 计量小室操作画布（步骤5+：挂电表 / 接线盒） -->
    <HMeteringRoomCanvas v-if="isMeteringStep" ref="meteringRef" :step-order="currentStepOrder"
      :experiment-id="experimentId" :step-id="stepId" @operation="onOperation" @error="onError"
      @step-completed="handleMeteringStepCompleted" @confirm="onConfirmClick" />

    <!-- 终端小室操作画布（步骤13+，流程与计量小室一致；步骤21 确认后销毁回柜体局部） -->
    <HTerminalRoomCanvas v-if="isTerminalStep && !showCabinetAfterPowerOn" ref="terminalRef"
      :step-order="currentStepOrder" :experiment-id="experimentId" :step-id="stepId" @operation="onOperation"
      @error="onError" @step-completed="handleTerminalStepCompleted" @confirm="onConfirmClick" />

    <!-- 右侧物品栏（终端/工器具/线材，第5个为验电笔） -->
    <HRightToolBar :items="rightTools" :vt-active="middleRef?.vtActive || false" :vt-done="middleRef?.vtDone || false"
      :active-idxs="rightToolActiveIdxs" @click="onRightToolClick" />

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

    <!-- 计量小室验电完成提示弹窗（提交后展示，确认后进入下一步） -->
    <PromptModal :visible="showElectrifyNotice" @close="onElectrifyNoticeClose" :button-bottom="'18%'">
      <img :src="Images.electrifyCompleteNotice" alt="验电完成提示" class="work-bg-img" />
    </PromptModal>

    <!-- 计量小室操作完成弹窗（步骤11 确认键触发，确认后进入终端小室验电） -->
    <PromptModal :visible="showMeterRoomSuccess" @close="onMeterRoomSuccessClose" :button-bottom="'22%'">
      <img :src="Images.meterRoomOperationSuccess" alt="计量小室操作完成" class="work-bg-img" />
    </PromptModal>

    <!-- 终端小室验电完成提示弹窗（提交后展示，确认后进入下一步） -->
    <PromptModal :visible="showTerminalElectrifyNotice" @close="onTerminalElectrifyNoticeClose"
      :button-bottom="'18%'">
      <img :src="Images.terminalElectrifyCompleteNotice" alt="终端小室验电完成提示" class="work-bg-img" />
    </PromptModal>

    <!-- 终端小室上电完成弹窗（步骤21 确认键触发 → 终端小室完成 → 送电完成提示 → 回柜体局部） -->
    <PromptModal :visible="showTerminalComplete" @close="onTerminalCompleteClose" :button-bottom="'22%'">
      <img :src="Images.terminalRoomCompleteNotice" alt="终端小室操作完成" class="work-bg-img" />
    </PromptModal>

    <PromptModal :visible="showReadyForPowerOn" @close="onReadyForPowerOnClose" :button-bottom="'24%'">
      <img :src="Images.readyForPowerOnNotice" alt="送电完成提示" class="work-bg-img" />
    </PromptModal>

    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitStep, saveDraft, getStepDraft, getExperimentSteps } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import PromptModal from '@/components/PromptModal.vue'
import HLeftToolBar from '@/components/HighVoltage/HLeftToolBar.vue'
import HMiddleArea from '@/components/HighVoltage/HMiddleArea.vue'
import HRightToolBar from '@/components/HighVoltage/HRightToolBar.vue'
import HMeteringRoomCanvas from '@/components/HighVoltage/HMeteringRoomCanvas.vue'
import HTerminalRoomCanvas from '@/components/HighVoltage/HTerminalRoomCanvas.vue'
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
const showTerminalElectrifyNotice = ref(false)
const showMeterRoomSuccess = ref(false)
const showTerminalComplete = ref(false)
const showReadyForPowerOn = ref(false)
// 步骤21 上电完成后回柜体局部页面（HMiddleArea + 合闸热区）
const showCabinetAfterPowerOn = ref(false)
const hasSubmitted = ref(false)
const saving = ref(false)

// ─── 步骤模式判定（步骤3=放置物品+验电，步骤4=仅验电） ───
function getStepsFromStore() {
  return JSON.parse(localStorage.getItem('experimentSteps_' + experimentId.value) || '[]')
}
const currentStepOrder = computed(() => {
  const fromQuery = Number(route.query.stepOrder)
  if (fromQuery >= 3) return fromQuery
  if (!stepId.value) return 3
  const s = getStepsFromStore().find(s => s.stepId === stepId.value)
  return s ? s.stepOrder : 3
})
const isStep4 = computed(() => currentStepOrder.value === 4 || currentStepOrder.value === 12)
const isMeteringStep = computed(() => currentStepOrder.value >= 5 && currentStepOrder.value <= 11)
const isTerminalStep = computed(() => currentStepOrder.value >= 13)

// stepId 跟随路由 query 同步：router.replace 跳步后原 stepId ref 不会自动更新，
// 会导致后续 submitStep/saveDraft/getStepDraft 一直写到旧步骤、刷新才正确
// （典型“必须刷新才能进入下一步”的根因）
watch(
  () => route.query.stepId,
  id => {
    if (id) stepId.value = id
  }
)
// 计量/终端小室步骤右栏工具高亮（接线状态机激活的工具：剥线钳+当前导线持续高亮）
const rightToolActiveIdxs = computed(() => {
  if (isMeteringStep.value) return meteringRef.value?.activeToolIdxs ?? []
  if (isTerminalStep.value) return terminalRef.value?.activeToolIdxs ?? []
  return []
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
  // ── 2.5MM² 导线（通用线图，按工器具选择页顺序） ──
  { name: '2.5MM²黄色导线', img: Images.barWireYellow },
  { name: '2.5MM²绿色导线', img: Images.barWireGreen },
  { name: '2.5MM²红色导线', img: Images.barWireRed },
  // ── 4.0MM² 导线（黄/红共用 2.5MM² 通用图，显示放大模拟） ──
  { name: '4.0MM²黄色导线', img: Images.barWireYellow, imgWidth: '100%' },
  { name: '4.0MM²黄黑色导线', img: Images.barWire4mm2YellowBlack },
  { name: '4.0MM²红色导线', img: Images.barWireRed, imgWidth: '100%' },
  { name: '4.0MM²红黑色导线', img: Images.barWire4mm2RedBlack },
  { name: '扎带标识牌', img: Images.barCableTieLabel },
  { name: '2芯遥控线', img: Images.barWire2Core },
  { name: '2芯遥信线', img: Images.barWire2Core },
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

// 中间栏子组件引用（步骤3/4/12 交互逻辑在 HMiddleArea 内）
const middleRef = ref(null)
// 计量小室画布组件引用（步骤5+ 交互逻辑在 HMeteringRoomCanvas 内）
const meteringRef = ref(null)
// 终端小室画布组件引用（步骤13+ 交互逻辑在 HTerminalRoomCanvas 内）
const terminalRef = ref(null)

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

// 右侧工具栏点击：计量/终端小室步骤转发画布组件，步骤3/4/12 验电笔走专用逻辑
function onRightToolClick(idx, e) {
  if (isMeteringStep.value) {
    meteringRef.value?.onRightToolClick?.(idx, e)
    return
  }
  if (isTerminalStep.value) {
    terminalRef.value?.onRightToolClick?.(idx, e)
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
  terminalRef.value?.onPageMouseMove?.(e)
}

function onPageMouseDown(e) {
  middleRef.value?.onPageMouseDown?.(e)
  terminalRef.value?.onPageMouseDown?.(e)
}

function onPageMouseUp(e) {
  middleRef.value?.onPageMouseUp?.(e)
  terminalRef.value?.onPageMouseUp?.(e)
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
      // 标准流程：提交时用空内容覆盖存档内容（恢复时物品按标准强制显示）
      resultData: '{}',
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
  terminalRef.value?.onPageClick?.(e)
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
    // 标准流程：提交时用空内容覆盖存档内容（验电结果按标准恢复）
    resultData: '{}',
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('验电操作完成！')
    // 步骤4（计量）与步骤12（终端）验电完成提示图不同
    if (currentStepOrder.value === 12) {
      showTerminalElectrifyNotice.value = true
    } else {
      showElectrifyNotice.value = true
    }
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}

/** 画布确认键点击（步骤11 铅封完成后）→ 显示计量小室完成弹窗 */
function onConfirmClick() {
  // 终端小室步骤21 确认键 → 销毁画布回柜体局部 → 双确认弹窗
  if (isTerminalStep.value && currentStepOrder.value === 21) {
    showCabinetAfterPowerOn.value = true
    showTerminalComplete.value = true
    return
  }
  showMeterRoomSuccess.value = true
}

/** 终端小室完成弹窗确认 → 送电完成提示弹窗 */
function onTerminalCompleteClose() {
  showTerminalComplete.value = false
  showReadyForPowerOn.value = true
}

/** 送电完成提示关闭（柜体局部已在显示，合闸热区可见） */
function onReadyForPowerOnClose() {
  showReadyForPowerOn.value = false
}

/** 计量小室完成弹窗确认 → 进入终端小室三步验电（步骤12） */
function onMeterRoomSuccessClose() {
  showMeterRoomSuccess.value = false
  setTimeout(() => {
    const next = getStepsFromStore().find(s => s.stepOrder === 12)
    router.replace({
      path: '/HCL',
      query: {
        experimentId: experimentId.value,
        stepId: next?.stepId || stepId.value,
        stepOrder: 12
      }
    })
  }, 500)
}

// 计量验电完成弹窗确认（步骤4）→ 进入挂表（5）
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

// 终端验电完成弹窗确认（步骤12）→ 进入终端小室挂表（13）
function onTerminalElectrifyNoticeClose() {
  showTerminalElectrifyNotice.value = false
  setTimeout(() => {
    const next = getStepsFromStore().find(s => s.stepOrder === 13)
    router.replace({
      path: '/HCL',
      query: {
        experimentId: experimentId.value,
        stepId: next?.stepId || stepId.value,
        stepOrder: 13
      }
    })
  }, 500)
}

/** 计量小室子步骤完成（挂电表/接线盒） → 提交当前步骤并跳下一步 */
async function handleMeteringStepCompleted(stepOrder) {
  if (hasSubmitted.value) return
  hasSubmitted.value = true
  try {
    // 画布步骤为标准流程：提交时用空内容覆盖存档内容（防数据库冗余），恢复靠标准推断+草稿
    await submitStep({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score: Math.max(0, 100 - stats.error_count * 10),
      resultData: '{}',
      startedAt: startedAt.value
    })
    // 步骤提交完成：清空本地兜底（防止回退/重进时残留旧状态误判完成态）
    localStorage.removeItem('meteringRoom_' + experimentId.value)
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
    } else if (stepOrder === 7) {
      ElMessage.success('接线完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 8)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 8
        }
      })
    } else if (stepOrder === 8) {
      ElMessage.success('6芯信号线连接完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 9)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 9
        }
      })
    } else if (stepOrder === 9) {
      ElMessage.success('扎带标识牌放置完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 10)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 10
        }
      })
    } else if (stepOrder === 10) {
      ElMessage.success('接线盒处理完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === 11)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: 11
        }
      })
    } else if (stepOrder === 11) {
      // 铅封完成：提交但不跳转，等待用户点击确认键（画布内）后弹窗进入步骤12
      ElMessage.success('铅封完成')
      hasSubmitted.value = false
    } else {
      ElMessage.success('步骤完成')
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

/** 终端小室子步骤完成（挂表/接线盒处理等） → 提交当前步骤并跳下一步 */
async function handleTerminalStepCompleted(stepOrder) {
  if (hasSubmitted.value) return
  hasSubmitted.value = true
  try {
    // 画布步骤为标准流程：提交时用空内容覆盖存档内容（防数据库冗余），恢复靠标准推断+草稿
    await submitStep({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score: Math.max(0, 100 - stats.error_count * 10),
      resultData: '{}',
      startedAt: startedAt.value
    })
    // 步骤提交完成：清空本地兜底（防止回退/重进时残留旧状态误判完成态）
    localStorage.removeItem('terminalRoom_' + experimentId.value)
    const nextMap = { 13: 14, 14: 15, 15: 16, 16: 17, 17: 18, 18: 19, 19: 20, 20: 21 }
    const msgMap = {
      13: '挂表成功',
      14: '接线盒处理完成',
      15: '接线完成',
      16: '遥控压板处理完成',
      17: '信号线连接完成',
      18: '通信模块安装完成',
      19: '绑扎带标识牌放置完成',
      20: '接线盒处理完成'
    }
    const nextOrder = nextMap[stepOrder]
    if (nextOrder) {
      ElMessage.success(msgMap[stepOrder] || '步骤完成')
      hasSubmitted.value = false
      const next = getStepsFromStore().find(s => s.stepOrder === nextOrder)
      router.replace({
        path: '/HCL',
        query: {
          experimentId: experimentId.value,
          stepId: next?.stepId || stepId.value,
          stepOrder: nextOrder
        }
      })
    } else {
      // 步骤21 上电：提交后等待用户点击确认键（画布内）弹窗进入合闸
      ElMessage.success('上电完成')
      hasSubmitted.value = false
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
    // 存档时该步骤全量传入后端（getFullState），提交时清空（见 handleMetering/TerminalStepCompleted）
    const base = isMeteringStep.value
      ? { ...(metering?.getFullState?.() || {}) }
      : isTerminalStep.value
        ? { ...(terminalRef.value?.getFullState?.() || {}) }
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
    // 步骤映射丢失时（新标签页/清缓存）从后端恢复，避免跳转时 stepId 错位
    if (!getStepsFromStore().length) {
      try {
        const steps = await getExperimentSteps(experimentId.value)
        if (steps?.length) {
          localStorage.setItem('experimentSteps_' + experimentId.value, JSON.stringify(steps))
        }
      } catch (_) { }
    }
    try {
      // 无论后端是否有草稿都执行恢复（restoreDraft 内部合并标准推断 + 草稿 + localStorage 兜底）
      const d = (await getStepDraft(experimentId.value, stepId.value)) || {}
      if (isMeteringStep.value) {
        meteringRef.value?.restoreDraft?.(d)
      } else if (isTerminalStep.value) {
        terminalRef.value?.restoreDraft?.(d)
      } else {
        middleRef.value?.restoreDraft?.(d)
      }
    } catch (_) { }
  }
  // 步骤4/12（验电）：物品必为已放置（步骤3 完成条件=4 个全放），无条件标记，防止草稿残留旧值干扰
  if (isStep4.value) {
    middleRef.value?.markPlacedForStep4?.()
  }
})

// 同组件导航（画布步骤5-11 ↔ 中间栏步骤12 切换）时组件不重新挂载，onMounted 不会再次执行，
// 需监听步骤变化确保进入步骤4/12 时物品显示为已放置状态
// 同时重置 hasSubmitted，防止上一步的提交锁阻塞当前步骤（如步骤3 的锁残留到步骤5）
watch(currentStepOrder, order => {
  hasSubmitted.value = false
  if (order === 4 || order === 12) {
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

/* 保存进度/查看工作任务按钮样式见 assets/styles/main.css */

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
