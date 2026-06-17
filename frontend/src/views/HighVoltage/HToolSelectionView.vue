<template>
  <div class="tool-selection-view">
    <!-- 页面标题 -->
    <div class="view-header">
      <div class="view-title">
        <el-icon :size="24"><Suitcase /></el-icon>
        <span>选择工器具</span>
      </div>
      <div class="view-subtitle">
        请依次选择正确的个人防护用具、终端设备、工器具和线材，确保作业安全
      </div>
    </div>

    <WizardInventorySelection
      :categories="categories"
      @finish="handleFinish"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Suitcase } from '@element-plus/icons-vue'
import WizardInventorySelection from '@/components/HighVoltage/HWizardInventorySelection.vue'
import { categories } from '@/constants/tool-selection-config'
import { ref } from 'vue'

const router = useRouter()
const submittedResult = ref(null)

function handleFinish(selectedMap) {
  console.log('✅ 工器具选择结果:', selectedMap)
  submittedResult.value = selectedMap

  // 构建提交数据（转换为后端需要的格式）
  const resultPayload = categories.map(cat => {
    const toolIds = selectedMap[cat.key] || []
    return {
      category: cat.key,
      categoryTitle: cat.title,
      selected: toolIds.map(id => {
        const tool = cat.tools.find(t => t.id === id)
        return { id, name: tool ? tool.name : '' }
      })
    }
  })

  console.log('📤 提交到后端的数据:', resultPayload)

  /*
    后续可在此调用后端接口提交数据：
    import { submitToolSelection } from '@/api/experiment'
    await submitToolSelection(resultPayload)
  */

  ElMessage.success('工器具选择已完成，即将进入下一步...')

  // 正确后可以跳转到下一个步骤
  // router.push('/next-step')
}
</script>

<style scoped>
.tool-selection-view {
  position: relative;
  padding: 24px;
  height: 100%;
  min-height: 100vh;
  /* 半透明背景色叠加，让内容区域保持可读 */
  background: linear-gradient(180deg, rgba(240, 245, 255, 0.82) 0%, rgba(245, 247, 250, 0.82) 100%);
}

/* 背景图伪元素 */
.tool-selection-view::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url('@/assets/images/selection.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
}

/* 确保内容在背景之上 */
.tool-selection-view > * {
  position: relative;
  z-index: 1;
}

.view-header {
  text-align: center;
  margin-bottom: 24px;
}

.view-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.view-subtitle {
  font-size: 14px;
  color: #909399;
}
</style>
