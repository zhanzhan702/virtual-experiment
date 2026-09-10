<template>
  <el-dialog v-model="visible" title="完成历史" width="900px" @closed="onClosed">
    <p v-if="student" class="student-tip">
      学生：<strong>{{ student.name }}</strong
      >（{{ student.username }}）
    </p>

    <el-skeleton v-if="loading" :rows="5" animated />

    <el-table v-else-if="rows.length" :data="rows" border size="small">
      <el-table-column prop="templateName" label="实验名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="类型" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="row.category === 'high_voltage' ? 'danger' : 'primary'" size="small">
            {{ row.category === 'high_voltage' ? '高压' : '低压' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已完成' : '进行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="系统分" width="80" align="center">
        <template #default="{ row }">{{ row.percentScore ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="人工分" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.manualScore != null" class="manual-score">{{ row.manualScore }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="150">
        <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="用时" width="90" align="center">
        <template #default="{ row }">{{ formatDuration(row.totalDuration) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 1">
            <el-button link type="primary" @click="openScoreEditor(row)">改分</el-button>
            <el-button v-if="row.manualScore != null" link type="info" @click="revokeScore(row)">
              撤销
            </el-button>
          </template>
          <span v-else class="muted">未完成</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="暂无实验记录" :image-size="80" />

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 改分输入：独立小弹窗，避免在表格里塞输入框 -->
  <el-dialog
    v-model="editorVisible"
    title="修改分数"
    width="380px"
    :close-on-click-modal="false"
    append-to-body
  >
    <p class="editor-tip">满分 100（百分制）。当前系统分：{{ editing?.percentScore ?? '—' }}</p>
    <el-form label-width="70px">
      <el-form-item label="新分数">
        <el-input-number v-model="newScore" :min="0" :max="100" :precision="1" :step="1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editorVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitScore">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchStudentExperiments, updateExperimentScore } from '@/api/admin-grade'

const emit = defineEmits(['changed'])

const visible = ref(false)
const loading = ref(false)
const rows = ref([])
const student = ref(null)

const editorVisible = ref(false)
const editing = ref(null)
const newScore = ref(0)
const submitting = ref(false)

/** 打开弹窗并加载该生的完成历史 */
async function open(row) {
  visible.value = true
  loading.value = true
  student.value = row
  rows.value = []
  await load()
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchStudentExperiments(student.value.userId)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载完成历史失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

function openScoreEditor(row) {
  editing.value = row
  newScore.value = row.manualScore != null ? Number(row.manualScore) : Number(row.percentScore ?? 0)
  editorVisible.value = true
}

async function submitScore() {
  const exp = editing.value
  const name = student.value?.name

  try {
    await ElMessageBox.confirm(
      `确定将「${name}」的「${exp.templateName}」分数改为 ${newScore.value} 分？`,
      '请确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await updateExperimentScore(exp.experimentId, newScore.value)
    ElMessage.success('分数修改成功')
    editorVisible.value = false
    await load()
    // 通知外层刷新成绩表，否则关掉弹窗后主表仍是旧分数
    emit('changed')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

async function revokeScore(row) {
  try {
    await ElMessageBox.confirm(
      `确定撤销「${student.value?.name}」的「${row.templateName}」人工改分？将恢复为系统分。`,
      '请确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await updateExperimentScore(row.experimentId, null)
    ElMessage.success('已撤销改分')
    await load()
    emit('changed')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '撤销失败')
  }
}

function onClosed() {
  rows.value = []
  student.value = null
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

function formatDuration(seconds) {
  if (seconds == null) return '—'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

defineExpose({ open })
</script>

<style scoped>
.student-tip,
.editor-tip {
  margin: 0 0 16px;
  color: #606266;
}

.manual-score {
  color: var(--admin-primary);
  font-weight: 600;
}

.muted {
  color: #909399;
}
</style>
