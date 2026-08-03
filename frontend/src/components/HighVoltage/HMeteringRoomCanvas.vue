<!-- 计量小室操作画布（Leafer UI）：背景层 + 交互层，挂电表（步骤5） -->
<template>
  <div class="metering-canvas-wrap">
    <div class="canvas-stage">
      <img ref="bgImgRef" :src="currentBg" alt="计量小室" class="bg-img" draggable="false" />
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
      <!-- 鼠标跟随（Vue HTML 层，不进入画布） -->
      <div v-if="isMeterFollowing" class="meter-following" :style="followStyle">
        <img :src="Images.barThreePhaseThreeWireMeter" alt="电表" draggable="false" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Leafer, Group, Image, Rect, PointerEvent } from 'leafer-ui'
import Images from '@/constants/images'

const props = defineProps({
  stepOrder: { type: Number, required: true }
})
const emit = defineEmits(['operation', 'error', 'stepCompleted'])

// ─── 状态 ───
const currentBg = ref(Images.meteringRoomNoMeter)
const meterPlaced = ref(false)
const isMeterFollowing = ref(false)
const followStyle = ref({})
const canvasStyle = ref({})
const switchStates = ref([])
const bgImgRef = ref(null)
const leaferViewRef = ref(null)

// ─── Leafer 实例与层 ───
let leafer = null
let bgLayer = null
let hitLayer = null

// ★ 挂表区域热区（图片左上方约 1/4 区域，相对画布宽高的比率，用户按背景图微调）
const DROP_ZONE = { x: 0.08, y: 0.08, w: 0.28, h: 0.3 }

// ★ 接线盒开关（10 个：4 竖 + 3 组双横）
//   orient: v=竖(顺时针90°), hU=上排横(0°), hD=下排横(180°)
//   target: 目标状态（1/4/5 断开 off、2/3/6/7 闭合 on，8/9/10 待用户补充）
//   x/y: 相对画布宽高的比率（占位坐标，用户按背景图微调）
const SWITCHES = [
  { orient: 'v', target: 'off', x: 0.22, y: 0.38 },
  { orient: 'v', target: 'on', x: 0.22, y: 0.54 },
  { orient: 'v', target: 'on', x: 0.22, y: 0.7 },
  { orient: 'v', target: 'off', x: 0.22, y: 0.86 },
  { orient: 'hU', target: 'off', x: 0.45, y: 0.42 },
  { orient: 'hD', target: 'on', x: 0.45, y: 0.58 },
  { orient: 'hU', target: 'on', x: 0.62, y: 0.42 },
  { orient: 'hD', target: 'on', x: 0.62, y: 0.58 },
  { orient: 'hU', target: 'on', x: 0.79, y: 0.42 },
  { orient: 'hD', target: 'on', x: 0.79, y: 0.58 }
]
// 开关图尺寸（相对画布宽高的比率）与 on 状态横向位移
const SWITCH_SIZE = { w: 0.07, h: 0.045 }
const SW_OFFSET = 0.012

const switchRefs = []
let canvasW = 0
let switchesCompleted = false
let pendingDraft = null

function switchBackground(url) {
  currentBg.value = url
  if (!leafer) return
  const w = leafer.width
  const h = leafer.height
  bgLayer.removeAll()
  bgLayer.add(new Image({ url, x: 0, y: 0, width: w, height: h }))
}

function bindEvents(w, h) {
  leafer.on(PointerEvent.CLICK, e => {
    const p = e.getLocal()
    const dz = {
      x: w * DROP_ZONE.x,
      y: h * DROP_ZONE.y,
      w: w * DROP_ZONE.w,
      h: h * DROP_ZONE.h
    }
    if (p.x >= dz.x && p.x <= dz.x + dz.w && p.y >= dz.y && p.y <= dz.y + dz.h) {
      handleDrop()
    } else {
      handleMiss()
    }
  })
}

/** 点击挂表热区（仅步骤5） */
function handleDrop() {
  if (props.stepOrder !== 5) return
  if (!isMeterFollowing.value) {
    ElMessage.warning('请先在右侧工具栏选择电表')
    emit('error')
    return
  }
  isMeterFollowing.value = false
  meterPlaced.value = true
  switchBackground(Images.meteringRoomWithMeter)
  emit('stepCompleted', props.stepOrder)
}

/** 点击热区以外（仅步骤5） */
function handleMiss() {
  if (props.stepOrder !== 5) return
  if (!isMeterFollowing.value) return
  ElMessage.warning('请选择正确的放置位置')
  emit('error')
  isMeterFollowing.value = false
}

// ─── 接线盒开关（步骤6） ───

/** 构建 10 个开关热区与小图（横开关宽 SW_SIZE.w、高 SW_SIZE.h，竖开关旋转90°） */
function buildSwitches(w, h) {
  switchRefs.length = 0
  switchStates.value = []
  SWITCHES.forEach((cfg, i) => {
    const x = w * cfg.x
    const y = h * cfg.y
    const sw = w * SWITCH_SIZE.w
    const sh = h * SWITCH_SIZE.h
    const rotation = cfg.orient === 'v' ? 90 : cfg.orient === 'hD' ? 180 : 0
    const img = new Image({
      url: Images.junctionBoxSwitch,
      x,
      y,
      width: sw,
      height: sh,
      rotation,
      zIndex: 2
    })
    hitLayer.add(img)
    const rect = new Rect({
      x: x - sw * 0.25,
      y: y - sh * 0.3,
      width: sw * 1.5,
      height: sh * 1.6,
      fill: 'rgba(0,0,0,0)'
    })
    rect.on(PointerEvent.CLICK, () => toggleSwitch(i))
    hitLayer.add(rect)
    switchRefs.push({ cfg, img, baseX: x })
    switchStates.value.push('off')
  })
}

