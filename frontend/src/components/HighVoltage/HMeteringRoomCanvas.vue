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

// ★ 挂表区域热区（相对画布宽高的比率，用户按背景图微调；带颜色便于调整定位）
const DROP_ZONE = { x: 0.15, y: 0.04, w: 0.37, h: 0.53 }

// ★ 接线盒（左下角贴底，宽固定为画布宽的 1/2，高度按图片比例 auto，用户按背景图微调）
//   步骤5（未挂表）起显示，直到背景图切换为盖盖子的计量小室后隐藏
const JUNCTION_BOX = { x: 0.2, y: 0.7, w: 0.28 }

// ★ 接线盒开关（10 个，一行排列：竖 双横 竖 双横 竖 双横 竖，竖作为双横的间隔）
//   orient: v=竖(顺时针90°), hU=上排横(0°), hD=下排横(180°)
//   target: 目标状态（按数组顺序 1/4/5 断开 off、2/3/6/7 闭合 on，8/9/10 待用户补充）
//   x/y: 相对接线盒左上角的比率（0~1，用户按背景图微调）
const SWITCHES = [
  { orient: 'v', target: 'off', x: 0.04, y: 0.5 },
  { orient: 'hU', target: 'on', x: 0.2, y: 0.4 },
  { orient: 'hD', target: 'on', x: 0.2, y: 0.6 },
  { orient: 'v', target: 'off', x: 0.36, y: 0.5 },
  { orient: 'hU', target: 'off', x: 0.52, y: 0.4 },
  { orient: 'hD', target: 'on', x: 0.52, y: 0.6 },
  { orient: 'v', target: 'on', x: 0.68, y: 0.5 },
  { orient: 'hU', target: 'on', x: 0.84, y: 0.4 },
  { orient: 'hD', target: 'on', x: 0.84, y: 0.6 },
  { orient: 'v', target: 'on', x: 0.97, y: 0.5 }
]
// 开关图宽（相对接线盒宽度的比率），高度按图片比例 auto（不压缩）
const SWITCH_SIZE = { w: 0.07 }
// 切换位移：未旋转横开关向右、倒置横开关向左、竖开关向下（相对开关自身宽度）
// 初始状态为闭合（on，基准位）；点击后向对应方向移动切换为断开（off）
const SW_DIR = { hU: { dx: 1, dy: 0 }, hD: { dx: -1, dy: 0 }, v: { dx: 0, dy: 1 } }
const SW_OFFSET_RATIO = 0.6

const switchRefs = []
let junctionBoxImg = null
let junctionBoxRect = { x: 0, y: 0, w: 0, h: 0 }
let dropZoneRect = null
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
    const p = e.getLocalPoint()
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

// ─── 接线盒（步骤5 起显示，中层级）与开关（步骤6，最顶层） ───
// 层级：背景图(bgLayer 最底) < 接线盒(zIndex 1) < 开关(zIndex 2) < 热区(zIndex 3)

/** 挂表区域热区可视化（带颜色，便于调整定位） */
function buildDropZone(w, h) {
  dropZoneRect = new Rect({
    x: w * DROP_ZONE.x,
    y: h * DROP_ZONE.y,
    width: w * DROP_ZONE.w,
    height: h * DROP_ZONE.h,
    fill: 'rgba(0, 150, 255, 0.25)',
    stroke: 'rgba(0, 150, 255, 0.9)',
    strokeWidth: 2,
    zIndex: 0
  })
  hitLayer.add(dropZoneRect)
}

/** 构建接线盒（位置由 JUNCTION_BOX.x/y 指定，宽固定画布比例，高度按图片比例 auto） */
function buildJunctionBox(w, h) {
  const boxW = w * JUNCTION_BOX.w
  const boxH = boxW / (junctionBoxAspect || 2)
  const boxY = JUNCTION_BOX.y != null ? h * JUNCTION_BOX.y : h - boxH
  junctionBoxRect = { x: w * JUNCTION_BOX.x, y: boxY, w: boxW, h: boxH }
  junctionBoxImg = new Image({
    url: Images.junctionBox,
    x: junctionBoxRect.x,
    y: junctionBoxRect.y,
    width: boxW,
    height: boxH,
    zIndex: 1
  })
  hitLayer.add(junctionBoxImg)
  // 图片比例加载完成后校正高度（初始用默认比例 2:1 立即显示）
  loadJunctionBoxAspect().then(() => {
    if (junctionBoxImg) {
      junctionBoxImg.height = junctionBoxImg.width / junctionBoxAspect
      junctionBoxRect = {
        x: junctionBoxImg.x,
        y: junctionBoxImg.y,
        w: junctionBoxImg.width,
        h: junctionBoxImg.height
      }
      ensureSwitches()
    }
  })
}

