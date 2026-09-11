<template>
  <div class="org-manage-view">
    <!-- 左：组织架构树 -->
    <aside class="org-aside admin-card">
      <OrgTreePanel ref="treeRef" @select="onSelectNode" />
    </aside>

    <!-- 右：节点编辑表单 -->
    <section class="org-main admin-card">
      <div class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, i) in breadcrumb" :key="i">
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <OrgNodeForm
        :node="current"
        :saving="saving"
        :siblings="current?.siblings || []"
        @save="onSave"
        @delete="onDelete"
        @create="onCreate"
        @move="onMove"
      />
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

.org-main {
  flex: 1;
  min-width: 0;
  max-width: 720px;
  min-height: calc(100vh - 136px);
}

.breadcrumb-bar {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--admin-border);
  font-size: 13px;
}
</style>
