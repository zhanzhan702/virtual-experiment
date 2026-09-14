<template>
  <div class="org-manage-view">
    <!-- 左：组织架构树 -->
    <aside class="org-aside admin-card">
      <OrgTreePanel ref="treeRef" @select="onSelectNode" />
    </aside>

    <!-- 右：编辑表单 + 节点详情 -->
    <section class="org-main admin-card">
      <div class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, i) in breadcrumb" :key="i">
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="org-main-body" :class="{ 'is-empty': !current }">
        <div class="org-form-area">
          <OrgNodeForm
            :node="current"
            :saving="saving"
            :siblings="current?.siblings || []"
            @save="onSave"
            @delete="onDelete"
            @create="onCreate"
            @move="onMove"
          />
        </div>

        <!--
          详情区：吃掉表单之外的全部宽度。
          加的初衷是消除宽屏右侧的大片留白，顺带让这一页多出「选中节点后能顺便看到什么」。
          数据全部来自已有接口 / 树节点自带的字段，没有为此新增后端接口。
        -->
        <div v-if="current" class="org-detail-area">
          <OrgClassStudents v-if="current.type === 'class'" :node="current" />

          <template v-else>
            <div class="panel-title">
              下级{{ childTypeLabel }}<span class="count">（{{ childNodes.length }}）</span>
            </div>

            <el-table
              v-if="childNodes.length"
              :data="childNodes"
              size="small"
              max-height="calc(100vh - 300px)"
            >
              <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
              <el-table-column label="类型" width="80" align="center">
                <template #default="{ row }">{{ TYPE_LABELS[row.type] || row.type }}</template>
              </el-table-column>
              <el-table-column prop="userCount" label="挂靠人数" width="90" align="center" />
            </el-table>

            <el-empty
              v-else
              :description="`该${TYPE_LABELS[current.type] || '节点'}下暂无子节点`"
              :image-size="80"
            />
          </template>
        </div>
      </div>
    </section>

    <!-- 新增节点弹窗 -->
    <el-dialog v-model="createVisible" :title="createTitle" width="420px">
      <el-form label-width="70px" @submit.prevent>
        <el-form-item label="父级">
          <el-input :model-value="createParentName" disabled placeholder="（根节点）" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input
            ref="createInputRef"
            v-model="createName"
            maxlength="50"
            placeholder="请输入名称"
            @keyup.enter="submitCreate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { createOrgNode, renameOrgNode, deleteOrgNode, moveOrgNode } from '@/api/admin-org'
import OrgTreePanel from '@/components/Admin/OrgTreePanel.vue'
import OrgNodeForm from '@/components/Admin/OrgNodeForm.vue'
import OrgClassStudents from '@/components/Admin/OrgClassStudents.vue'

const treeRef = ref(null)
const saving = ref(false)
const current = ref(null)

const createVisible = ref(false)
const createName = ref('')
const createParent = ref(null)
const createInputRef = ref(null)

const breadcrumb = computed(() => current.value?.pathNames || ['请选择左侧组织节点'])

const TYPE_LABELS = {
  university: '学校',
  college: '学院',
  major: '专业',
  grade: '年级',
  class: '班级'
}

/** 新增弹窗的标题随父级变化，让用户清楚正在往哪一级加 */
const createTitle = computed(() => {
  if (!createParent.value) return '新增学校'
  const childType = {
    university: 'college',
    college: 'major',
    major: 'grade',
    grade: 'class'
  }[createParent.value.type]
  return `新增${TYPE_LABELS[childType] || '节点'}`
})

const createParentName = computed(() => createParent.value?.name || '')

/** 各类型可添加的子节点类型；班级没有下一级 */
const CHILD_TYPES = {
  university: 'college',
  college: 'major',
  major: 'grade',
  grade: 'class'
}

const childTypeLabel = computed(() => TYPE_LABELS[CHILD_TYPES[current.value?.type]] || '节点')

/**
 * 下级节点直接取树节点自带的 children —— el-tree 的节点数据就是后端返回的完整节点，
 * 所以展示下级不需要再发一次请求。
 */
