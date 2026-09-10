<template>
  <div>
    <el-table
      v-loading="loading"
      :data="rows"
      highlight-current-row
      @row-click="onRowClick"
      class="clickable-table"
    >
      <el-table-column label="班级" min-width="240">
        <template #default="{ row }">
          <el-tooltip :content="row.orgName" placement="top" :disabled="!row.orgName">
            <span>{{ row.className }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="studentCount" label="人数" width="80" align="center" />
      <el-table-column label="高压完成" width="110" align="center">
        <template #default="{ row }">{{ row.highDone }}/{{ row.studentCount }}</template>
      </el-table-column>
      <el-table-column label="低压完成" width="110" align="center">
        <template #default="{ row }">{{ row.lowDone }}/{{ row.studentCount }}</template>
      </el-table-column>
      <el-table-column label="平均分" width="100" align="center">
        <template #default="{ row }">{{ row.avgScore ?? '—' }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="该节点下暂无班级" :image-size="80" />
  </div>
</template>

<script setup>
const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['drill'])

/** 点击行即下钻到该班级的学生成绩表 */
function onRowClick(row) {
  emit('drill', row)
}
</script>

<style scoped>
.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>
