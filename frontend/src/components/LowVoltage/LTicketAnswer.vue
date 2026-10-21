<template>
  <el-popover
    :visible="visible"
    :width="popoverWidth"
    placement="bottom-start"
    :persistent="false"
    @after-enter="handleFocus"
  >
    <template #reference>
      <button
        ref="triggerRef"
        type="button"
        class="answer-line"
        :class="{ invalid, empty: !selected.length }"
        :disabled="disabled"
        :aria-label="label"
        :aria-expanded="visible"
        @click="handleOpen"
      >
        <span v-if="selected.length" class="answer-text">
          <span v-for="option in selected" :key="option.value">{{ option.label }}</span>
        </span>
        <span v-else class="empty-answer">点击选择</span>
      </button>
    </template>
    <div
      ref="panelRef"
      class="answer-panel"
      role="dialog"
      :aria-label="label"
      @keydown.esc="handleCancel"
    >
      <div class="option-list">
        <template v-if="multiple">
          <el-checkbox-group v-model="pending" :aria-label="label">
            <el-checkbox v-for="option in options" :key="option.value" :value="option.value">
              {{ option.letter }}、{{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
        </template>
        <el-radio-group v-else v-model="pending" :aria-label="label">
          <el-radio v-for="option in options" :key="option.value" :value="option.value">
            {{ option.letter }}、{{ option.label }}
          </el-radio>
        </el-radio-group>
      </div>
      <div class="answer-actions">
        <span>答案：{{ pendingLetters || '未选择' }}</span>
        <el-button size="small" @click="handleCancel">取消</el-button>
        <el-button type="warning" size="small" @click="handleConfirm">确定</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  options: { type: Array, default: () => [] },
  multiple: { type: Boolean, default: false },
  label: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const visible = ref(false)
const pending = ref('')
const panelRef = ref(null)
const triggerRef = ref(null)
const popoverWidth = ref(480)
const selected = computed(() =>
  props.options.filter(option =>
    props.multiple ? props.modelValue.includes(option.value) : props.modelValue === option.value
  )
)
const pendingLetters = computed(() =>
  props.options
    .filter(option =>
      props.multiple ? pending.value.includes(option.value) : pending.value === option.value
    )
    .map(option => option.letter)
    .join('、')
)

function handleOpen() {
  if (props.disabled) return
  pending.value = props.multiple ? [...props.modelValue] : props.modelValue
  popoverWidth.value = Math.min(props.multiple ? 620 : 300, window.innerWidth - 32)
  visible.value = !visible.value
}
function handleCancel() {
  visible.value = false
}
function handleConfirm() {
  emit('update:modelValue', props.multiple ? [...pending.value] : pending.value)
  visible.value = false
}
function handleFocus() {
  panelRef.value?.querySelector('input')?.focus()
}
function handleOutside(event) {
  if (!panelRef.value?.contains(event.target) && !triggerRef.value?.contains(event.target))
    visible.value = false
}
watch(
  () => props.disabled,
  () => {
    visible.value = false
  }
)
onMounted(() => document.addEventListener('pointerdown', handleOutside))
onUnmounted(() => document.removeEventListener('pointerdown', handleOutside))
</script>

<style scoped>
.answer-line {
  display: inline-block;
  width: 100%;
  min-height: 1.8em;
  border: 0;
  border-bottom: 1.5px solid #ff454f;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  padding: 0.15em 0.3em;
}
.answer-line:hover:not(:disabled) {
  background-color: #f3faf9;
}
.answer-line:focus-visible {
  outline: 2px solid #078f88;
  outline-offset: 3px;
}
.answer-line:disabled {
  cursor: default;
}
.answer-line.invalid {
  background: #fff1ed;
}
.empty-answer {
  opacity: 0;
  color: #8596a7;
  font-size: 0.75em;
}
.answer-text > span {
  display: block;
}
.answer-panel {
  font-family: 'SimSun', '宋体', serif;
  color: #163f65;
}
.option-list {
  max-height: 48vh;
  overflow-y: auto;
}
:deep(.el-checkbox-group),
:deep(.el-radio-group) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
:deep(.el-checkbox),
:deep(.el-radio) {
  height: auto;
  margin: 0;
  padding: 0.5em 0;
  white-space: normal;
  align-items: flex-start;
}
:deep(.el-checkbox__label),
:deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.7;
  font-size: clamp(14px, 1.2vw, 18px);
}
:deep(.el-checkbox__input),
:deep(.el-radio__input) {
  margin-top: 0.35em;
}
.answer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #d6e5e5;
}
.answer-actions > span {
  margin-right: auto;
}
</style>
