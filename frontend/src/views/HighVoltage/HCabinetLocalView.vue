<!-- 柜体局部操作：Leafer UI 画布 + 进线柜/计量柜/出线柜交互 -->
<template>
  <div class="cabinet-local-page">
    <!-- TODO: 使用·leafer ui实现该页面 -->
    <HSceneFrame :src="localBg" alt="柜体局部操作" aspect-ratio="16 / 9">

    </HSceneFrame>

    <!-- 右下角保存进度 -->
    <div class="save-bar-fixed">
      <el-button type="info" size="default" @click="saveProgress" :loading="saving">
        <el-icon>
          <FolderOpened />
        </el-icon> 保存进度
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened } from '@element-plus/icons-vue'
import HSceneFrame from '@/components/HighVoltage/HSceneFrame.vue'
import { saveDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import localBg from '@/assets/images/CabinetLocalOperation.png'

const route = useRoute()
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)

const saveProgress = async () => {
  saving.value = true
  try {
    await saveDraft({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 0,
      durationSeconds: 0,
      resultData: null,
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cabinet-local-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #020617;
}

.save-bar-fixed {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}
</style>
