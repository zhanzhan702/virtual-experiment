<template>
    <div class="wizard-container">
        <!-- 步骤条（下移） -->
        <div class="steps-wrapper">
            <div class="steps-bar">
                <div v-for="(cat, index) in categories" :key="cat.key" class="step-item" :class="{
                    active: index === activeStep,
                    done: index < activeStep,
                    'has-error': hasStepError(cat.key)
                }" @click="goToStep(index)">
                    <div class="step-dot">
                        <el-icon v-if="index < activeStep && !hasStepError(cat.key)" class="step-check">
                            <Check />
                        </el-icon>
                        <el-icon v-else-if="hasStepError(cat.key)" class="step-error-icon">
                            <Close />
                        </el-icon>
                        <span v-else class="step-num">{{ index + 1 }}</span>
                    </div>
                    <div class="step-info">
                        <el-icon class="step-icon">
                            <component :is="cat.icon" />
                        </el-icon>
                        <span class="step-title">{{ cat.title }}</span>
                        <span class="step-count">
                            {{ getSelectedCount(cat.key) }}/{{ getRequiredCount(cat) }}
                        </span>
                    </div>
                    <div v-if="index < categories.length - 1" class="step-line"
                        :class="{ filled: index < activeStep }" />
                </div>
            </div>
        </div>

        <!-- 主体区域 -->
        <div class="content-layout">
            <!-- 左侧：已选工具 -->
            <div class="left-panel">
                <div class="left-header">
                    <span class="left-title">已选工具</span>
                    <el-button text size="small" type="danger" :disabled="totalSelected === 0" @click="resetCurrent">
                        <el-icon><Delete /></el-icon>
                    </el-button>
                </div>
                <div v-if="totalSelected === 0" class="left-empty">暂未选择工具</div>
                <div v-else class="left-list">
                    <div v-for="tool in allSelectedTools" :key="tool.id" class="left-item"
                        :class="{ 'left-err': isWrong(tool.categoryKey, tool.id) }"
                        @click="removeTool(tool)">
                        <span class="left-ico">{{ tool.icon }}</span>
                        <span class="left-name">{{ tool.name }}</span>
                        <span class="left-cat">{{ tool.categoryTitle }}</span>
                        <el-icon class="left-del"><Close /></el-icon>
                    </div>
                </div>
            </div>

            <!-- 中间：工器具卡片 -->
            <div class="tool-panel">
                <!-- 工具卡片网格（分页） -->
                <div class="tool-grid">
                    <div v-for="tool in currentPageTools" :key="tool.id" class="tool-card" :class="{
                        selected: isSelected(currentCategory.key, tool.id),
                        wrong: isWrong(currentCategory.key, tool.id),
                        'wrong-flash': isWrong(currentCategory.key, tool.id)
                    }" @click="toggleTool(tool)">
                        <div class="card-img">
                            <span class="card-emoji">{{ tool.icon }}</span>
                            <div v-if="isSelected(currentCategory.key, tool.id)" class="card-check">
                                <el-icon>
                                    <Check />
                                </el-icon>
                            </div>
                            <div v-else-if="isWrong(currentCategory.key, tool.id)" class="card-wrong-mark">
                                <el-icon>
                                    <Close />
                                </el-icon>
                            </div>
                        </div>
                        <div class="card-body">
                            <div class="card-name">{{ tool.name }}</div>
                            <div class="card-desc">{{ tool.description }}</div>
                        </div>
                    </div>
                </div>
                <!-- 分页导航 -->
                <div v-if="totalPages > 1" class="pagi-bar">
                    <button class="pagi-btn" :disabled="toolPage === 0" @click="prevToolPage">
                        <el-icon><ArrowLeft /></el-icon>
                    </button>
                    <div class="pagi-dots">
                        <span v-for="p in totalPages" :key="p" class="pagi-dot"
                            :class="{ active: p - 1 === toolPage }" />
                    </div>
                    <button class="pagi-btn" :disabled="toolPage >= totalPages - 1" @click="nextToolPage">
                        <el-icon><ArrowRight /></el-icon>
                    </button>
                </div>
            </div>

            <!-- 右侧人物装备面板 -->
            <div class="avatar-panel">
                <div class="avatar-header">
                    <span class="avatar-header-title">作业人员装备</span>
                    <el-tag size="small" type="info" effect="plain">
                        共 {{ totalEquipped }} 件
                    </el-tag>
                </div>

                <div class="character-section">
                    <div class="character-body">
                        <div class="character-svg">
                            <!-- 使用 SVG 人物剪影 -->
                            <svg viewBox="0 0 140 260" class="person-svg">
                                <!-- 头部 -->
                                <ellipse cx="70" cy="42" rx="28" ry="32" class="person-head"
                                    :class="{ equipped: hasSlotItem('head') }" />
                                <!-- 安全帽 -->
                                <ellipse v-if="hasSlotItem('head')" cx="70" cy="28" rx="34" ry="14"
                                    class="person-helmet" />
                                <!-- 眼睛/护目镜 -->
                                <g v-if="hasSlotItem('eye')">
                                    <rect x="48" y="38" width="18" height="10" rx="3" class="person-goggle" />
                                    <rect x="74" y="38" width="18" height="10" rx="3" class="person-goggle" />
                                    <line x1="66" y1="43" x2="74" y2="43" class="person-goggle-bridge" />
                                </g>
                                <!-- 身体 -->
                                <rect x="34" y="72" width="72" height="90" rx="12" class="person-body"
                                    :class="{ equipped: hasSlotItem('body') }" />
                                <!-- 手臂 -->
                                <rect x="10" y="78" width="28" height="60" rx="12" class="person-arm"
                                    :class="{ equipped: hasSlotItem('hand') }" />
                                <rect x="102" y="78" width="28" height="60" rx="12" class="person-arm"
                                    :class="{ equipped: hasSlotItem('hand') }" />
                                <!-- 手部手套 -->
                                <ellipse v-if="hasSlotItem('hand')" cx="24" cy="142" rx="16" ry="12"
                                    class="person-glove" />
                                <ellipse v-if="hasSlotItem('hand')" cx="116" cy="142" rx="16" ry="12"
                                    class="person-glove" />
                                <!-- 腿部 -->
                                <rect x="42" y="160" width="22" height="70" rx="8" class="person-leg"
                                    :class="{ equipped: hasSlotItem('foot') }" />
                                <rect x="76" y="160" width="22" height="70" rx="8" class="person-leg"
                                    :class="{ equipped: hasSlotItem('foot') }" />
                                <!-- 鞋子 -->
                                <ellipse v-if="hasSlotItem('foot')" cx="53" cy="234" rx="18" ry="10"
                                    class="person-shoe" />
                                <ellipse v-if="hasSlotItem('foot')" cx="87" cy="234" rx="18" ry="10"
                                    class="person-shoe" />
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="equipment-slots">
                    <div v-for="slot in equipmentSlots" :key="slot.key" class="slot-row">
                        <div class="slot-label">
                            <el-icon class="slot-icon" :size="14">
                                <component :is="slot.icon" />
                            </el-icon>
                            <span>{{ slot.label }}</span>
                        </div>
                        <div class="slot-tags">
                            <el-tag v-for="(name, idx) in slot.items" :key="idx" size="small"
                                :type="slot.hasError ? 'danger' : 'success'" effect="light" round>
                                {{ name }}
                            </el-tag>
                            <span v-if="slot.items.length === 0" class="slot-empty">--</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="bottom-bar">
            <div class="action-bar">
                <el-button size="large" @click="prevStep" :disabled="activeStep === 0">
                    <el-icon><ArrowLeft /></el-icon> 上一步
                </el-button>
                <el-button v-if="!isLastStep" size="large" type="primary" :disabled="!canProceed" @click="nextStep">
                    下一步
                    <el-icon>
                        <ArrowRight />
                    </el-icon>
                </el-button>
                <el-button v-else size="large" type="success" :disabled="!allPagesFilled" @click="submitSelection"
                    :loading="submitting">
                    <el-icon>
                        <Finished />
                    </el-icon>
                    提交工器具选择
                </el-button>
            </div>
        </div>

        <!-- 提交错误反馈对话框 -->
        <el-dialog v-model="errorDialogVisible" title="校验不通过" width="560px" :close-on-click-modal="false" center>
            <div class="err-dialog">
                <el-icon :size="44" color="#f56c6c"><WarningFilled /></el-icon>
                <p>共 <strong>{{ errorMessages.length }}</strong> 处错误：</p>
                <div class="err-list">
                    <div v-for="(msg, idx) in errorMessages" :key="idx" class="err-item"
                        @click="goToErrorPage(msg.categoryKey)">
                        <el-tag type="danger" size="small" round>{{ msg.categoryTitle }}</el-tag>
                        <span>{{ msg.text }}</span>
                        <el-icon><Right /></el-icon>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button type="primary" @click="errorDialogVisible = false">去修改</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
    Check, Close, Delete, ArrowLeft, ArrowRight, Finished,
    InfoFilled, WarningFilled, Right, UserFilled, Monitor,
    SetUp, Connection, Box, Aim, Headset, Pointer, TakeawayBox
} from '@element-plus/icons-vue'

