<!-- 左工具栏：围栏/告示牌 4 个放置物品（纯展示组件，状态由父组件驱动） -->
<template>
  <div class="tool-bar tool-bar-left">
    <div
      v-for="(item, idx) in items"
      :key="'L' + idx"
      class="tool-item tool-item-img"
      :class="{ 'tool-selected': activeIdx === idx, 'tool-placed': placedMask[idx] }"
      @click="emit('select', idx, $event)"
    >
      <img :src="item.img" alt="" draggable="false" />
    </div>
  </div>
</template>

<script setup>
import '@/assets/styles/toolbar.css'

defineProps({
  items: { type: Array, required: true },
  placedMask: { type: Array, required: true },
  activeIdx: { type: Number, default: null }
})
const emit = defineEmits(['select'])
</script>
