<template>
  <div class="experiment-scene">
    <div class="scene-frame" :style="sceneFrameStyle">
      <div class="scroll-wrapper">
        <WorkTicketForm ref="formRef" :finalize="isFinalize" @submit-ticket="handleTicketSubmit" />
      </div>
      <img class="work-ticket-sign" :src="Images.workTicketSign" alt="填写工作票" />
      <img class="work-ticket-commit" :src="Images.workTicketCommit" alt="提交" />
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
})
onUnmounted(() => {
  window.removeEventListener('resize', updateSceneScale)
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
  /* 右上角：挂环中心(原图 x≈52)缩放后约 49px，勾住右边框；顶部挂点对齐顶边框 */
  top: -58px;
  right: -33px;
  z-index: 11;
  /* 高度 550px => 绳尾约在 650px 高度的 2/3 处、不触底 */
  height: 550px;
  width: auto;
  pointer-events: none;
}

/* 核心要求：限制区域大小，其他内容通过滚动显示 */
.scroll-wrapper {
  width: 900px;
  height: 650px;
  overflow-y: auto;
  background-color: rgba(255, 255, 255, 0.9);
  /* 半透明背景增加景深感 */
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  /* 边框：使用 outline 不占据盒模型空间，避免出现滚动条 */
  outline: 10px solid #73bcbb;
  outline-offset: -4px;
}

/* 自定义滚动条，使其风格契合仿真平台 */
.scroll-wrapper::-webkit-scrollbar {
  width: 8px;
}

.scroll-wrapper::-webkit-scrollbar-thumb {
  background: #a0a5aa;
  border-radius: 4px;
}

.scroll-wrapper::-webkit-scrollbar-thumb:hover {
  background: #7a8085;
}

/* 保存进度/查看工作任务按钮样式见 assets/styles/main.css */

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