const props = defineProps({
    categories: {
        type: Array,
        required: true
    }
})

const emit = defineEmits(['finish', 'operation', 'submit-error'])

// ============ 状态 ============
const activeStep = ref(0)
const submitting = ref(false)
const hasSubmitted = ref(false)
const selectedMap = ref({})
const errorMap = ref({})
const errorDialogVisible = ref(false)
const toolPage = ref(0)
const TOOLS_PER_PAGE = 9

// 初始化
props.categories.forEach(cat => {
    selectedMap.value[cat.key] = []
    errorMap.value[cat.key] = []
})

// ============ 计算属性 ============
const currentCategory = computed(() => props.categories[activeStep.value])

// 当前分类工具分页
const totalPages = computed(() => {
    return Math.ceil(currentCategory.value.tools.length / TOOLS_PER_PAGE)
})
const currentPageTools = computed(() => {
    const start = toolPage.value * TOOLS_PER_PAGE
    return currentCategory.value.tools.slice(start, start + TOOLS_PER_PAGE)
})

const isLastStep = computed(() => activeStep.value === props.categories.length - 1)

const currentSelected = computed(() => selectedMap.value[currentCategory.value.key] || [])

const currentErrors = computed(() => errorMap.value[currentCategory.value.key] || [])

function getSelectedCount(catKey) {
    return (selectedMap.value[catKey] || []).length
}

