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
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useOrgTreeStore } from '@/stores/orgTree'

const emit = defineEmits(['select'])

const orgTreeStore = useOrgTreeStore()
const treeRef = ref(null)

/**
 * 读到的是 store 里的共享数据，不是本地快照 ——
 * 「查看学生成绩」与「专业班级管理」两个实例因此始终一致，任一处增删改另一处自动更新。
 */
const tree = computed(() => orgTreeStore.tree)
const loading = computed(() => orgTreeStore.loading)

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
 * 抛给父组件时补充三项信息，省得父组件再去遍历树。
 *
 * <ul>
 *   <li>{@code pathNames} 从根到当前节点的名称链（面包屑用）—— el-tree 的
 *       node-click 第二个参数 treeNode 有 parent 链，顺着往上走即可；直接遍历 data 拿不到，子节点没有父引用
 *   <li>{@code siblings} 同级节点列表（含自身），父组件据此判断能否上移 / 下移
 * </ul>
 */
function onNodeClick(data, treeNode) {
  const pathNames = []
  let cursor = treeNode
  while (cursor) {
    if (cursor.data?.name) pathNames.unshift(cursor.data.name)
    cursor = cursor.parent
  }

  const parentData = treeNode.parent?.data
  const siblings = parentData?.children ?? tree.value

  emit('select', { ...data, pathNames, siblings })
}

async function load(force = false) {
  try {
    await orgTreeStore.ensure(force)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载组织架构失败')
  }
}

/*
 * 进页面时 ensure 一次：store 里已有数据就直接命中，不重发请求。
 * 「查看学生成绩」与「专业班级管理」来回切时，只有第一次会真的打 /admin/org/tree。
 */
onMounted(() => load())

/** 供父组件在需要时定位某个节点 */
function setCurrentKey(key) {
  treeRef.value?.setCurrentKey(key)
}

/**
 * 重新拉取树数据。
 *
 * <p>增删改后调用 —— 依赖 default-expanded-keys 不足以保留展开状态（它只在首次渲染生效），
 * 因此重载后会按传入的 key 重新定位并选中，避免整棵树折叠回初始状态。
 *
 * <p>强制重拉（force），因为调用方刚改过数据，缓存已不可信。
 */
async function refresh(keepKey) {
  await load(true)
  if (keepKey) {
    // 等 el-tree 用新数据渲染完再设置选中，否则节点还不存在
    await nextTick()
    treeRef.value?.setCurrentKey(keepKey)
  }
}

defineExpose({ refresh, setCurrentKey })
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
