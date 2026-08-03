<!-- 右工具栏：21 个工具（纯展示组件，验电笔高亮由父组件驱动） -->
<template>
  <div class="tool-bar tool-bar-right">
    <div
      v-for="(tool, idx) in items"
      :key="'R' + idx"
      class="tool-item tool-item-img"
      :class="{
        'tool-selected': activeIdxs.includes(idx) || (idx === 4 && vtActive),
        'tool-placed': idx === 4 && vtDone
      }"
      @click="emit('click', idx, $event)"
    >
      <img :src="tool.img" :alt="tool.name" draggable="false" />
    </div>
  </div>
</template>

<script setup>
import '@/assets/styles/toolbar.css'

defineProps({
  items: { type: Array, required: true },
  vtActive: { type: Boolean, default: false },
  vtDone: { type: Boolean, default: false },
  activeIdxs: { type: Array, default: () => [] }
})
const emit = defineEmits(['click'])
</script>
