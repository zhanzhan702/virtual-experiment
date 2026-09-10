<template>
  <div>
    <el-form class="search-bar" :inline="true" @submit.prevent>
      <el-form-item label="姓名">
        <el-input
          v-model="name"
          placeholder="请输入姓名"
          clearable
          @keyup.enter="emitSearch"
          @clear="emitSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="emitSearch">搜索 / 刷新</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="姓名" min-width="90" />
      <el-table-column prop="username" label="用户名" min-width="110" />
      <el-table-column label="高压" width="88" align="center">
        <template #default="{ row }">
          <el-tag :type="row.highDone ? 'success' : 'info'" size="small">
            {{ row.highDone ? '已完成' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="高压得分" width="88" align="center">
        <template #default="{ row }">
          <span :class="{ 'manual-score': row.highManual }">
            {{ row.highScore ?? '—' }}
            <el-tooltip v-if="row.highManual" content="人工改分" placement="top">
              <span class="manual-mark">*</span>
            </el-tooltip>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="低压" width="88" align="center">
        <template #default="{ row }">
          <el-tag :type="row.lowDone ? 'success' : 'info'" size="small">
            {{ row.lowDone ? '已完成' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="低压得分" width="88" align="center">
        <template #default="{ row }">
          <span :class="{ 'manual-score': row.lowManual }">
            {{ row.lowScore ?? '—' }}
            <el-tooltip v-if="row.lowManual" content="人工改分" placement="top">
              <span class="manual-mark">*</span>
            </el-tooltip>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="emit('detail', row)">完成历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="pager"
      :page-sizes="[10, 15, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="emitPage"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 }
})

const emit = defineEmits(['search', 'page-change', 'detail'])

const name = ref('')
const page = ref(props.page)
const size = ref(props.size)

// 父组件重置页码（如切换班级）时同步本地状态
watch(
  () => props.page,
  v => (page.value = v)
)
watch(
  () => props.size,
  v => (size.value = v)
)

function emitSearch() {
  emit('search', name.value)
}

function onReset() {
  name.value = ''
  emit('search', '')
}

function emitPage() {
  emit('page-change', { page: page.value, size: size.value })
}

/** 切换每页条数后回到第 1 页，否则可能停在越界页码上 */
function onSizeChange() {
  page.value = 1
  emit('page-change', { page: 1, size: size.value })
}
</script>

<style scoped>
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

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 人工改分的分数用青绿加粗，并带一个星号角标，一眼可辨 */
.manual-score {
  color: var(--admin-primary);
  font-weight: 600;
}

.manual-mark {
  cursor: help;
}
</style>