function getRequiredCount(cat) {
    if (cat.requiredCount !== undefined && cat.requiredCount !== null) {
        return cat.requiredCount
    }
    return cat.correctIds.length
}

const canProceed = computed(() => {
    const cat = currentCategory.value
    const selected = selectedMap.value[cat.key] || []
    return selected.length >= getRequiredCount(cat)
})

const allPagesFilled = computed(() => {
    return props.categories.every(cat => {
        const selected = selectedMap.value[cat.key] || []
        return selected.length >= getRequiredCount(cat)
    })
})

const selectionStatusType = computed(() => {
    const count = currentSelected.value.length
    const required = getRequiredCount(currentCategory.value)
    if (count >= required) return 'success'
    if (count > 0) return 'warning'
    return 'info'
})

const selectionStatusText = computed(() => {
    const count = currentSelected.value.length
    const required = getRequiredCount(currentCategory.value)
    if (count >= required) return `已满足要求 (${count}/${required})`
    return `还需 ${required - count} 项 (${count}/${required})`
})

const totalSelected = computed(() => {
    return Object.values(selectedMap.value).reduce((sum, arr) => sum + arr.length, 0)
})

const totalEquipped = computed(() => {
    return equipmentSlots.value.reduce((sum, s) => sum + s.items.length, 0)
})

// 所有已选工器具（带分类信息）
const allSelectedTools = computed(() => {
    const result = []
    props.categories.forEach(cat => {
        (selectedMap.value[cat.key] || []).forEach(id => {
            const tool = cat.tools.find(t => t.id === id)
            if (tool) {
                result.push({
                    ...tool,
                    categoryKey: cat.key,
                    categoryTitle: cat.title
                })
            }
        })
    })
    return result
})

// 装备槽位映射
const slotIconMap = {
    body: Box,
    head: Aim,
    eye: Headset,
    hand: Pointer,
    foot: TakeawayBox,
    device: Monitor,
    tool: SetUp,
    wire: Connection
}