const childNodes = computed(() => current.value?.children || [])

function onSelectNode(node) {
  current.value = node
}

function onSave(name) {
  saving.value = true
  const id = current.value.id
  renameOrgNode(id, name)
    .then(async () => {
      ElMessage.success('保存成功')
      // 改名后父级链可能变了，用新名字重建 pathNames，避免面包屑显示旧名
      const pathNames = [...(current.value.pathNames || [])]
      pathNames[pathNames.length - 1] = name
      await treeRef.value?.refresh(id)
      if (current.value) {
        current.value = { ...current.value, name, pathNames }
      }
    })
    .catch(err => ElMessage.error(err.response?.data?.message || '保存失败'))
    .finally(() => (saving.value = false))
}

async function onDelete() {
  const id = current.value.id
  saving.value = true
  try {
    await deleteOrgNode(id)
    ElMessage.success('删除成功')
    current.value = null
    await treeRef.value?.refresh()
  } catch (err) {
    // 有子节点或有用户时后端会拒绝，这里直接展示后端给的具体原因
    ElMessage.error(err.response?.data?.message || '删除失败')
  } finally {
    saving.value = false
  }
}

async function onCreate(parent) {
  createParent.value = parent
  createName.value = ''
  createVisible.value = true
  await nextTick()
  createInputRef.value?.focus()
}

async function submitCreate() {
  const name = createName.value.trim()
  if (!name) {
    ElMessage.warning('请输入名称')
    return
  }

  saving.value = true
  try {
    const res = await createOrgNode({
      parentId: createParent.value?.id || undefined,
      name
    })
    ElMessage.success('新增成功')
    createVisible.value = false
    // 重载树并选中新节点，省去用户再点一次
    await treeRef.value?.refresh(res.id)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '新增失败')
  } finally {
    saving.value = false
  }
}

async function onMove(direction) {
  const id = current.value.id
  saving.value = true
  try {
    await moveOrgNode(id, direction)
    // 顺序变了，同级列表也要重取，所以传回当前节点 id 让它重新定位
    await treeRef.value?.refresh(id)
    ElMessage.success('排序已调整')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '排序调整失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.org-manage-view {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.org-aside {
  flex: 0 0 230px;
  min-height: calc(100vh - 136px);
}

/*
 * 这里原本有一条 max-width: 720px，是从成绩页抄骨架时带过来的遗留 ——
 * 成绩页的表格正好 640px 左右，限制宽度有意义；本页只有一张窄表单，
 * 于是 1920 宽屏下右侧空出约 900px。去掉后由「表单 + 详情」两栏填满。
 */
.org-main {
  flex: 1;
  min-width: 0;
  min-height: calc(100vh - 136px);
}

.org-main-body {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 表单字段不需要横向拉伸，给它固定宽度比铺满更好读 */
.org-form-area {
  flex: 0 0 420px;
  min-width: 0;
}

/* 未选中任何节点时不渲染详情区，让表单区独占整宽 ——
   否则页面上会并排出现两个「请选择左侧节点」的空状态，看着像出了错 */
.org-main-body.is-empty .org-form-area {
  flex: 1;
}

.org-detail-area {
  flex: 1;
  min-width: 0;
  min-height: 320px;
  padding-left: 24px;
  border-left: 1px solid var(--admin-border);
}

.panel-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-text);
}

.panel-title .count {
  color: var(--admin-text-muted);
  font-size: 13px;
  font-weight: 400;
}

/* 窄屏放不下两栏时改为上下堆叠，分隔线也跟着从竖变横 */
@media (max-width: 1200px) {
  .org-main-body {
    flex-direction: column;
  }

  .org-form-area,
  .org-detail-area {
    flex: none;
    width: 100%;
  }

  .org-detail-area {
    padding-left: 0;
    padding-top: 20px;
    border-left: none;
    border-top: 1px solid var(--admin-border);
  }
}

.breadcrumb-bar {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--admin-border);
  font-size: 13px;
}
</style>
