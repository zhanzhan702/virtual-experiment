<template>
    <div class="tool-selection-view">
        <WizardInventorySelection :categories="categories" @finish="handleFinish" />
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
    width: 100%;
    height: 100vh;
    overflow: hidden;
    padding: 14px 18px;
    box-sizing: border-box;
}

.tool-selection-view::before {
    content: '';
    position: fixed;
    inset: 0;
    background-image: url('@/assets/images/WISBackground.png');
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    z-index: 0;
    pointer-events: none;
}

.tool-selection-view > * {
    position: relative;
    z-index: 1;
}
</style>
