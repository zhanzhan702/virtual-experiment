<template>
  <el-dialog v-model="visible" title="用户详情" width="820px" @closed="onClosed">
    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderText }}</el-descriptions-item>
        <el-descriptions-item label="生日">{{ detail.birthday || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detail.studentNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{
          formatTime(detail.createdAt)
        }}</el-descriptions-item>
        <el-descriptions-item label="单位/班级" :span="2">
          {{ detail.orgName || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="section-title">实验记录</h4>

      <el-table v-if="detail.experiments?.length" :data="detail.experiments" border size="small">
        <el-table-column
          prop="templateName"
          label="实验名称"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.category === 'high_voltage' ? 'danger' : 'primary'" size="small">
              {{ row.category === 'high_voltage' ? '高压' : '低压' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已完成' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="80" align="center">
          <template #default="{ row }">{{ row.score ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="用时" width="100" align="center">
          <template #default="{ row }">{{ formatDuration(row.totalDuration) }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="暂无实验记录" :image-size="80" />
    </template>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchUserDetail } from '@/api/admin-user'

const visible = ref(false)
const loading = ref(false)
const detail = ref(null)

const genderText = computed(() => {
  const g = detail.value?.gender
  if (g === '1' || g === 1) return '男'
  if (g === '2' || g === 2) return '女'
  return '—'
})

/** 打开弹窗并加载详情 */
async function open(user) {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    detail.value = await fetchUserDetail(user.id)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载详情失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

function onClosed() {
  detail.value = null
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
.section-title {
  margin: 20px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
