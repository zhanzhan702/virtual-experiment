<template>
  <div class="admin-card">
    <!-- 搜索区 -->
    <el-form class="search-bar" :inline="true" @submit.prevent>
      <el-form-item label="姓名">
        <el-input
          v-model="query.name"
          placeholder="请输入姓名"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input
          v-model="query.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </el-form-item>
      <el-form-item label="电话">
        <el-input
          v-model="query.phone"
          placeholder="请输入联系电话"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </el-form-item>
      <el-form-item class="search-actions">
        <el-button type="primary" @click="handleSearch">搜索 / 刷新</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ genderText(row.gender) }}</template>
      </el-table-column>
      <el-table-column label="单位/班级" min-width="180">
        <template #default="{ row }">
          <el-tooltip :content="fullOrg(row.orgName)" placement="top" :disabled="!row.orgName">
            <span class="org-text">{{ shortOrg(row.orgName) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="联系电话" min-width="130">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="onResetPassword(row)">
            修改密码
          </el-button>
          <el-button type="primary" size="small" @click="onViewDetail(row)">详细信息</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      class="pager"
      :page-sizes="[10, 15, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="loadUsers"
      @size-change="onSizeChange"
    />

    <ResetPasswordDialog ref="resetDialogRef" />
    <UserDetailDialog ref="detailDialogRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchUserPage } from '@/api/admin-user'
import ResetPasswordDialog from '@/components/Admin/ResetPasswordDialog.vue'
import UserDetailDialog from '@/components/Admin/UserDetailDialog.vue'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  name: '',
  username: '',
  phone: ''
})

const resetDialogRef = ref(null)
const detailDialogRef = ref(null)

function genderText(gender) {
  if (gender === '1' || gender === 1) return '男'
  if (gender === '2' || gender === 2) return '女'
  return '—'
}

/** 组织路径按 / 拆段，去掉首尾空串 */
function orgParts(orgName) {
  return orgName ? orgName.split('/').filter(Boolean) : []
}

/** 单元格只显示末两级（年级 / 班级），完整路径过长会撑破列宽 */
function shortOrg(orgName) {
  const parts = orgParts(orgName)
  return parts.length ? parts.slice(-2).join(' / ') : '—'
}

/** 悬停显示完整路径；无组织时返回空串并禁用 tooltip */
function fullOrg(orgName) {
  const parts = orgParts(orgName)
  return parts.length ? parts.join(' / ') : ''
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await fetchUserPage({
      page: query.page,
      size: query.size,
      name: query.name || undefined,
      username: query.username || undefined,
      phone: query.phone || undefined
    })

    // 搜索条件变化后页码可能越界（如搜到只剩 3 条却停在 page=5），
    // 此时后端返回空 records，重置到第 1 页重查一次，避免用户看到空表格却不知能翻回
    if (!res.records?.length && query.page > 1) {
      query.page = 1
      return loadUsers()
    }

    records.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadUsers()
}

function handleReset() {
  query.name = ''
  query.username = ''
  query.phone = ''
  query.page = 1
  loadUsers()
}

/** 切换每页条数后回到第 1 页，否则可能停在越界页码上 */
function onSizeChange() {
  query.page = 1
  loadUsers()
}

function onResetPassword(row) {
  resetDialogRef.value?.open(row)
}

function onViewDetail(row) {
  detailDialogRef.value?.open(row)
}

onMounted(loadUsers)
</script>

<style scoped>
/* 搜索区：限定输入框宽度并让按钮组靠右，
   否则长 placeholder 会把整行撑破导致按钮换行 */
.search-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 4px;
}

.search-bar :deep(.el-form-item) {
  margin-bottom: 16px;
}

.search-bar :deep(.el-input) {
  width: 180px;
}

.search-actions {
  margin-left: auto !important;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 组织路径单行显示，超出用省略号；完整路径由 tooltip 呈现 */
.org-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

/* 操作列两个按钮的间距收紧，避免 180px 宽内换行 */
:deep(.el-table .cell > .el-button + .el-button) {
  margin-left: 8px;
}
</style>
