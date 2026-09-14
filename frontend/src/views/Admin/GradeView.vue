<template>
  <div class="grade-view">
    <!-- 左：组织架构树 -->
    <aside class="grade-aside admin-card">
      <OrgTreePanel ref="treeRef" @select="onSelectNode" />
    </aside>

    <!-- 右：面包屑 + 内容 -->
    <section class="grade-main admin-card">
      <div class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, i) in breadcrumb" :key="i">
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 选中非班级节点：班级汇总 -->
      <ClassSummaryTable
        v-if="mode === 'summary'"
        :rows="classRows"
        :loading="loading"
        @drill="drillIntoClass"
      />

      <!-- 选中班级：概况卡 + 学生成绩 -->
      <template v-else>
        <ClassOverviewCard :data="overview" :loading="overviewLoading" />

        <StudentGradeTable
          :rows="studentRows"
          :loading="loading"
          :total="studentTotal"
          :page="query.page"
          :size="query.size"
          @search="onSearch"
          @page-change="onPageChange"
          @detail="onDetail"
        />
      </template>
    </section>

    <StudentGradeDialog ref="detailDialogRef" @changed="refreshCurrentView" />
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchClassOverview, fetchClassSummaries, fetchStudentGrades } from '@/api/admin-grade'
import OrgTreePanel from '@/components/Admin/OrgTreePanel.vue'
import ClassSummaryTable from '@/components/Admin/ClassSummaryTable.vue'
import ClassOverviewCard from '@/components/Admin/ClassOverviewCard.vue'
import StudentGradeTable from '@/components/Admin/StudentGradeTable.vue'
import StudentGradeDialog from '@/components/Admin/StudentGradeDialog.vue'

const treeRef = ref(null)
const detailDialogRef = ref(null)

const loading = ref(false)
const mode = ref('summary')
const currentNode = ref(null)

// 概况卡单独维护 loading：它与学生表是两个请求，用同一个标志会让其中一方先回来时
// 就把另一方的骨架屏也关掉
const overview = ref(null)
const overviewLoading = ref(false)

const classRows = ref([])
const studentRows = ref([])
const studentTotal = ref(0)

const query = reactive({ page: 1, size: 10, name: '' })

/** 面包屑：从根到当前节点的名称链 */
const breadcrumb = computed(() => {
  if (!currentNode.value) return ['请选择左侧组织节点']
  return currentNode.value.pathNames || [currentNode.value.name]
})

function onSelectNode(node) {
  currentNode.value = node
  query.page = 1
  query.name = ''

  // 班级节点看概况 + 学生名单，其余节点看其下班级汇总
  if (node.type === 'class') {
    mode.value = 'students'
    loadClassView()
  } else {
    mode.value = 'summary'
    loadClasses(node.id)
  }
}

/** 班级视图的两块数据（概况卡 + 学生表）一起拉 */
function loadClassView() {
  loadStudents()
  loadOverview()
}

async function loadClasses(orgId) {
  loading.value = true
  try {
    classRows.value = await fetchClassSummaries(orgId)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载班级汇总失败')
  } finally {
    loading.value = false
  }
}

async function loadStudents() {
  loading.value = true
  try {
    const res = await fetchStudentGrades({
      orgId: currentNode.value.id,
      name: query.name || undefined,
      page: query.page,
      size: query.size
    })

    // 搜索后页码可能越界，回到第 1 页重查，避免看到空表格却不知能翻回
    if (!res.records?.length && query.page > 1) {
      query.page = 1
      return loadStudents()
    }

    studentRows.value = res.records || []
    studentTotal.value = res.total || 0
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载学生成绩失败')
  } finally {
    loading.value = false
  }
}

/**
 * 拉班级概况。
 *
 * <p>失败时只清空概况卡并提示，不影响下方的学生成绩表 —— 概况是附加信息，
 * 不该因为它出错就让整个页面看起来挂了。
 */
async function loadOverview() {
  const orgId = currentNode.value?.id
  if (!orgId) return

  overviewLoading.value = true
  try {
    overview.value = await fetchClassOverview(orgId)
  } catch (err) {
    overview.value = null
    ElMessage.error(err.response?.data?.message || '加载班级概况失败')
  } finally {
    overviewLoading.value = false
  }
}

/** 从汇总表下钻到某个班级 */
function drillIntoClass(row) {
  currentNode.value = {
    id: row.orgId,
    name: row.className,
    type: 'class',
    pathNames: (row.orgName || '').split('/').filter(Boolean)
  }
  treeRef.value?.setCurrentKey(row.orgId)
  mode.value = 'students'
  query.page = 1
  query.name = ''
  loadClassView()
}

function onSearch(name) {
  query.name = name
  query.page = 1
  loadStudents()
}

function onPageChange({ page, size }) {
  query.page = page
  query.size = size
  loadStudents()
}

function onDetail(row) {
  detailDialogRef.value?.open(row)
}

/**
 * 弹窗里改过分后刷新当前视图。
 *
 * 改动同时影响分数、人工改分标记，还会波及概况卡与班级汇总的平均分，
 * 因此整块重新拉取，比只改内存里那一行更不容易漏更新。
 */
function refreshCurrentView() {
  if (mode.value === 'students') {
    loadClassView()
  } else {
    loadClasses(currentNode.value?.id)
  }
}
</script>

<style scoped>
.grade-view {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

/* 左栏固定宽度，右栏自适应 */
.grade-aside {
  flex: 0 0 230px;
  min-height: calc(100vh - 136px);
}

.grade-main {
  flex: 1;
  min-width: 0;
  min-height: calc(100vh - 136px);
}

.breadcrumb-bar {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--admin-border);
  font-size: 13px;
}
</style>
