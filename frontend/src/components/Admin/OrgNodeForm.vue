<template>
  <div class="org-node-form">
    <!-- 未选中：提示 + 新增学校 -->
    <div v-if="!node" class="empty-state">
      <el-empty description="请选择左侧节点进行编辑" :image-size="90" />
      <el-button type="primary" @click="emit('create', null)">新增学校</el-button>
    </div>

    <template v-else>
      <el-form label-width="70px" @submit.prevent>
        <el-form-item label="父级">
          <el-input :model-value="parentName" disabled placeholder="（根节点）" />
        </el-form-item>

        <el-form-item label="名称">
          <el-input v-model="name" maxlength="50" placeholder="请输入名称" @keyup.enter="save" />
        </el-form-item>

        <el-form-item label="类型">
          <el-input :model-value="typeLabel" disabled />
        </el-form-item>

        <el-form-item label="排序">
          <el-button :disabled="!canMoveUp" @click="emit('move', 'UP')">↑ 上移</el-button>
          <el-button :disabled="!canMoveDown" @click="emit('move', 'DOWN')">↓ 下移</el-button>
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button type="primary" :disabled="!dirty" :loading="saving" @click="save">
          保存修改
        </el-button>
        <el-button type="danger" plain @click="confirmDelete">删除节点</el-button>
      </div>

      <el-divider />

      <!-- 班级是叶子节点，不能再加子节点 -->
      <div v-if="childTypeLabel" class="add-child">
        <div class="add-child-tip">可新增子节点类型：{{ childTypeLabel }}</div>
        <el-button @click="emit('create', node)">+ 新增{{ childTypeLabel }}</el-button>
      </div>
      <el-alert v-else type="info" :closable="false" title="班级为最末级，不能添加子节点" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  /** 当前选中节点；null 表示未选中 */
  node: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  /** 同级节点列表（含自身），用于判断能否上移/下移 */
  siblings: { type: Array, default: () => [] }
})

const emit = defineEmits(['save', 'delete', 'create', 'move'])

const name = ref('')

const TYPE_LABELS = {
  university: '学校',
  college: '学院',
  major: '专业',
  grade: '年级',
  class: '班级'
}

/** 各类型可添加的子节点类型；班级没有下一级 */
const CHILD_TYPES = {
  university: 'college',
  college: 'major',
  major: 'grade',
  grade: 'class'
}

watch(
  () => props.node,
  v => (name.value = v?.name || ''),
  { immediate: true }
)

const parentName = computed(() => props.node?.pathNames?.slice(-2, -1)[0] || '')
const typeLabel = computed(() => TYPE_LABELS[props.node?.type] || props.node?.type || '')
const childTypeLabel = computed(() => TYPE_LABELS[CHILD_TYPES[props.node?.type]] || '')

const dirty = computed(() => !!props.node && name.value.trim() !== props.node.name)

const index = computed(() => props.siblings.findIndex(s => s.id === props.node?.id))
const canMoveUp = computed(() => index.value > 0)
const canMoveDown = computed(() => index.value >= 0 && index.value < props.siblings.length - 1)

function save() {
  const trimmed = name.value.trim()
  if (!trimmed) {
    return
  }
  emit('save', trimmed)
}

async function confirmDelete() {
  try {
    await ElMessageBox.confirm(`确定删除「${props.node.name}」？此操作不可撤销。`, '请确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  emit('delete')
}
</script>

<style scoped>
.org-node-form {
  padding: 4px;
}

.empty-state {
  padding-top: 40px;
  text-align: center;
}

.actions {
  display: flex;
  gap: 8px;
  padding-left: 70px;
}

.add-child {
  display: flex;
  align-items: center;
  gap: 12px;
}

.add-child-tip {
  color: var(--admin-text-muted);
  font-size: 13px;
}
</style>
