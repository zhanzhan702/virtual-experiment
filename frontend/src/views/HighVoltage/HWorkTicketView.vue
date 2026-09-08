<template>
  <div class="experiment-scene">
    <div class="scene-frame" :style="sceneFrameStyle">
      <div ref="scrollEl" class="scroll-wrapper">
        <WorkTicketForm ref="formRef" :finalize="isFinalize" @submit-ticket="handleTicketSubmit" />
      </div>
      <img class="work-ticket-sign" :src="Images.workTicketSign" alt="填写工作票" />
      <div class="work-ticket-commit">
        <img :src="Images.workTicketCommit" alt="提交" />
        <button
          class="commit-hit-area"
          type="button"
          aria-label="提交"
          title="提交"
          @click.stop="handleCommitClick"
        />
        <img
          ref="focusEl"
          class="scroll-focus"
          :class="{ 'is-dragging': focusDragging, 'is-ready': focusReady }"
          :style="{ top: focusTop + 'px' }"
          :src="Images.scrollFocus"
          alt=""
          draggable="false"
          @mousedown="onFocusMouseDown"
          @click.stop
        />
      </div>
    </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
const scrollEl = ref(null)
const focusEl = ref(null)
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

// ===== 绳子滚动条：焦点沿绳子移动，与滚动位置按比例联动 =====
// 几何（基于 WorkTicketCommit 130x586，图片显示高 550）
const FOCUS_KNOT_Y = 120    // 容器内绳顶/交点 y（scrollTop=0 时焦点位置）
// 焦点最底端不贴图片底：按行程向上收 11%（行程=549-120≈429，新底端≈502）
const FOCUS_TAIL_Y = 502
const focusTop = ref(FOCUS_KNOT_Y)
const focusDragging = ref(false)
const focusReady = ref(false)
const RANGE = FOCUS_TAIL_Y - FOCUS_KNOT_Y

// 焦点容器元素（绝对定位基准是 .work-ticket-commit）
function setFocusTop(y) {
  focusTop.value = Math.min(FOCUS_TAIL_Y, Math.max(FOCUS_KNOT_Y, y))
}

// scrollTop -> 焦点 top
function syncFocusFromScroll() {
  if (focusDragging.value || !scrollEl.value) return
  const el = scrollEl.value
  const max = el.scrollHeight - el.clientHeight
  const ratio = max > 0 ? el.scrollTop / max : 0
  setFocusTop(FOCUS_KNOT_Y + ratio * RANGE)
}

// 焦点 top -> scrollTop
function syncScrollFromFocus(top) {
  if (!scrollEl.value) return
  const el = scrollEl.value
  const max = el.scrollHeight - el.clientHeight
  const ratio = (top - FOCUS_KNOT_Y) / RANGE
  el.scrollTop = ratio * max
}

// 拖动焦点：仅当鼠标按下在焦点图上才触发
function onFocusMouseDown(e) {
  e.preventDefault()
  focusDragging.value = true
  const startY = e.clientY
  const startTop = focusTop.value
  const onMove = ev => {
    const next = startTop + (ev.clientY - startY)
    setFocusTop(next)
    syncScrollFromFocus(next)
  }
  const onUp = () => {
    focusDragging.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// 随视口缩放整个工作票外框，让标题横幅与边框等比例缩放、不扭曲
const sceneScale = ref(1)
const sceneFrameStyle = computed(() => ({ transform: `scale(${sceneScale.value})` }))
function updateSceneScale() {
  const sx = window.innerWidth / 1000
  const sy = window.innerHeight / 770
  sceneScale.value = Math.min(1, sx, sy)
}
onMounted(() => {
  updateSceneScale()
  window.addEventListener('resize', updateSceneScale)
  // 绑定滚动同步：滚轮/触控板滚动时焦点跟随
  scrollEl.value?.addEventListener('scroll', syncFocusFromScroll, { passive: true })
  // 初始对齐焦点与滚动条，完成后显示焦点
  requestAnimationFrame(() => {
    syncFocusFromScroll()
    focusReady.value = true
  })
})
onUnmounted(() => {
  window.removeEventListener('resize', updateSceneScale)
  scrollEl.value?.removeEventListener('scroll', syncFocusFromScroll)
})

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

// 点击顶部“提交”标牌上的标签 → 触发表单校验并提交
function handleCommitClick() {
  formRef.value?.validateAndSubmit?.()
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
/* 整个实验场景外层，通常铺满屏幕 */
.experiment-scene {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: var(--img-hwt-bg);
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

/* 外层相对容器：用于把标题横幅定位到青色边框上方 */
.scene-frame {
  position: relative;
}

/* 工作票标题横幅：压在 scroll-wrapper 青色 outline 边框的左上角（源图 701×224） */
.work-ticket-sign {
  position: absolute;
  top: -80px;
  left: 22px;
  z-index: 10;
  width: clamp(200px, 28%, 320px);
}

/* 提交标牌：金属挂环卡在边框右上角，绳子垂坠到约 2/3 高度、不触底 */
.work-ticket-commit {
  position: absolute;
  top: -53px;
  right: -28px;
  z-index: 11;
  /* 高度 550px => 绳尾约在 650px 高度的 2/3 处、不触底 */
  height: 550px;
  width: auto;
}

.work-ticket-commit img:not(.scroll-focus) {
  width: auto;
  height: 100%;
  display: block;
  /* 图片本身不拦截点击，点击交给热区 */
  pointer-events: none;
  user-select: none;
}

/* 绳上焦点（jiaodian）：绝对定位，沿绳子移动，可拖动 */
.scroll-focus {
  position: absolute;
  /* 绳子中心显示 x≈49px，焦点宽 34px => 左移半宽 17px 对齐中心 */
  left: 32px;
  top: 120px;
  width: 34px;
  height: auto;
  z-index: 12;
  cursor: grab;
  user-select: none;
  -webkit-user-drag: none;
  /* 初始隐藏到拿到滚动状态后再定位，避免闪跳 */
  visibility: hidden;
}

.scroll-focus.is-dragging {
  cursor: grabbing;
}

.scroll-focus.is-ready {
  visibility: visible;
}

/* 点击热区：仅黄色“提交”牌子本体，不含金属柄/挂钩与绳子 */
.commit-hit-area {
  position: absolute;
  /* 相对图片左上：黄色牌子约在 x15–120 / y13–96（550 高展示坐标） */
  left: 15px;
  top: 13px;
  width: 105px;
  height: 83px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

/* 核心要求：限制区域大小，其他内容通过滚动显示 */
.scroll-wrapper {
  width: 900px;
  height: 650px;
  overflow-y: auto;
  /* 内部背景统一为工作票纸的米白 #fffef8，撑满整个表单 */
  background-color: #fffef8;
  /* 加粗 + 圆角；box-sizing: border-box 让边框占据盒内空间，整体大小不变 */
  border: 14px solid #73bcbb;
  border-radius: 16px;
  box-sizing: border-box;
  /* 隐藏原生竖向滚动条，保留滚动能力 */
  scrollbar-width: none;
}

.scroll-wrapper::-webkit-scrollbar {
  display: none;
}

/* 保存进度/查看工作任务按钮样式见 assets/styles/main.css */

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
