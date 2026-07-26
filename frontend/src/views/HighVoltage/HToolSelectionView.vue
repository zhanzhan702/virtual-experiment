<template>
  <div class="tool-selection-view">
    <WizardInventorySelection ref="wizardRef" :categories="categories" @finish="handleToolSelectionSubmit"
      @operation="handleOperation" @submit-error="handleSubmitError" />

    <div class="save-bar-fixed">
      <el-button type="info" size="default" @click="saveProgress" :loading="saving">
        <el-icon>
          <FolderOpened />
        </el-icon> 保存进度
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Suitcase, FolderOpened } from '@element-plus/icons-vue'
import WizardInventorySelection from '@/components/HighVoltage/HWizardInventorySelection.vue'
import { categories } from '@/constants/tool-selection-config'
import { cabinetVisitedKey } from '@/constants/scene-hotspot-config'
import { saveSelectedTools } from '@/utils/selectedTools'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()

const wizardRef = ref(null)

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
  } catch (_) { /* ignore */ }
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
onMounted(() => { timer = setInterval(() => { stats.duration_seconds++ }, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })

// 统计操作次数（每次点击/选择工器具）
const handleOperation = () => { stats.operation_count++ }

// 统计提交错误次数
const handleSubmitError = (errorPageCount) => { stats.error_count += errorPageCount }

const handleToolSelectionSubmit = async (selectedMap) => {
  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: stats.duration_seconds,
    operationCount: stats.operation_count,
    errorCount: stats.error_count,
    score: 100.00 - (stats.error_count * 10) > 0 ? 100.00 - (stats.error_count * 10) : 0,//最低得分为0分
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('工器具选择已完成，即将进入配电室...')
    // 保存已选工具到 sessionStorage，供后续柜内操作右侧面板展示
    saveSelectedTools(selectedMap)
    // 进入 A1 配电室总览（前端子流程，不单独占用库表步骤）
    sessionStorage.setItem(cabinetVisitedKey, JSON.stringify({ incoming: false, metering: false }))
    setTimeout(() => {
      router.push({
        path: '/HScene',
        query: { experimentId: experimentId.value }
      })
    }, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
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
  background-image: url('@/assets/images/selection.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
}

.tool-selection-view>* {
  position: relative;
  z-index: 1;
}



.save-bar-fixed {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}
</style>
