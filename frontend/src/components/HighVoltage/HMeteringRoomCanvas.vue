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
      <div v-if="screwdriverFollowing" class="meter-following" :style="screwdriverStyle">
        <img :src="Images.barCrossScrewdriver" alt="螺丝刀" draggable="false" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Leafer, Group, Image, Rect, Path, PointerEvent } from 'leafer-ui'
import Images from '@/constants/images'
import { getStepDraft } from '@/api/experiment'

const props = defineProps({
  stepOrder: { type: Number, required: true },
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' }
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
// 步骤7 接线状态机
const wiringStep = ref('idle') // idle | plier_selected | wire_selected | screwdriver_active | wire_drawing
const selectedWire = ref(null)
const wireStart = ref(null) // { type: 'box' | 'meter', hole: n }
const wireStartPos = ref(null)
const connectedWires = ref([]) // [{ boxHole, meterHole, spec }]
const activeToolIdxs = ref([]) // 剥线钳+当前导线持续高亮，螺丝刀不高亮（仅跟随）
const screwdriverFollowing = ref(false)
const screwdriverStyle = ref({})

// 步骤6+ 必然已挂表：初始化已挂表背景（不依赖草稿，刷新/重挂载均正确）
if (props.stepOrder >= 6) {
  meterPlaced.value = true
  currentBg.value = Images.meteringRoomWithMeter
}

// ─── Leafer 实例与层 ───
let leafer = null
let bgLayer = null
let hitLayer = null

// ★ 挂表区域热区（相对画布宽高的比率，用户按背景图微调；带颜色便于调整定位）
const DROP_ZONE = { x: 0.15, y: 0.04, w: 0.37, h: 0.53 }

// ★ 接线盒（左下角贴底，宽固定为画布宽的 1/2，高度按图片比例 auto，用户按背景图微调）
//   步骤5（未挂表）起显示，直到背景图切换为盖盖子的计量小室后隐藏
const JUNCTION_BOX = { x: 0.2, y: 0.742, w: 0.27 }

// ★ 接线盒开关（10 个，一行排列：竖 双横 竖 双横 竖 双横 竖，竖作为双横的间隔）
//   orient: v=竖(顺时针90°), hU=上排横(0°), hD=下排横(180°)
//   target: 目标状态（按数组顺序 1/4/5 断开 off、2/3/6/7 闭合 on，8/9/10 待用户补充）
//   on/off: 两个状态各自独立的坐标（相对接线盒左上角的比率 0~1），切换时直接定位173
const SWITCHES = [
  { orient: 'v', target: 'off', on: { x: 0.143, y: 0.41 }, off: { x: 0.143, y: 0.5 } },
  { orient: 'hU', target: 'on', on: { x: 0.173, y: 0.4 }, off: { x: 0.225, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.314, y: 0.66 }, off: { x: 0.26, y: 0.66 } },
  { orient: 'v', target: 'off', on: { x: 0.394, y: 0.41 }, off: { x: 0.394, y: 0.5 } },
  { orient: 'hU', target: 'on', on: { x: 0.433, y: 0.4 }, off: { x: 0.485, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.574, y: 0.66 }, off: { x: 0.52, y: 0.66 } },
  { orient: 'v', target: 'off', on: { x: 0.654, y: 0.41 }, off: { x: 0.654, y: 0.5 } },
  { orient: 'hU', target: 'on', on: { x: 0.688, y: 0.4 }, off: { x: 0.74, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.829, y: 0.66 }, off: { x: 0.775, y: 0.66 } },
  { orient: 'v', target: 'on', on: { x: 0.907, y: 0.41 }, off: { x: 0.907, y: 0.5 } }
]
// 开关图宽（相对接线盒宽度的比率），高度按图片比例 auto（不压缩）
const SWITCH_SIZE = { w: 0.1 }
// 图片宽高比兜底值（当前资源实际比例：JunctionBox 1272×505、JunctionBoxSwitch 251×119）
// 图片更换时需同步更新；运行时优先加载图片真实比例
const JUNCTION_BOX_ASPECT = 2.519
const SWITCH_ASPECT = 2.109

// ★ 接线盒顶部 13 孔（相对接线盒比率），编号从右往左：孔1 最右、孔13 最左
//   两端对齐最左/最右竖开关（SWITCHES[0].on.x=0.13 → SWITCHES[9].on.x=0.866），y 在盒顶部
const BOX_HOLES = { count: 13, x0: 0.13, x1: 0.866, y: 0.03, size: 0.05 }

// ★ 电表底部 9 孔（相对画布比率）：3 组等腰三角形水平排列在挂表热区底部中间
//   编号从右往左：右组=孔1-3、中组=孔4-6、左组=孔7-9；组内 k=1 左底、k=2 顶点(上)、k=3 右底
const METER_HOLES = {
  cx: 0.31,
  spanW: 0.183,
  y0: 0.535,
  triH: 0.007,
  size: 0.015
}

// ★ 7 根导线固定配对（接线顺序不限）；双色线（红黑/黄黑）用两条半宽线并排模拟
const WIRE_CONNECTIONS = [
  { spec: '4.0红黑', boxHole: 3, meterHole: 3, pathColor: '#d40000', secondColor: '#000000' },
  { spec: '4.0红', boxHole: 4, meterHole: 1, pathColor: '#e60000' },
  { spec: '2.5红', boxHole: 5, meterHole: 2, pathColor: '#e60000' },
  { spec: '2.5绿', boxHole: 9, meterHole: 5, pathColor: '#00a650' },
  { spec: '4.0黄黑', boxHole: 11, meterHole: 9, pathColor: '#f0a500', secondColor: '#000000' },
  { spec: '4.0黄', boxHole: 12, meterHole: 7, pathColor: '#f0a500' },
  { spec: '2.5黄', boxHole: 13, meterHole: 8, pathColor: '#f0a500' }
]
// 右栏工具索引 → 导线配对（rightTools 顺序：6=2.5黄 7=2.5绿 8=2.5红 9=4.0黄 10=4.0黄黑 11=4.0红 12=4.0红黑）
const TOOL_IDX_TO_WIRE = {
  6: WIRE_CONNECTIONS[6],
  7: WIRE_CONNECTIONS[3],
  8: WIRE_CONNECTIONS[2],
  9: WIRE_CONNECTIONS[5],
  10: WIRE_CONNECTIONS[4],
  11: WIRE_CONNECTIONS[1],
  12: WIRE_CONNECTIONS[0]
}

const switchRefs = []
let junctionBoxImg = null
let junctionBoxRect = { x: 0, y: 0, w: 0, h: 0 }
let dropZoneRect = null
let switchesCompleted = false
let wiresCompleted = false
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
  // 挂表完成后移除挂表热区
  dropZoneRect?.remove()
  dropZoneRect = null
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

/** 挂表区域热区可视化（带颜色，便于调整定位；挂表完成后不再显示） */
function buildDropZone(w, h) {
  if (meterPlaced.value) return
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
  const boxH = boxW / (junctionBoxAspect || JUNCTION_BOX_ASPECT)
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
  // 先用默认比例立即构建开关/孔热区（保证任何情况都显示）
  ensureSwitches()
  ensureHoles()
  // 图片比例加载完成后校正接线盒高度，并重建开关/孔热区/导线到最终位置（保留状态）
  loadJunctionBoxAspect().then(() => {
    if (junctionBoxImg) {
      junctionBoxImg.height = junctionBoxImg.width / junctionBoxAspect
      junctionBoxRect = {
        x: junctionBoxImg.x,
        y: junctionBoxImg.y,
        w: junctionBoxImg.width,
        h: junctionBoxImg.height
      }
      rebuildSwitches()
      rebuildHoles()
      redrawConnectedWires()
    }
  })
}

/** 接线盒比例校正后重建开关（保留当前状态），未构建时直接构建 */
function rebuildSwitches() {
  if (switchRefs.length === 0) {
    ensureSwitches()
    return
  }
  const saved = [...switchStates.value]
  switchRefs.forEach(s => {
    s.img.remove()
    s.rect?.remove()
  })
  switchRefs.length = 0
  buildSwitches()
  saved.forEach((v, i) => {
    if (switchRefs[i]) {
      switchStates.value[i] = v
      moveSwitch(switchRefs[i], v)
    }
  })
}

/** 构建 10 个开关热区/小图（开关在接线盒上层，初始状态闭合 on，位置相对接线盒） */
function buildSwitches() {
  switchRefs.length = 0
  switchStates.value = []
  const { x: bx, y: by, w: bw, h: bh } = junctionBoxRect
  SWITCHES.forEach((cfg, i) => {
    // 初始状态为闭合（on），用 on 坐标定位
    const x = bx + cfg.on.x * bw
    const y = by + cfg.on.y * bh
    const sw = bw * SWITCH_SIZE.w
    const sh = sw / (switchAspect || SWITCH_ASPECT)
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
    // 热区与开关图位置尺寸一致并同步旋转（zIndex 3），蓝色半透明便于调整定位；仅步骤6 可交互
    let rect = null
    if (props.stepOrder === 6) {
      rect = new Rect({
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
    }
    switchRefs.push({ cfg, img, rect, sw })
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
    img.onerror = () => resolve(SWITCH_ASPECT)
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

// ─── 步骤7：接线孔热区与导线 ───
const boxHoleRects = []
const meterHoleRects = []
const wirePaths = []
let wireFollowPaths = []

/** 接线盒孔 i（1~13，右起）绝对坐标 */
function boxHolePos(i) {
  const rx =
    BOX_HOLES.x0 + ((BOX_HOLES.count - i) / (BOX_HOLES.count - 1)) * (BOX_HOLES.x1 - BOX_HOLES.x0)
  return {
    x: junctionBoxRect.x + rx * junctionBoxRect.w,
    y: junctionBoxRect.y + BOX_HOLES.y * junctionBoxRect.h
  }
}

/** 电表孔 j（1~9，右起）画布比率坐标 */
function meterHoleRatio(j) {
  const g = Math.ceil(j / 3) // 1=右组 2=中组 3=左组
  const k = ((j - 1) % 3) + 1 // 组内 1=左底 2=顶点 3=右底
  const triW = METER_HOLES.spanW / 3
  const gc = METER_HOLES.cx + (g === 1 ? triW : g === 2 ? 0 : -triW)
  const half = triW * 0.3
  if (k === 1) return { x: gc - half, y: METER_HOLES.y0 }
  if (k === 2) return { x: gc, y: METER_HOLES.y0 - METER_HOLES.triH }
  return { x: gc + half, y: METER_HOLES.y0 }
}

/** 步骤7：接线盒就绪后构建孔热区（接线盒构建完成后即可） */
function ensureHoles() {
  if (props.stepOrder !== 7 || junctionBoxRect.w <= 0 || !leafer) return
  buildBoxHoles()
  buildMeterHoles()
}

function buildBoxHoles() {
  if (boxHoleRects.length > 0) return
  const sz = junctionBoxRect.w * BOX_HOLES.size
  for (let i = 1; i <= BOX_HOLES.count; i++) {
    const p = boxHolePos(i)
    const rect = new Rect({
      x: p.x - sz / 2,
      y: p.y - sz / 2,
      width: sz,
      height: sz,
      fill: 'rgba(0, 150, 255, 0.25)',
      stroke: 'rgba(0, 150, 255, 0.9)',
      strokeWidth: 1,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => onHoleClick('box', i))
    hitLayer.add(rect)
    boxHoleRects.push(rect)
  }
}

function buildMeterHoles() {
  if (meterHoleRects.length > 0) return
  const w = leafer.width
  const h = leafer.height
  const sz = Math.max(w * METER_HOLES.size, 8)
  for (let j = 1; j <= 9; j++) {
    const r = meterHoleRatio(j)
    const rect = new Rect({
      x: r.x * w - sz / 2,
      y: r.y * h - sz / 2,
      width: sz,
      height: sz,
      fill: 'rgba(0, 150, 255, 0.25)',
      stroke: 'rgba(0, 150, 255, 0.9)',
      strokeWidth: 1,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => onHoleClick('meter', j))
    hitLayer.add(rect)
    meterHoleRects.push(rect)
  }
}

/** 接线盒比例校正后重建孔热区 */
function rebuildHoles() {
  boxHoleRects.forEach(r => r.remove())
  boxHoleRects.length = 0
  meterHoleRects.forEach(r => r.remove())
  meterHoleRects.length = 0
  ensureHoles()
}

/** 2.5 细线基准宽 */
function thinWireWidth() {
  return junctionBoxRect.w * 0.006
}

/** 生成导线 Path 列表（单色 1 条；4.0=2 倍细线宽，双色线=两条细线并排拼接） */
function makeWirePaths(from, to, wire) {
  const thin = thinWireWidth()
  const w = wire.spec.startsWith('4.0') ? thin * 2 : thin
  const mk = (x1, y1, x2, y2, color, width) =>
    new Path({
      path: `M ${x1} ${y1} L ${x2} ${y2}`,
      stroke: color,
      strokeWidth: width,
      lineCap: 'round',
      zIndex: 4,
      hittable: false
    })
  if (!wire.secondColor) {
    return [mk(from.x, from.y, to.x, to.y, wire.pathColor, w)]
  }
  const dx = to.x - from.x
  const dy = to.y - from.y
  const len = Math.hypot(dx, dy) || 1
  const nx = -dy / len
  const ny = dx / len
  const off = thin / 2
  return [
    mk(
      from.x + nx * off,
      from.y + ny * off,
      to.x + nx * off,
      to.y + ny * off,
      wire.pathColor,
      thin
    ),
    mk(
      from.x - nx * off,
      from.y - ny * off,
      to.x - nx * off,
      to.y - ny * off,
      wire.secondColor,
      thin
    )
  ]
}

/** 绘制一根导线（持久，zIndex 4 在孔热区之上） */
function drawWirePath(boxHole, meterHole, wire) {
  const from = boxHolePos(boxHole)
  const r = meterHoleRatio(meterHole)
  const to = { x: r.x * leafer.width, y: r.y * leafer.height }
  const paths = makeWirePaths(from, to, wire)
  paths.forEach(p => hitLayer.add(p))
  wirePaths.push(...paths)
}

/** 按已接导线重绘（画布重建/比例校正后恢复视觉） */
function redrawConnectedWires() {
  wirePaths.forEach(p => p.remove())
  wirePaths.length = 0
  connectedWires.value.forEach(w => {
    const conn = WIRE_CONNECTIONS.find(c => c.boxHole === w.boxHole && c.meterHole === w.meterHole)
    if (conn) drawWirePath(w.boxHole, w.meterHole, conn)
  })
}

/** 孔绝对像素坐标（盒孔=接线盒坐标换算；表孔=画布比率换算） */
function holeAbsPos(type, hole) {
  if (type === 'box') return boxHolePos(hole)
  const r = meterHoleRatio(hole)
  return { x: r.x * leafer.width, y: r.y * leafer.height }
}

/** 点孔：screwdriver_active → 记录起点并开始跟随；wire_drawing → 起点终点一起校验 */
function onHoleClick(type, hole) {
  if (props.stepOrder !== 7) return
  if (wiringStep.value === 'screwdriver_active') {
    wireStart.value = { type, hole }
    wireStartPos.value = holeAbsPos(type, hole)
    wiringStep.value = 'wire_drawing'
    const p = wireStartPos.value
    // 跟随线不拦截点击，保证终点孔热区可命中
    wireFollowPaths = makeWirePaths(p, { x: p.x, y: p.y }, selectedWire.value)
    wireFollowPaths.forEach(ph => hitLayer.add(ph))
    leafer.on(PointerEvent.MOVE, onWireMove)
    return
  }
  if (wiringStep.value === 'wire_drawing') {
    const start = wireStart.value
    if (start.type === type) {
      ElMessage.warning('请点击另一端的接线孔')
      emit('error')
      return
    }
    const boxHole = start.type === 'box' ? start.hole : hole
    const meterHole = start.type === 'meter' ? start.hole : hole
    stopWireFollow()
    const w = selectedWire.value
    if (w && boxHole === w.boxHole && meterHole === w.meterHole) {
      drawWirePath(boxHole, meterHole, w)
      connectedWires.value.push({ boxHole, meterHole, spec: w.spec })
      emit('operation')
      resetWiring()
      checkWires()
    } else {
      ElMessage.warning('接线位置错误')
      emit('error')
      resetWiring()
    }
  }
}

function onWireMove(e) {
  if (wireFollowPaths.length === 0 || !wireStartPos.value) return
  const p = e.getLocalPoint()
  const s = wireStartPos.value
  const paths = makeWirePaths(s, { x: p.x, y: p.y }, selectedWire.value)
  wireFollowPaths.forEach((ph, i) => {
    if (paths[i]) ph.path = paths[i].path
  })
}

function stopWireFollow() {
  leafer.off(PointerEvent.MOVE, onWireMove)
  wireFollowPaths.forEach(ph => ph.remove())
  wireFollowPaths = []
}

function resetWiring() {
  wiringStep.value = 'idle'
  selectedWire.value = null
  wireStart.value = null
  wireStartPos.value = null
  activeToolIdxs.value = []
  screwdriverFollowing.value = false
  stopWireFollow()
}

/** 7 根全部接完 → 提交（仅步骤7） */
function checkWires() {
  if (props.stepOrder !== 7) return
  if (wiresCompleted) return
  const allOk = WIRE_CONNECTIONS.every(c =>
    connectedWires.value.some(w => w.boxHole === c.boxHole && w.meterHole === c.meterHole)
  )
  if (allOk) {
    wiresCompleted = true
    emit('stepCompleted', props.stepOrder)
  }
}

/** 开关位置：按状态取 on/off 两套独立坐标（相对接线盒），直接定位 */
function switchPos(s, state) {
  const p = s.cfg[state]
  return {
    x: junctionBoxRect.x + p.x * junctionBoxRect.w,
    y: junctionBoxRect.y + p.y * junctionBoxRect.h
  }
}

/** 开关图与热区同步定位到状态坐标 */
function moveSwitch(s, state) {
  const p = switchPos(s, state)
  s.img.x = p.x
  s.img.y = p.y
  if (s.rect) {
    s.rect.x = p.x
    s.rect.y = p.y
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
    img.onerror = () => resolve(JUNCTION_BOX_ASPECT)
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
  moveSwitch(s, next)
  checkSwitches()
}

/** 全部开关达到目标状态 → 提交（仅步骤6），并移除开关热区（保留开关图，重建时再生成） */
function checkSwitches() {
  if (props.stepOrder !== 6) return
  if (switchesCompleted) return
  const allOk = SWITCHES.every((cfg, i) => switchStates.value[i] === cfg.target)
  if (allOk) {
    switchesCompleted = true
    switchRefs.forEach(s => s.rect?.remove())
    emit('stepCompleted', props.stepOrder)
  }
}

/** 恢复开关状态（位置/视觉）与已接导线 */
function applyDraft(d) {
  if (Array.isArray(d?.switchStates)) {
    d.switchStates.forEach((v, i) => {
      if (!switchRefs[i]) return
      switchStates.value[i] = v
      moveSwitch(switchRefs[i], v)
    })
  }
  if (Array.isArray(d?.connectedWires)) {
    connectedWires.value = d.connectedWires.map(w => ({
      boxHole: w.boxHole,
      meterHole: w.meterHole,
      spec: w.spec
    }))
    redrawConnectedWires()
  }
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
}

// ─── 供父组件调用的方法 ───

/** 步骤7 接线状态机（剥线钳→导线→螺丝刀→孔→孔）；剥线钳与当前导线持续高亮直到接线完成 */
function onWiringToolClick(idx, e) {
  if (idx === 3) {
    // 剥线钳
    if (wiringStep.value !== 'idle' && wiringStep.value !== 'plier_selected') {
      ElMessage.warning('请先完成当前接线')
      emit('error')
      return
    }
    wiringStep.value = 'plier_selected'
    if (!activeToolIdxs.value.includes(idx)) activeToolIdxs.value.push(idx)
    return
  }
  const wire = TOOL_IDX_TO_WIRE[idx]
  if (wire) {
    if (wiringStep.value !== 'plier_selected' && wiringStep.value !== 'wire_selected') {
      ElMessage.warning('请先选择剥线钳')
      emit('error')
      return
    }
    selectedWire.value = wire
    wiringStep.value = 'wire_selected'
    // 只保留剥线钳与当前导线的高亮（换线时移除上一根）
    activeToolIdxs.value = activeToolIdxs.value.filter(i => i !== idx && !TOOL_IDX_TO_WIRE[i])
    activeToolIdxs.value.push(idx)
    return
  }
  if (idx === 2) {
    // 十字螺丝刀：不高亮，仅跟随鼠标
    if (wiringStep.value !== 'wire_selected') {
      ElMessage.warning('请先选择导线')
      emit('error')
      return
    }
    wiringStep.value = 'screwdriver_active'
    screwdriverFollowing.value = true
    screwdriverStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
    return
  }
  ElMessage.info('该工具将在后续步骤中使用')
}

/** 右侧工具栏点击：智能电表 → 启动跟随，其余工具占位提示 */
function onRightToolClick(idx, e) {
  emit('operation')
  if (props.stepOrder === 7) {
    if (idx === 0) {
      ElMessage.warning('当前步骤无需挂表')
      emit('error')
      return
    }
    onWiringToolClick(idx, e)
    return
  }
  if (idx !== 0) {
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
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
  if (screwdriverFollowing.value) {
    screwdriverStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  }
}

function getDraftState() {
  return {
    meterPlaced: meterPlaced.value,
    switchStates: [...switchStates.value],
    connectedWires: connectedWires.value.map(w => ({ ...w })),
    stepOrder: props.stepOrder
  }
}

function restoreDraft(d) {
  if (d?.meterPlaced) {
    meterPlaced.value = true
    // 恢复挂表状态时移除挂表热区（画布先于草稿构建，需同步清理）
    dropZoneRect?.remove()
    dropZoneRect = null
    switchBackground(Images.meteringRoomWithMeter)
  }
  const needRedraw = leafer && junctionBoxRect.w > 0
  if (needRedraw) {
    applyDraft(d)
  } else {
    pendingDraft = d
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
    // 步骤5+：重建热区/接线盒/开关/孔热区（按当前状态保持视觉位置）
    if (props.stepOrder >= 5) {
      const saved = [...switchStates.value]
      hitLayer.removeAll()
      switchRefs.length = 0
      boxHoleRects.length = 0
      meterHoleRects.length = 0
      wirePaths.length = 0
      buildDropZone(w, h)
      buildJunctionBox(w, h)
      // 接线盒构建后按当前状态恢复开关位置
      if (switchRefs.length > 0) {
        saved.forEach((v, i) => {
          if (switchRefs[i]) {
            switchStates.value[i] = v
            moveSwitch(switchRefs[i], v)
          }
        })
      }
      // 已接导线由 buildJunctionBox 比例校正回调中重绘
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
  // 组件重挂载（刷新/HMR）时自行恢复草稿；HCL 恢复逻辑保留作双保险（restoreDraft 幂等）
  if (props.experimentId && props.stepId) {
    getStepDraft(props.experimentId, props.stepId)
      .then(d => {
        if (d) restoreDraft(d)
      })
      .catch(() => {})
  }
})
// 同组件导航时组件不重新挂载，需监听步骤变化构建开关/孔热区
watch(
  () => props.stepOrder,
  order => {
    // 进入步骤6+ 且未挂表（异常跳转）→ 补上已挂表状态
    if (order >= 6 && !meterPlaced.value) {
      meterPlaced.value = true
      switchBackground(Images.meteringRoomWithMeter)
    }
    if (order >= 5 && leafer) {
      // 步骤5→6：开关在步骤5 构建时无热区，进入步骤6 需重建以生成热区
      if (order === 6 && switchRefs.length > 0 && !switchRefs[0].rect) {
        rebuildSwitches()
      }
      ensureSwitches()
      ensureHoles()
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
  restoreDraft,
  activeToolIdxs
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
