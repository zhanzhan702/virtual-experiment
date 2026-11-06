<!-- 右工具栏：工具列表（纯展示组件，验电笔高亮由父组件驱动） -->
<template>
  <div class="tool-bar tool-bar-right">
    <div
      v-for="(tool, idx) in items"
      :key="'R' + idx"
      class="tool-item tool-item-img"
      :class="{
        'tool-selected': activeIdxs.includes(idx) || (idx === vtIdx && vtActive),
        'tool-placed': idx === vtIdx && vtDone
      }"
      @click="emit('click', idx, $event)"
    >
      <img
        :src="tool.img"
        :alt="tool.name"
        :style="tool.imgWidth ? { width: tool.imgWidth, height: tool.imgWidth } : undefined"
        draggable="false"
      />
    </div>
  </div>
</template>

<script setup>
import '@/assets/styles/toolbar.css'

defineProps({
  items: { type: Array, required: true },
  vtActive: { type: Boolean, default: false },
  vtDone: { type: Boolean, default: false },
  activeIdxs: { type: Array, default: () => [] },
  /** 验电笔在 items 里的下标。高压是 4；低压场景只有一个工具，传 0 */
  vtIdx: { type: Number, default: 4 }
})
const emit = defineEmits(['click'])
</script>