const equipmentSlots = computed(() => {
    const slotOrder = ['body', 'head', 'eye', 'hand', 'foot', 'device', 'tool', 'wire']
    const slotLabelMap = {
        body: '身体', head: '头部', eye: '眼部', hand: '手部',
        foot: '足部', device: '终端', tool: '工器具', wire: '线材'
    }

    const slots = {}

    // 收集所有分类的选中项
    props.categories.forEach(cat => {
        (selectedMap.value[cat.key] || []).forEach(id => {
            const tool = cat.tools.find(t => t.id === id)
            if (!tool || !tool.slot) return
            if (!slots[tool.slot]) slots[tool.slot] = { items: [], hasError: false }
            slots[tool.slot].items.push(tool.name)
        })
    })

    // 检查错误
    props.categories.forEach(cat => {
        (errorMap.value[cat.key] || []).forEach(id => {
            const tool = cat.tools.find(t => t.id === id)
            if (!tool || !tool.slot) return
            if (slots[tool.slot]) {
                slots[tool.slot].hasError = true
            }
        })
    })

    return slotOrder.map(key => ({
        key,
        label: slotLabelMap[key] || key,
        icon: slotIconMap[key] || Box,
        items: slots[key] ? slots[key].items : [],
        hasError: slots[key] ? slots[key].hasError : false
    }))
})

// 校验错误消息
const errorMessages = computed(() => {
    const msgs = []
    props.categories.forEach(cat => {
        const errors = errorMap.value[cat.key] || []
        if (errors.length === 0) return

        // 找缺失的正确项
        const selected = selectedMap.value[cat.key] || []
        const missingCorrect = cat.correctIds.filter(id => !selected.includes(id))
        const missingNames = missingCorrect.map(id => {
            const t = cat.tools.find(tool => tool.id === id)
            return t ? t.name : `ID:${id}`
        })

        const wrongNames = errors.map(id => {
            const t = cat.tools.find(tool => tool.id === id)
            return t ? t.name : `ID:${id}`
        })

        let text = ''
        if (missingNames.length > 0 && wrongNames.length > 0) {
            text = `选错了 ${wrongNames.join('、')}，应为 ${missingNames.join('、')}`
        } else if (missingNames.length > 0) {
            text = `缺少 ${missingNames.join('、')}`
        } else if (wrongNames.length > 0) {
            text = `${wrongNames.join('、')} 选择有误`
        }

        msgs.push({
            categoryKey: cat.key,
            categoryTitle: cat.title,
            text
        })
    })
    return msgs
})

// ============ 方法 ============
function isSelected(catKey, toolId) {
    return (selectedMap.value[catKey] || []).includes(toolId)
}

function isWrong(catKey, toolId) {
    return (errorMap.value[catKey] || []).includes(toolId)
}

function hasStepError(catKey) {
    return (errorMap.value[catKey] || []).length > 0
}

function hasSlotItem(slotKey) {
    return equipmentSlots.value.some(
        s => s.key === slotKey && s.items.length > 0
    )
}

function toggleTool(tool) {
    const catKey = currentCategory.value.key
    const selected = selectedMap.value[catKey]
    const idx = selected.indexOf(tool.id)

    // 如果已经提交过且当前页面有错误，切换选择时清除错误状态
    if (hasSubmitted.value && errorMap.value[catKey].length > 0) {
        errorMap.value[catKey] = []
    }

    if (idx > -1) {
        selected.splice(idx, 1)
    } else {
        selected.push(tool.id)
    }
    emit('operation')
}

function removeTool(tool) {
    const selected = selectedMap.value[tool.categoryKey] || []
    const idx = selected.indexOf(tool.id)
    if (idx > -1) {
        selected.splice(idx, 1)
    }
    if (hasSubmitted.value && errorMap.value[tool.categoryKey].length > 0) {
        errorMap.value[tool.categoryKey] = []
    }
    emit('operation')
}

function nextToolPage() {
    if (toolPage.value < totalPages.value - 1) toolPage.value++
}
function prevToolPage() {
    if (toolPage.value > 0) toolPage.value--
}

function resetCurrent() {
    selectedMap.value[currentCategory.value.key] = []
    errorMap.value[currentCategory.value.key] = []
    emit('operation')
}

function goToStep(index) {
    activeStep.value = index
}

function goToErrorPage(catKey) {
    const idx = props.categories.findIndex(c => c.key === catKey)
    if (idx >= 0) {
        activeStep.value = idx
        errorDialogVisible.value = false
    }
}