/** 构建 10 个开关热区/小图（开关在接线盒上层，初始状态闭合 on，位置相对接线盒） */
function buildSwitches() {
  switchRefs.length = 0
  switchStates.value = []
  const { x: bx, y: by, w: bw, h: bh } = junctionBoxRect
  SWITCHES.forEach((cfg, i) => {
    const x = bx + cfg.x * bw
    const y = by + cfg.y * bh
    const sw = bw * SWITCH_SIZE.w
    const sh = sw / (switchAspect || 1.6)
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
    // 热区与开关图位置尺寸一致并同步旋转（zIndex 3），蓝色半透明便于调整定位
    const rect = new Rect({
      x,
      y,
      width: sw,
      height: sh,
      fill: 'rgba(0, 150, 255, 0.25)',
      rotation,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => toggleSwitch(i))
    hitLayer.add(rect)
    switchRefs.push({ cfg, img, rect, baseX: x, baseY: y, sw })
    switchStates.value.push('on')
  })
  // 开关图片比例加载完成后校正高度（不压缩比例，热区同步）
  loadSwitchAspect().then(() => {
    switchRefs.forEach(s => {
      if (s.img) s.img.height = s.img.width / switchAspect
      if (s.rect) s.rect.height = s.rect.width / switchAspect
    })
  })
}

/** 加载开关图片宽高比（用于高度 auto 计算） */
let switchAspect = null
function loadSwitchAspect() {
  return new Promise(resolve => {
    if (switchAspect) return resolve(switchAspect)
    const img = new Image()
    img.onload = () => {
      switchAspect = img.naturalWidth / img.naturalHeight
      resolve(switchAspect)
    }
    img.src = Images.junctionBoxSwitch
  })
}

/** 步骤5+ 且接线盒就绪时构建开关（首次构建，保留草稿恢复） */
function ensureSwitches() {
  if (props.stepOrder >= 5 && switchRefs.length === 0 && junctionBoxRect.w > 0) {
    buildSwitches()
    if (pendingDraft) {
      applyDraft(pendingDraft)
      pendingDraft = null
    }
  }
}

/** 开关位置：闭合 on 在基准位，断开 off 按方向位移（hU 右 / hD 左 / v 下） */
function switchPos(s, state) {
  if (state === 'on') return { x: s.baseX, y: s.baseY }
  const dir = SW_DIR[s.cfg.orient]
  return {
    x: s.baseX + dir.dx * s.sw * SW_OFFSET_RATIO,
    y: s.baseY + dir.dy * s.sw * SW_OFFSET_RATIO
  }
}

/** 加载接线盒图片宽高比（用于高度 auto 计算） */
let junctionBoxAspect = null
function loadJunctionBoxAspect() {
  return new Promise(resolve => {
    if (junctionBoxAspect) return resolve(junctionBoxAspect)
    const img = new Image()
    img.onload = () => {
      junctionBoxAspect = img.naturalWidth / img.naturalHeight
      resolve(junctionBoxAspect)
    }
    img.src = Images.junctionBox
  })
}

function toggleSwitch(i) {
  emit('operation')
  const s = switchRefs[i]
  if (!s) return
  const cur = switchStates.value[i]
  const next = cur === 'on' ? 'off' : 'on'
  switchStates.value[i] = next
  const p = switchPos(s, next)
  s.img.x = p.x
  s.img.y = p.y
  checkSwitches()
}

/** 全部开关达到目标状态 → 提交（仅步骤6） */
function checkSwitches() {
  if (props.stepOrder !== 6) return
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
    const p = switchPos(switchRefs[i], v)
    switchRefs[i].img.x = p.x
    switchRefs[i].img.y = p.y
  })
}

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
  bindEvents(w, h)
  buildDropZone(w, h)
  buildJunctionBox(w, h)
  ensureSwitches()
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
    canvasStyle.value = { width: w + 'px', height: h + 'px' }
    leafer.resize({ width: w, height: h })
    // 背景图随画布尺寸重铺
    bgLayer.removeAll()
    bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
    // 步骤5+：重建热区/接线盒/开关（按当前状态保持视觉位置）
    if (props.stepOrder >= 5) {
      const saved = [...switchStates.value]
      hitLayer.removeAll()
      buildDropZone(w, h)
      buildJunctionBox(w, h)
      ensureSwitches()
      saved.forEach((v, i) => {
        if (switchRefs[i]) {
          switchStates.value[i] = v
          const p = switchPos(switchRefs[i], v)
          switchRefs[i].img.x = p.x
          switchRefs[i].img.y = p.y
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
// 同组件导航时组件不重新挂载，需监听步骤变化构建开关
watch(
  () => props.stepOrder,
  order => {
    if (order >= 5 && leafer) ensureSwitches()
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