function toggleSwitch(i) {
  emit('operation')
  const s = switchRefs[i]
  if (!s) return
  const cur = switchStates.value[i]
  const next = cur === 'on' ? 'off' : 'on'
  switchStates.value[i] = next
  s.img.x = s.baseX + (next === 'on' ? canvasW * SW_OFFSET : 0)
  checkSwitches()
}

/** 全部开关达到目标状态 → 提交步骤6 */
function checkSwitches() {
  if (switchesCompleted) return
  const allOk = SWITCHES.every((cfg, i) => switchStates.value[i] === cfg.target)
  if (allOk) {
    switchesCompleted = true
    emit('stepCompleted', props.stepOrder)
  }
}

/** 恢复开关状态（位置/视觉） */
function applyDraft(d) {
  if (!Array.isArray(d?.switchStates)) return
  d.switchStates.forEach((v, i) => {
    if (!switchRefs[i]) return
    switchStates.value[i] = v
    switchRefs[i].img.x = switchRefs[i].baseX + (v === 'on' ? leafer.width * SW_OFFSET : 0)
  })
}

function createCanvas() {
  const img = bgImgRef.value
  if (!img) return
  const r = img.getBoundingClientRect()
  const w = Math.round(r.width)
  const h = Math.round(r.height)
  canvasW = w
  canvasStyle.value = { width: w + 'px', height: h + 'px' }
  leafer = new Leafer({ view: leaferViewRef.value, width: w, height: h })
  bgLayer = new Group()
  hitLayer = new Group()
  leafer.add(bgLayer)
  leafer.add(hitLayer)
  bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
  bindEvents(w, h)
  if (props.stepOrder === 6) buildSwitches(w, h)
  if (pendingDraft) {
    applyDraft(pendingDraft)
    pendingDraft = null
  }
}

// ─── 供父组件调用的方法 ───

/** 右侧工具栏点击：智能电表 → 启动跟随，其余工具占位提示 */
function onRightToolClick(idx, e) {
  if (idx !== 0) {
    emit('operation')
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  emit('operation')
  if (props.stepOrder !== 5) {
    ElMessage.warning('当前步骤无需挂表')
    emit('error')
    return
  }
  if (meterPlaced.value) {
    ElMessage.warning('电表已挂载')
    emit('error')
    return
  }
  isMeterFollowing.value = true
  followStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
}

function onPageMouseMove(e) {
  if (isMeterFollowing.value) {
    followStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  }
}

function getDraftState() {
  return {
    meterPlaced: meterPlaced.value,
    switchStates: [...switchStates.value],
    stepOrder: props.stepOrder
  }
}

function restoreDraft(d) {
  if (d?.meterPlaced) {
    meterPlaced.value = true
    switchBackground(Images.meteringRoomWithMeter)
  }
  if (props.stepOrder === 6) {
    if (switchRefs.length > 0) {
      applyDraft(d)
    } else {
      pendingDraft = d
    }
  }
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
    canvasW = w
    canvasStyle.value = { width: w + 'px', height: h + 'px' }
    leafer.resize(w, h)
    // 背景图随画布尺寸重铺
    bgLayer.removeAll()
    bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
    // 步骤6：按当前状态重建开关（保持视觉位置）
    if (props.stepOrder === 6) {
      const saved = [...switchStates.value]
      hitLayer.removeAll()
      buildSwitches(w, h)
      saved.forEach((v, i) => {
        if (switchRefs[i]) {
          switchStates.value[i] = v
          switchRefs[i].img.x = switchRefs[i].baseX + (v === 'on' ? w * SW_OFFSET : 0)
        }
      })
    }
  }, 200)
}

onMounted(() => {
  const img = bgImgRef.value
  if (!img) return
  if (img.complete) {
    createCanvas()
  } else {
    img.addEventListener('load', createCanvas, { once: true })
  }
  window.addEventListener('resize', onResize)
})
// 同组件导航（步骤5→6）时组件不重新挂载，需监听步骤变化构建开关
watch(
  () => props.stepOrder,
  order => {
    if (order === 6 && leafer && switchRefs.length === 0) {
      buildSwitches(leafer.width, leafer.height)
      if (pendingDraft) {
        applyDraft(pendingDraft)
        pendingDraft = null
      }
    }
  }
)
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  leafer?.destroy()
})

defineExpose({
  onRightToolClick,
  onPageMouseMove,
  getDraftState,
  restoreDraft
})
</script>

<style scoped>
.metering-canvas-wrap {
  position: fixed;
  left: 12vw;
  right: 12vw;
  top: 5vh;
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
  max-width: 76vw;
  max-height: 90vh;
  display: block;
  user-select: none;
}

.leafer-view {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

.meter-following {
  position: fixed;
  z-index: 999;
  pointer-events: none;
  width: 5vw;
  min-width: 40px;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.85;
  transform: translate(-50%, -50%);
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.5));
}

.meter-following img {
  width: 90%;
  height: 90%;
  object-fit: contain;
}
</style>