function prevStep() {
    if (activeStep.value > 0) {
        activeStep.value--
    }
}

function nextStep() {
    if (!canProceed.value) {
        const cat = currentCategory.value
        ElMessage.warning(`请至少选择 ${getRequiredCount(cat)} 项工器具`)
        return
    }
    activeStep.value++
}

function submitSelection() {
    // 校验所有页面
    let hasAnyError = false
    const newErrorMap = {}

    props.categories.forEach(cat => {
        newErrorMap[cat.key] = []
        const selected = selectedMap.value[cat.key] || []

        // 找出选错的项目
        const wrongIds = selected.filter(id => !cat.correctIds.includes(id))
        // 找出缺失的正确项目
        const missingIds = cat.correctIds.filter(id => !selected.includes(id))

        if (wrongIds.length > 0 || missingIds.length > 0) {
            newErrorMap[cat.key] = wrongIds
            hasAnyError = true
        }
    })

    errorMap.value = newErrorMap
    hasSubmitted.value = true
    submitting.value = false

    if (hasAnyError) {
        // 统计本次有错误的页面数
        const errorPageCount = Object.values(newErrorMap).filter(arr => arr.length > 0).length
        emit('submit-error', errorPageCount)
        errorDialogVisible.value = true
    } else {
        // 全部正确
        ElMessage.success('🎉 工器具选择全部正确！')
        emit('finish', { ...selectedMap.value })
    }
}

defineExpose({ selectedMap })
</script>

<style scoped>
/* ========== 容器 ========== */
.wizard-container {
    width: 100%;
    max-width: 1200px;
    height: 100%;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 10px;
    overflow: hidden;
}

/* ========== 步骤条（下移） ========== */
.steps-wrapper {
    background: rgba(255,255,255,.92);
    backdrop-filter: blur(8px);
    border-radius: 12px;
    padding: 14px 24px;
    margin-top: 32px;
    flex-shrink: 0;
    box-shadow: 0 2px 12px rgba(0,0,0,.06);
}

.steps-bar {
    display: flex;
    align-items: center;
    justify-content: center;
}

