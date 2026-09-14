<template>
  <div class="org-class-students">
    <div class="panel-title">
      <span class="panel-icon" />
      本班学生
      <span v-if="total" class="count">（{{ total }} 人）</span>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      size="small"
      max-height="calc(100vh - 300px)"
      empty-text="该班级暂无学生"
    >
      <el-table-column prop="name" label="姓名" min-width="90" show-overflow-tooltip />
      <el-table-column label="学号" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.studentNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="高压" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.highDone ? 'success' : 'info'" size="small">
            {{ row.highDone ? '已完成' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="低压" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.lowDone ? 'success' : 'info'" size="small">
            {{ row.lowDone ? '已完成' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!--
      单次只取 100 人。超出时明确告知，避免老师误以为这就是全班名单
      （完整名单在「查看学生成绩」页，那里带分页与搜索）。
    -->
    <p v-if="total > LIMIT" class="truncated-tip">
      仅显示前 {{ LIMIT }} 人，完整名单请到「查看学生成绩」页查看
    </p>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchStudentGrades } from '@/api/admin-grade'

/** 单次取回的上限。班级规模通常在几十人，够用；超出部分靠提示兜底 */
const LIMIT = 100

const props = defineProps({
  /** 当前选中的组织节点；只有 type 为 class 时才请求数据 */
  node: { type: Object, default: null }
})

const loading = ref(false)
const rows = ref([])
const total = ref(0)

/**
 * 只在选中班级时取数。
 *
 * 用后端已有的学生成绩分页接口（成绩页也在用），不额外新增接口 ——
 * 这里要的「谁做完高压、谁做完低压」正好是它的返回字段。
 */
async function load(orgId) {
  loading.value = true
  try {
    const res = await fetchStudentGrades({ orgId, page: 1, size: LIMIT })
    rows.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载本班学生失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => props.node,
  node => {
    if (node?.type === 'class') {
      load(node.id)
    } else {
      // 切到非班级节点：清掉上一个班的数据，否则会短暂显示错班的名单
      rows.value = []
      total.value = 0
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-text);
}

/* 标题前的青色小竖条，与左侧「组织架构树」面板保持同一视觉语言 */
.panel-icon {
  width: 6px;
  height: 14px;
  background: var(--admin-primary);
  border-radius: 1px;
}

.count {
  color: var(--admin-text-muted);
  font-size: 13px;
  font-weight: 400;
}

.truncated-tip {
  margin: 12px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
}
</style>
