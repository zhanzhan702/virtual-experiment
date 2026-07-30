<template>
  <div class="experiment-scene">
    <div class="scroll-wrapper">
      <WorkTicketForm ref="formRef" @submit-ticket="handleTicketSubmit" />
    </div>
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgress" title="保存进度" />

    <!-- 查看工作任务按钮（左下角） -->
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened } from '@element-plus/icons-vue'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import PromptModal from '@/components/PromptModal.vue'
import WorkTicketForm from '@/components/HighVoltage/HWorkTicketForm.vue'
import Images from '@/constants/images'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const showWorkBg = ref(false)

// 从路由 query 获取实验元数据
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
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
    const submitRes = await submitStep(payload)
    ElMessage.success('提交成功！即将进入工器具选择...')
    // 从 sessionStorage 获取下一步 stepId
    const steps = JSON.parse(sessionStorage.getItem('experimentSteps') || '[]')
    const nextStep = steps.find(s => s.stepOrder === 2)
    // 跳转到下一步（工器具选择），传递 experimentId + stepId
    setTimeout(() => {
      router.push({
        path: '/HTS',
        query: {
          experimentId: experimentId.value,
          stepId: nextStep ? nextStep.stepId : ''
        }
      })
    }, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
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
  /* 可以替换成你的高压场景背景图 */
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
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

/* 查看工作任务按钮（左下角） */
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
