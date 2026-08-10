<!-- 终端小室操作画布（步骤13+，Leafer UI）：流程与计量小室一致，尺寸一致，背景图暂共用 -->
<template>
  <div class="terminal-canvas-wrap">
    <div class="canvas-stage">
      <img ref="bgImgRef" :src="currentBg" alt="终端小室" class="bg-img" draggable="false" />
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Leafer, Group, Image } from 'leafer-ui'
import Images from '@/constants/images'

const props = defineProps({
  stepOrder: { type: Number, required: true },
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' }
})
const emit = defineEmits(['operation', 'error', 'stepCompleted'])

// ─── 状态 ───
// 终端小室背景图（步骤 13 起逐步替换为对应状态图）
const currentBg = ref(Images.terminalRoomNoMeter)
const canvasStyle = ref({})
const bgImgRef = ref(null)
const leaferViewRef = ref(null)

// ─── Leafer 实例与层 ───
let leafer = null
let bgLayer = null
let hitLayer = null

async function createCanvas() {
  const img = bgImgRef.value
  if (!img) return
  const r = img.getBoundingClientRect()
  const w = Math.round(r.width)
  const h = Math.round(r.height)
  canvasStyle.value = { width: w + 'px', height: h + 'px' }
  leafer = new Leafer({ view: leaferViewRef.value, width: w, height: h })
  bgLayer = new Group()
  hitLayer = new Group()
  leafer.add(bgLayer)
  leafer.add(hitLayer)
  bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
}

// ─── 生命周期 ───
let resizeTimer = null
function onResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    const img = bgImgRef.value
    if (!img || !leafer) return
    const r = img.getBoundingClientRect()
    const w = Math.round(r.width)
    const h = Math.round(r.height)
    canvasStyle.value = { width: w + 'px', height: h + 'px' }
    leafer.resize({ width: w, height: h })
    bgLayer.removeAll()
    bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
  }, 200)
}

onMounted(() => {
  const img = bgImgRef.value
  if (!img) return
  if (img.complete) createCanvas()
  else img.addEventListener('load', createCanvas, { once: true })
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  leafer?.destroy()
})

defineExpose({})
</script>

<style scoped>
/* 与计量小室画布尺寸一致 */
.terminal-canvas-wrap {
  position: fixed;
  left: 12vw;
  right: 12vw;
  top: 14vh;
  bottom: 5vh;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.canvas-stage {
  position: relative;
  display: inline-block;
  max-width: 100%;
  max-height: 100%;
}

.bg-img {
  max-width: 66vw;
  max-height: 76vh;
  display: block;
  user-select: none;
}

.leafer-view {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}
</style>