.step-item {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;
    transition: all .25s;
}
.step-item:hover { transform: translateY(-1px); }
.step-dot {
    width: 30px; height: 30px;
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    background: #e8eaed; color: #999;
    font-weight: 700; font-size: 13px;
    transition: all .35s; flex-shrink: 0;
}
.step-item.active .step-dot {
    background: linear-gradient(135deg,#409eff,#66b1ff);
    color: #fff; box-shadow: 0 3px 10px rgba(64,158,255,.35);
    transform: scale(1.08);
}
.step-item.done .step-dot {
    background: linear-gradient(135deg,#67c23a,#85ce61);
    color: #fff; box-shadow: 0 3px 10px rgba(103,194,58,.3);
}
.step-item.has-error .step-dot {
    background: linear-gradient(135deg,#f56c6c,#f89898);
    color: #fff; box-shadow: 0 3px 10px rgba(245,108,108,.35);
    animation: pulse 2s infinite;
}
@keyframes pulse { 0%,100% { box-shadow: 0 3px 10px rgba(245,108,108,.35); } 50% { box-shadow: 0 3px 18px rgba(245,108,108,.55); } }
.step-check,.step-error-icon { font-size: 15px; font-weight: 700; }
.step-num { font-size: 13px; }
.step-info { display: flex; align-items: center; gap: 5px; white-space: nowrap; }
.step-icon { font-size: 15px; color: #909399; transition: color .3s; }
.step-item.active .step-icon { color: #409eff; }
.step-item.done .step-icon { color: #67c23a; }
.step-title { font-size: 13px; font-weight: 500; color: #606266; }
.step-item.active .step-title { color: #303133; font-weight: 600; }
.step-count { font-size: 11px; color: #909399; background: #f5f7fa; padding: 1px 7px; border-radius: 8px; }
.step-line { width: 40px; height: 2px; background: #e4e7ed; margin: 0 10px; border-radius: 1px; transition: background .4s; }
.step-line.filled { background: #67c23a; }

/* ========== 主体布局（撑满剩余高度） ========== */
.content-layout {
    flex: 1;
    display: flex;
    gap: 14px;
    min-height: 0;
    overflow: hidden;
}

/* ========== 左侧：已选工具面板 ========== */
.left-panel {
    flex: 0 0 200px;
    display: flex;
    flex-direction: column;
    background: rgba(255,255,255,.85);
    backdrop-filter: blur(4px);
    border-radius: 12px;
    padding: 12px;
    overflow: hidden;
}
.left-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    flex-shrink: 0;
}
.left-title { font-size: 14px; font-weight: 700; color: #303133; }
.left-empty {
    flex: 1;
    display: flex; align-items: center; justify-content: center;
    font-size: 12px; color: #c0c4cc;
}
.left-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 5px;
}
.left-list::-webkit-scrollbar { width: 3px; }
.left-list::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 2px; }
.left-item {
    display: flex; align-items: center; gap: 5px;
    padding: 5px 8px;
    border-radius: 6px;
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    cursor: pointer; transition: all .2s;
    font-size: 12px;
}
.left-item:hover { background: #ecf5ff; }
.left-err { border-color: #f56c6c; background: #fef0f0; }
.left-ico { font-size: 16px; flex-shrink: 0; }
.left-name { flex: 1; font-weight: 500; color: #303133; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.left-cat { font-size: 10px; color: #909399; background: #fff; padding: 0 5px; border-radius: 4px; flex-shrink: 0; }
.left-del { color: #c0c4cc; font-size: 11px; flex-shrink: 0; }
.left-del:hover { color: #f56c6c; }

/* ========== 中间：工具面板（无标题） ========== */
.tool-panel {
    flex: 1;
    min-width: 0;
    background: rgba(255,255,255,.88);
    backdrop-filter: blur(4px);
    border-radius: 12px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.tool-grid {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(165px, 1fr));
    gap: 10px;
    align-content: start;
    overflow-y: auto;
    padding-right: 4px;
}
.tool-grid::-webkit-scrollbar { width: 4px; }
.tool-grid::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 2px; }

/* ========== 工具卡片 ========== */
.tool-card {
    border: 2px solid #e4e7ed;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    transition: all .25s;
    background: #fff;
    position: relative;
}
.tool-card:hover {
    border-color: #b3d8ff;
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(64,158,255,.1);
}
.tool-card:active { transform: translateY(-1px) scale(.98); }
.tool-card.selected {
    border-color: #67c23a;
    background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
    box-shadow: 0 4px 16px rgba(103,194,58,.15);
}
.tool-card.wrong {
    border-color: #f56c6c !important;
    background: linear-gradient(135deg, #fef0f0, #fde2e2) !important;
    box-shadow: 0 4px 16px rgba(245,108,108,.2);
    animation: wrongPulse 1s ease-in-out;
}
@keyframes wrongPulse {
    0% { transform: translateX(0); }
    25% { transform: translateX(-3px); }
    50% { transform: translateX(3px); }
    75% { transform: translateX(-2px); }
    100% { transform: translateX(0); }
}
.card-img {
    height: 80px;
    display: flex; align-items: center; justify-content: center;
    background: #fafbfc; position: relative;
    transition: background .3s;
}
.tool-card.selected .card-img { background: #e8f5e0; }
.tool-card.wrong .card-img { background: #fde8e8; }
.card-emoji { font-size: 36px; transition: transform .3s; }
.tool-card:hover .card-emoji { transform: scale(1.1); }
.card-check, .card-wrong-mark {
    position: absolute; top: 6px; right: 6px;
    width: 22px; height: 22px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-size: 13px;
}
.card-check { background: #67c23a; color: #fff; }
.card-wrong-mark { background: #f56c6c; color: #fff; }
.card-body { padding: 10px; }
.card-name { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 3px; line-height: 1.3; }
.card-desc { font-size: 11px; color: #909399; line-height: 1.3; }

/* ========== 右侧人物面板 ========== */
.avatar-panel {
    flex: 0 0 280px;
    background: rgba(255,255,255,.88);
    backdrop-filter: blur(4px);
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}
.avatar-header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 14px 14px 0;
}
.avatar-header-title { font-size: 14px; font-weight: 700; color: #303133; }
.character-section { padding: 10px 14px 0; display: flex; justify-content: center; }
.character-body { width: 120px; position: relative; }
.person-svg { width: 120px; height: 210px; }
.person-head { fill: #fde8d0; stroke: #d4b896; stroke-width: 1.5; transition: fill .3s; }
.person-head.equipped { fill: #fef5e7; }
.person-helmet { fill: #409eff; stroke: #337ecc; stroke-width: 1.5; }
.person-goggle { fill: none; stroke: #606266; stroke-width: 1.5; }
.person-goggle-bridge { stroke: #606266; stroke-width: 1.5; }
.person-body { fill: #e8eaed; stroke: #c0c4cc; stroke-width: 1.5; transition: fill .3s; }
.person-body.equipped { fill: #409eff; stroke: #337ecc; }
.person-arm { fill: #e8eaed; stroke: #c0c4cc; stroke-width: 1.5; transition: fill .3s; }
.person-arm.equipped { fill: #ffd54f; stroke: #f5a623; }
.person-glove { fill: #ff9800; stroke: #e68900; stroke-width: 1.2; }
.person-leg { fill: #e8eaed; stroke: #c0c4cc; stroke-width: 1.5; transition: fill .3s; }
.person-leg.equipped { fill: #37474f; stroke: #263238; }
.person-shoe { fill: #37474f; stroke: #263238; stroke-width: 1.5; }
.equipment-slots { padding: 10px 14px 14px; flex: 1; overflow-y: auto; }
.equipment-slots::-webkit-scrollbar { width: 3px; }
.equipment-slots::-webkit-scrollbar-thumb { background: #e4e7ed; border-radius: 2px; }
.slot-row { display: flex; align-items: flex-start; gap: 8px; padding: 6px 0; border-bottom: 1px solid #f0f2f5; }
.slot-row:last-child { border-bottom: none; }
.slot-label { display: flex; align-items: center; gap: 4px; width: 56px; flex-shrink: 0; font-size: 12px; font-weight: 500; color: #606266; padding-top: 2px; }
.slot-icon { color: #909399; }
.slot-tags { display: flex; flex-wrap: wrap; gap: 3px; flex: 1; }
.slot-empty { font-size: 11px; color: #c0c4cc; line-height: 22px; }

/* ========== 底部操作栏 ========== */
.bottom-bar {
    flex-shrink: 0;
    background: rgba(255,255,255,.92);
    backdrop-filter: blur(8px);
    border-radius: 12px;
    padding: 10px 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.action-bar {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

/* ========== 分页导航 ========== */
.pagi-bar {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 8px 0 0;
    flex-shrink: 0;
}
.pagi-btn {
    width: 28px; height: 28px;
    border-radius: 50%;
    border: 1px solid #dcdfe6;
    background: #fff;
    color: #606266;
    cursor: pointer;
    display: flex; align-items: center; justify-content: center;
    transition: all .2s;
}
.pagi-btn:hover { border-color: #409eff; color: #409eff; }
.pagi-btn:disabled { opacity: .3; cursor: default; border-color: #e4e7ed; color: #c0c4cc; }
.pagi-dots { display: flex; gap: 6px; }
.pagi-dot {
    width: 7px; height: 7px;
    border-radius: 50%;
    background: #dcdfe6;
    transition: all .3s;
}
.pagi-dot.active { background: #409eff; transform: scale(1.3); }

/* ========== 错误对话框 ========== */
.err-dialog { text-align: center; }
.err-dialog p { margin: 10px 0 14px; font-size: 14px; color: #606266; }
.err-list { text-align: left; display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto; }
.err-item {
    display: flex; align-items: center; gap: 8px;
    padding: 10px 14px; background: #fef0f0;
    border: 1px solid #fde2e2; border-radius: 8px;
    cursor: pointer; transition: all .2s; font-size: 13px;
}
.err-item:hover { background: #fde2e2; border-color: #fab6b6; transform: translateX(4px); }
.err-item .el-icon:last-child { color: #f56c6c; margin-left: auto; }

/* ========== 响应式 ========== */
@media (max-width:900px) {
    .content-layout { flex-direction: column; }
    .left-panel { flex: none; width: 100%; max-height: 140px; }
    .left-list { flex-direction: row; flex-wrap: wrap; }
    .left-item { width: auto; flex: 0 0 auto; }
    .avatar-panel { flex: 0 0 auto; }
    .character-section { display: none; }
    .tool-grid { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 8px; }
    .steps-bar { flex-wrap: wrap; }
}
</style>
