<template>
  <main class="experiment-scene">
    <LWorkTicketForm
      ref="formRef"
      :player-name="authStore.user?.name || ''"
      :disabled="loading || restoreFailed || busy || completed || !hasMetadata"
      :paused="loading || restoreFailed || completed || !hasMetadata"
      :busy="busy"
      :completed="completed"
      @submit-ticket="handleTicketSubmit"
    />
    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="sessionSeconds" />
    <div v-if="loading || restoreFailed || !hasMetadata" class="load-panel" role="status">
      <template v-if="!hasMetadata">
        <p>缺少实验信息，请从实验选择页进入低压场景。</p>
        <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
      </template>
      <p v-else-if="loading">正在恢复工作票…</p>
      <template v-else>
        <p>工作票恢复失败，重试成功后可继续填写，已有存档不会被覆盖。</p>
        <el-button type="primary" @click="handleRestore">重新加载</el-button>
      </template>
    </div>
    <div v-if="completed" class="completion-banner" role="status">✓ 两张工作票已完成</div>
    <div
      class="save-bar-fixed"
      :class="{ saving, disabled: !canWrite }"
      role="button"
      :tabindex="canWrite ? 0 : -1"
      :aria-disabled="!canWrite"
      aria-label="保存进度"
      @click="handleSave"
      @keydown.enter.prevent="handleSave"
      @keydown.space.prevent="handleSave"
    />
    <div
      class="work-task-btn"
      role="button"
      tabindex="0"
      aria-label="查看工作任务"
      @click="showTask = true"
      @keydown.enter.prevent="showTask = true"
      @keydown.space.prevent="showTask = true"
    />
    <PromptModal
      :visible="showTask"
      :button-bottom="'6%'"
      :btn-width="'24%'"
      @close="showTask = false"
    >
      <article class="task-notice">
        <h2>低压场景工作任务</h2>
        <p>请分别完成集中器、智能电表两张配电第二种工作票。</p>
        <p>集中器：福州台江江滨3#变压器低压计量表箱，进行集中器安装与调试。</p>
        <p>智能电表：福州江滨小区5号楼一层居民表箱110012000011，进行单相电能表安装。</p>
        <p>
          填写红色下划线部分，确认工作班人员、安全措施和危险点防范措施，两张票均正确后统一提交。
        </p>
      </article>
    </PromptModal>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LWorkTicketForm from '@/components/LowVoltage/LWorkTicketForm.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import PromptModal from '@/components/PromptModal.vue'
import { getStepDraft, saveDraft, submitStep } from '@/api/experiment'
import { useAuthStore } from '@/stores/auth'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const experimentId = typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
const stepId = typeof route.query.stepId === 'string' ? route.query.stepId : ''
const hasMetadata = !!experimentId && !!stepId
const formRef = ref(null)
const loading = ref(true)
const restoreFailed = ref(false)
const saving = ref(false)
const submitting = ref(false)
const completed = ref(false)
const showTask = ref(false)
const startedAt = ref(formatLocalTime(new Date()))
const busy = computed(() => saving.value || submitting.value)
const canWrite = computed(
  () => hasMetadata && !loading.value && !restoreFailed.value && !busy.value && !completed.value
)
const sessionSeconds = computed(() => formRef.value?.getSessionSeconds() || 0)
// 后端累加统计；基线仅在接口成功后推进，界面计时保持连续。
let persistedStats = { duration_seconds: 0, operation_count: 0, error_count: 0 }

async function handleRestore() {
  if (!hasMetadata) {
    loading.value = false
    return
  }
  loading.value = true
  restoreFailed.value = false
  try {
    const snapshot = await getStepDraft(experimentId, stepId)
    if (snapshot && Object.keys(snapshot).length) {
      if (snapshot.version !== 1 || !snapshot.tickets?.concentrator || !snapshot.tickets?.meter) {
        throw new Error('存档格式无法识别')
      }
      await formRef.value?.restore(snapshot)
      persistedStats = { ...formRef.value.getSnapshot().stats }
      if (snapshot.startedAt) startedAt.value = snapshot.startedAt
      completed.value = snapshot.completed === true
    }
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

function createPayload(snapshot, isSubmission) {
  const stats = snapshot.stats
  return {
    experimentId,
    stepId,
    status: isSubmission ? 1 : 0,
    durationSeconds: Math.max(0, stats.duration_seconds - persistedStats.duration_seconds),
    operationCount: Math.max(0, stats.operation_count - persistedStats.operation_count),
    errorCount: Math.max(0, stats.error_count - persistedStats.error_count),
    ...(isSubmission ? { score: Math.max(0, 100 - stats.error_count * 10) } : {}),
    resultData: JSON.stringify({
      ...snapshot,
      startedAt: startedAt.value,
      completed: isSubmission
    }),
    startedAt: startedAt.value
  }
}

async function handleSave() {
  if (!canWrite.value) return
  saving.value = true
  const snapshot = formRef.value.getSnapshot()
  try {
    await saveDraft(createPayload(snapshot, false))
    persistedStats = { ...snapshot.stats }
    ElMessage.success('两张工作票进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

async function handleTicketSubmit(result) {
  if (!canWrite.value) return
  if (!result.success) {
    ElMessage.error(`${result.errors[0].message}（错误次数：${result.errorCount}）`)
    return
  }
  submitting.value = true
  try {
    await submitStep(createPayload(result.data, true))
    persistedStats = { ...result.data.stats }
    completed.value = true
    ElMessage.success('两张工作票已完成')
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}
onMounted(handleRestore)
</script>

<style scoped>
.experiment-scene {
  /* 悬挂绳子的旋转边界不应撑宽窄屏页面。 */
  overflow-x: clip;
  width: 100vw;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10vh 0 9vh;
  box-sizing: border-box;
  background: var(--img-hwt-bg) center / cover no-repeat;
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
@media (max-height: 600px) {
  .experiment-scene {
    /* 悬挂绳子的旋转边界不应撑宽窄屏页面。 */
    overflow-x: clip;
    padding-top: 9vh;
  }
}
</style>
