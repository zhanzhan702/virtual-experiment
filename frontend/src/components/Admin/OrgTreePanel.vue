<template>
  <div class="org-tree-panel">
    <div class="panel-title">
      <span class="panel-icon" />
      组织架构树
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-tree
      v-else
      ref="treeRef"
      :data="tree"
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      :default-expanded-keys="defaultExpanded"
      highlight-current
      :expand-on-click-node="false"
      @node-click="onNodeClick"
    >
      <!--
        树里用短名（「1班」）即可 —— 层级本身已经提供了上下文。
        需要拼接全名的是扁平化的班级汇总表，那里没有层级可参考。
      -->
      <template #default="{ data }">
        <span class="tree-node">{{ data.name }}</span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchOrgTree } from '@/api/admin-grade'

const emit = defineEmits(['select'])

const loading = ref(false)
const tree = ref([])
const treeRef = ref(null)

/** 默认展开到年级层（学校 / 学院 / 专业 / 年级 四层展开） */
const defaultExpanded = computed(() => {
  const ids = []
  const walk = (nodes, depth) => {
    for (const node of nodes) {
      if (depth >= 4) continue
      ids.push(node.id)
      if (node.children?.length) walk(node.children, depth + 1)
    }
  }
  walk(tree.value, 0)
  return ids
})

/**
 * 抛给父组件时带上从根到当前节点的名称链。
 *
 * <p>el-tree 的 node-click 第二个参数 treeNode 有 parent 链，
 * 顺着往上走即可得到路径；直接遍历 data 是拿不到的（子节点没有父引用）。
 */
function onNodeClick(data, treeNode) {
  const pathNames = []
  let cursor = treeNode
  while (cursor) {
    if (cursor.data?.name) pathNames.unshift(cursor.data.name)
    cursor = cursor.parent
  }
  emit('select', { ...data, pathNames })
}

onMounted(async () => {
  loading.value = true
  try {
    tree.value = await fetchOrgTree()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载组织架构失败')
  } finally {
    loading.value = false
  }
})

/** 供父组件在需要时定位某个节点 */
function setCurrentKey(key) {
  treeRef.value?.setCurrentKey(key)
}

defineExpose({ setCurrentKey })
</script>

<style scoped>
.org-tree-panel {
  height: 100%;
  padding: 16px 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding-left: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-text);
}

/* 标题前的小圆点，呼应原站「组织架构树」的图标位 */
.panel-icon {
  width: 6px;
  height: 14px;
  background: var(--admin-primary);
  border-radius: 1px;
}

.tree-node {
  font-size: 13px;
}

:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: 30px;
  border-radius: 4px;
}

:deep(.el-tree-node__content:hover) {
  background: var(--admin-primary-tint);
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: var(--admin-primary-tint);
  color: var(--admin-primary);
  font-weight: 600;
}
</style>
