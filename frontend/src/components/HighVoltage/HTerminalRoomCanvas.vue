<!-- 终端小室操作画布（步骤13+，Leafer UI）：流程与计量小室一致 -->
<template>
  <div class="terminal-canvas-wrap">
    <div class="canvas-stage">
      <img ref="bgImgRef" :src="currentBg" alt="终端小室" class="bg-img" draggable="false" />
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
      <!-- 鼠标跟随（Vue HTML 层，不进入画布；挂表用右侧物品栏第 2 个专变终端） -->
      <div v-if="isMeterFollowing" class="meter-following" :style="followStyle">
        <img :src="Images.barThreePhaseThreeWireTerminal" alt="专变终端" draggable="false" />
      </div>
      <!-- 孔位信息悬浮层 -->
      <div v-if="tooltipVisible" class="hole-tooltip" :style="tooltipStyle">{{ tooltipText }}</div>
      <!-- 确认键（照搬计量小室：正方形常驻，hover 换图放大；绝对定位按画布像素） -->
      <div class="seal-confirm-btn" :style="confirmBtnStyle" @click="onConfirmClick" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Leafer, Group, Image, Rect, PointerEvent } from 'leafer-ui'
import Images from '@/constants/images'
import { getStepDraft } from '@/api/experiment'

const props = defineProps({
  stepOrder: { type: Number, required: true },
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' }
})
const emit = defineEmits(['operation', 'error', 'stepCompleted', 'confirm'])

// ─── 状态 ───
const currentBg = ref(Images.terminalRoomNoMeter)
const meterPlaced = ref(false)
const isMeterFollowing = ref(false)
const followStyle = ref({})
const canvasStyle = ref({})
const confirmBtnStyle = ref({})
const switchStates = ref([])
const controlSwitchStates = ref([])
const bgImgRef = ref(null)
const leaferViewRef = ref(null)

// ─── Leafer 实例与层 ───
let leafer = null
let bgLayer = null
let hitLayer = null
// 热区/元素引用（resize 重建时清理）
let dropZoneRect = null
let junctionBoxImg = null
let junctionBoxRect = null
const switchRefs = [] // { img, rect }
const controlSwitchRefs = [] // { img, rect }
const controlStripRects = [] // 压板内 4 个竖直长方条热区（信息显示层）
let controlBoardImg = null
let controlBoardRect = null
const stripRects = [] // 12 长方条热区
const holeRects = [] // 36 孔热区
// 悬浮提示
const tooltipVisible = ref(false)
const tooltipStyle = ref({})
const tooltipText = ref('')

// ★ 挂表区域热区（与计量小室一致，相对画布宽高比率，用户按背景图微调）
const DROP_ZONE = { x: 0.15, y: 0.04, w: 0.37, h: 0.58 }

// ★ 接线盒（与计量小室一致：左下角贴底，宽固定画布 27%，高按图片比例 auto）
const JUNCTION_BOX = { x: 0.202, y: 0.746, w: 0.27 }

// ★ 接线盒开关（10 个，位置与计量小室一致；on/off 独立坐标相对接线盒左上角）
const SWITCHES = [
  { orient: 'v', on: { x: 0.145, y: 0.41 }, off: { x: 0.145, y: 0.55 } },
  { orient: 'hU', on: { x: 0.173, y: 0.4 }, off: { x: 0.225, y: 0.4 } },
  { orient: 'hD', on: { x: 0.314, y: 0.67 }, off: { x: 0.26, y: 0.67 } },
  { orient: 'v', on: { x: 0.395, y: 0.41 }, off: { x: 0.395, y: 0.55 } },
  { orient: 'hU', on: { x: 0.433, y: 0.4 }, off: { x: 0.485, y: 0.4 } },
  { orient: 'hD', on: { x: 0.574, y: 0.67 }, off: { x: 0.52, y: 0.67 } },
  { orient: 'v', on: { x: 0.654, y: 0.41 }, off: { x: 0.654, y: 0.55 } },
  { orient: 'hU', on: { x: 0.688, y: 0.4 }, off: { x: 0.74, y: 0.4 } },
  { orient: 'hD', on: { x: 0.829, y: 0.67 }, off: { x: 0.775, y: 0.67 } },
  { orient: 'v', on: { x: 0.909, y: 0.41 }, off: { x: 0.909, y: 0.55 } }
]
// 开关图宽（相对接线盒宽度比率），高度按图片比例 auto
const SWITCH_SIZE = { w: 0.1 }
// 步骤14 第一次开关目标状态（按开关顺序 1-10）
const SWITCH_TARGETS_1 = ['off', 'on', 'on', 'off', 'off', 'off', 'off', 'on', 'on', 'off']
// 图片宽高比兜底值（与计量小室一致；运行时优先加载真实比例）
const JUNCTION_BOX_ASPECT = 2.519
const SWITCH_ASPECT = 2.109

// ─── 遥控压板与开关 ───
// 压板/压板开关图片宽高比兜底值（与真实资源一致：1318×921、752×217；运行时优先加载真实比例）
const CONTROL_BOARD_ASPECT = 1.431
const CONTROL_SWITCH_ASPECT = 3.465
let controlBoardAspect = null

// ★ 遥控压板面板（右侧靠上，占位，用户按背景图微调；高按图片比例 auto）
const CONTROL_BOARD = { x: 0.6825, y: 0.124, w: 0.217 }

// ★ 遥控压板开关（4 个，覆盖在压板热区上方，相对压板左上角定位）
//   target: 目标状态（关），on/off: 两个状态独立坐标（相对压板比率），点击热区切换
const CONTROL_SWITCHES = [
  { target: 'off', on: { x: 0.28, y: 0.066 }, off: { x: 0.43, y: 0.066 } },
  { target: 'off', on: { x: 0.28, y: 0.325 }, off: { x: 0.43, y: 0.325 } },
  { target: 'off', on: { x: 0.28, y: 0.566 }, off: { x: 0.43, y: 0.566 } },
  { target: 'off', on: { x: 0.28, y: 0.818 }, off: { x: 0.43, y: 0.818 } }
]
// 压板开关图宽（相对压板宽度比率），高度按图片比例 auto
const CONTROL_SWITCH_SIZE = { w: 0.29 }

// ★ 压板内 4 个竖直长方体热区（信息显示层：开关下一层、面板上一层；hover 浮窗显示名称）
//   y 与 CONTROL_SWITCHES 对齐（自上而下遥控1-1/1-2/2-1/2-2），连线终点在长方条左侧边中点
// 中心 y = h/2 + i*h（间距 = 高度 h 时紧贴无间隙），覆盖压板全高 0~1
const CONTROL_STRIPS = [
  { name: '遥控1-1', y: 0.125 },
  { name: '遥控1-2', y: 0.375 },
  { name: '遥控2-1', y: 0.625 },
  { name: '遥控2-2', y: 0.875 }
]
// 长方条宽/高（相对压板宽度/高度比率，占位，用户按背景图微调）
const CONTROL_STRIP_SIZE = { w: 0.987, h: 0.25, x: 2 }

// ★ 压板下方 12 个长方形热区（横条上下贴排，占位，用户按背景图微调）
const TERMINAL_STRIPS = {
  x: 0.73,
  y: 0.342,
  w: 0.122,
  h: 0.315,
  names: [
    '遥信1-1',
    '遥信1-2',
    '遥信2-1',
    '遥信2-2',
    '正向有功+',
    '正向有功-',
    '正向无功+',
    '正向无功-',
    'RS485 A',
    'RS485 B',
    '前门禁1',
    '前门禁2'
  ]
}

// ★ 终端 36 孔热区（编号 13-48，挂表后显示，用于步骤17 连线）
//   第一行 3 组（8+8+10），第二行 3 组（3+3+4）；组内间隔相同、组间间隔不同
//   间隔需按背景图手动调节（占位值）
const TERMINAL_HOLES = {
  size: 0.007,
  row1: {
    y: 0.476,
    x0: 0.216,
    groups: [
      { start: 13, count: 8, gap: 0.00365 },
      { start: 21, count: 8, gap: 0.0017 },
      { start: 29, count: 10, gap: 0.0014 }
    ],
    gapBetween: [0.001, 0.002]
  },
  row2: {
    y: 0.432,
    x0: 0.216,
    groups: [
      { start: 39, count: 3, gap: 0.0048 },
      { start: 42, count: 3, gap: 0.0048 },
      { start: 45, count: 4, gap: 0.0037 }
    ],
    gapBetween: [0.004, 0.0035]
  }
}

// 背景按步骤切换
function bgForStep(order) {
  if (order >= 21) return Images.terminalRoomCovered
  if (order >= 18) return Images.terminalRoomWired
  if (order >= 14) return Images.terminalRoomWithMeter
  return Images.terminalRoomNoMeter
}

function switchBackground(url) {
  currentBg.value = url
  if (leafer) {
    const w = leafer.width
    const h = leafer.height
    bgLayer.removeAll()
    bgLayer.add(new Image({ url, x: 0, y: 0, width: w, height: h }))
  }
}

// ─── 画布构建 ───
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
  buildAll(w, h)
  updateConfirmBtn(w, h)
  if (pendingDraft) {
    applyDraft(pendingDraft)
    pendingDraft = null
  }
}

/** 构建全部初始元素（接线盒/开关、遥控压板/开关、12 长方条、挂表热区） */
function buildAll(w, h) {
  buildJunctionBox(w, h)
  buildControlBoard(w, h)
  buildTerminalStrips(w, h)
  buildDropZone(w, h)
  // 挂表后构建 36 孔热区（含回档恢复场景）
  if (meterPlaced.value) buildTerminalHoles(w, h)
}

function bindEvents(w, h) {
  leafer.on(PointerEvent.CLICK, e => {
    const p = e.getLocalPoint()
    // 挂表热区（仅步骤13 且未挂表）
    if (!meterPlaced.value && props.stepOrder === 13) {
      const dz = { x: w * DROP_ZONE.x, y: h * DROP_ZONE.y, w: w * DROP_ZONE.w, h: h * DROP_ZONE.h }
      if (p.x >= dz.x && p.x <= dz.x + dz.w && p.y >= dz.y && p.y <= dz.y + dz.h) {
        handleDrop()
        return
      }
      if (isMeterFollowing.value) {
        ElMessage.warning('请选择正确的放置位置')
        emit('error')
        isMeterFollowing.value = false
        return
      }
    }
  })
}

// ─── 挂表（步骤13） ───
function handleDrop() {
  if (props.stepOrder !== 13 || meterPlaced.value) return
  if (!isMeterFollowing.value) {
    ElMessage.warning('请先在右侧工具栏选择专变终端')
    emit('error')
    return
  }
  isMeterFollowing.value = false
  meterPlaced.value = true
  dropZoneRect?.remove()
  dropZoneRect = null
  switchBackground(Images.terminalRoomWithMeter)
  // 挂表完成后绘制 36 孔热区（步骤17 连线用）
  buildTerminalHoles(leafer.width, leafer.height)
  emit('stepCompleted', props.stepOrder)
}

// ─── 接线盒与开关 ───
function buildJunctionBox(w, h) {
  const boxW = w * JUNCTION_BOX.w
  const boxH = boxW / JUNCTION_BOX_ASPECT
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
  // 开关初始全 on
  buildSwitches()
  // 图片比例加载后校正高度并重建开关
  const probe = new Image()
  probe.onload = () => {
    if (!junctionBoxImg) return
    const aspect = probe.naturalWidth / probe.naturalHeight || JUNCTION_BOX_ASPECT
    junctionBoxImg.height = junctionBoxImg.width / aspect
    junctionBoxRect = {
      x: junctionBoxImg.x,
      y: junctionBoxImg.y,
      w: junctionBoxImg.width,
      h: junctionBoxImg.height
    }
    rebuildSwitches()
  }
  probe.src = Images.junctionBox
}

/** 构建 10 个开关小图 + 热区（照搬计量小室：初始状态 on，位置相对接线盒，按 orient 旋转） */
function buildSwitches() {
  switchRefs.length = 0
  switchStates.value = []
  const { x: bx, y: by, w: bw, h: bh } = junctionBoxRect
  SWITCHES.forEach((cfg, i) => {
    const state = 'on'
    switchStates.value.push(state)
    const pos = cfg[state]
    const x = bx + pos.x * bw
    const y = by + pos.y * bh
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
    switchRefs.push({ cfg, img, rect, sw })
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

function toggleSwitch(i) {
  emit('operation')
  const s = switchRefs[i]
  if (!s) return
  const cur = switchStates.value[i]
  const next = cur === 'on' ? 'off' : 'on'
  switchStates.value[i] = next
  moveSwitch(s, next)
  // 步骤14：状态匹配目标则完成
  if (props.stepOrder === 14) checkSwitches()
}

/** 开关位置：按状态取 on/off 两套独立坐标（相对接线盒），直接定位 */
function switchPos(s, state) {
  const p = s.cfg[state]
  return {
    x: junctionBoxRect.x + p.x * junctionBoxRect.w,
    y: junctionBoxRect.y + p.y * junctionBoxRect.h
  }
}

function moveSwitch(s, state) {
  const p = switchPos(s, state)
  s.img.x = p.x
  s.img.y = p.y
  if (s.rect) {
    s.rect.x = p.x
    s.rect.y = p.y
  }
}

/** 接线盒比例校正后重建开关（保留当前状态） */
function rebuildSwitches() {
  if (switchRefs.length === 0) {
    buildSwitches()
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

/** 步骤14 完成检测：开关状态全部匹配目标 */
function checkSwitches() {
  if (switchStates.value.length !== SWITCH_TARGETS_1.length) return
  const ok = SWITCH_TARGETS_1.every((t, i) => switchStates.value[i] === t)
  if (ok) emit('stepCompleted', props.stepOrder)
}

function buildControlBoard(w, h) {
  const bw = w * CONTROL_BOARD.w
  const bh = bw / (controlBoardAspect || CONTROL_BOARD_ASPECT)
  controlBoardRect = { x: w * CONTROL_BOARD.x, y: h * CONTROL_BOARD.y, w: bw, h: bh }
  controlBoardImg = new Image({
    url: Images.remoteControlBoard,
    x: controlBoardRect.x,
    y: controlBoardRect.y,
    width: bw,
    height: bh,
    zIndex: 1
  })
  hitLayer.add(controlBoardImg)
  // 层级：面板(zIndex 1) < 信息长方条(zIndex 2) < 开关(zIndex 3)
  buildControlStrips()
  buildControlSwitches()
  const probe = new Image()
  probe.onload = () => {
    if (!controlBoardImg) return
    controlBoardAspect = probe.naturalWidth / probe.naturalHeight || CONTROL_BOARD_ASPECT
    controlBoardImg.height = controlBoardImg.width / controlBoardAspect
    controlBoardRect = {
      x: controlBoardImg.x,
      y: controlBoardImg.y,
      w: controlBoardImg.width,
      h: controlBoardImg.height
    }
    rebuildControlSwitches()
  }
  probe.src = Images.remoteControlBoard
}

/** 构建压板内 4 个竖直长方条热区（信息显示层，zIndex 2；hover 浮窗显示名称） */
function buildControlStrips() {
  controlStripRects.forEach(r => r.remove())
  controlStripRects.length = 0
  CONTROL_STRIPS.forEach(item => {
    const sw = controlBoardRect.w * CONTROL_STRIP_SIZE.w
    const sh = controlBoardRect.h * CONTROL_STRIP_SIZE.h
    const rect = new Rect({
      x: controlBoardRect.x + CONTROL_STRIP_SIZE.x,
      y: controlBoardRect.y + controlBoardRect.h * (item.y - CONTROL_STRIP_SIZE.h / 2),
      width: sw,
      height: sh,
      fill: 'rgba(0, 150, 255, 0.15)',
      stroke: 'rgba(0, 150, 255, 0.7)',
      strokeWidth: 1,
      zIndex: 2
    })
    hitLayer.add(rect)
    rect.on(PointerEvent.ENTER, () => showTooltip(item.name, rect))
    rect.on(PointerEvent.LEAVE, () => {
      tooltipVisible.value = false
    })
    controlStripRects.push(rect)
  })
}

/** 构建压板 4 个开关小图 + 热区（初始 on，目标为关） */
function buildControlSwitches() {
  controlSwitchRefs.length = 0
  controlSwitchStates.value = []
  CONTROL_SWITCHES.forEach(cfg => {
    const state = 'on'
    controlSwitchStates.value.push(state)
    const img = new Image({
      url: Images.remoteControlSwitch,
      x: 0,
      y: 0,
      width: 0,
      height: 0,
      zIndex: 2
    })
    hitLayer.add(img)
    const rect = new Rect({
      x: 0,
      y: 0,
      width: 1,
      height: 1,
      fill: 'rgba(0, 150, 255, 0.2)',
      stroke: 'rgba(0, 150, 255, 0.8)',
      strokeWidth: 1,
      zIndex: 3
    })
    hitLayer.add(rect)
    const ref = { img, rect, cfg }
    controlSwitchRefs.push(ref)
    positionControlSwitch(ref)
  })
  controlSwitchRefs.forEach((ref, i) => {
    ref.rect.on(PointerEvent.CLICK, () => {
      toggleControlSwitch(i)
    })
  })
}

function positionControlSwitch(ref) {
  if (!controlBoardRect) return
  const w = controlBoardRect.w * CONTROL_SWITCH_SIZE.w
  const h = w / CONTROL_SWITCH_ASPECT
  const pos = ref.cfg[controlSwitchStates.value[controlSwitchRefs.indexOf(ref)]]
  ref.img.x = controlBoardRect.x + controlBoardRect.w * pos.x
  ref.img.y = controlBoardRect.y + controlBoardRect.h * pos.y
  ref.img.width = w
  ref.img.height = h
  ref.rect.x = ref.img.x
  ref.rect.y = ref.img.y
  ref.rect.width = w
  ref.rect.height = h
}

function toggleControlSwitch(i) {
  emit('operation')
  const v = controlSwitchStates.value[i]
  const next = v === 'on' ? 'off' : 'on'
  controlSwitchStates.value[i] = next
  moveControlSwitch(controlSwitchRefs[i], next)
}

function moveControlSwitch(ref, state) {
  if (!controlBoardRect) return
  const w = controlBoardRect.w * CONTROL_SWITCH_SIZE.w
  const h = w / CONTROL_SWITCH_ASPECT
  const pos = ref.cfg[state]
  ref.img.x = controlBoardRect.x + controlBoardRect.w * pos.x
  ref.img.y = controlBoardRect.y + controlBoardRect.h * pos.y
  ref.img.width = w
  ref.img.height = h
  ref.rect.x = ref.img.x
  ref.rect.y = ref.img.y
  ref.rect.width = w
  ref.rect.height = h
}

function rebuildControlSwitches() {
  if (controlSwitchRefs.length === 0) {
    buildControlStrips()
    buildControlSwitches()
    return
  }
  const saved = [...controlSwitchStates.value]
  controlSwitchRefs.forEach(s => {
    s.img.remove()
    s.rect?.remove()
  })
  controlSwitchRefs.length = 0
  buildControlStrips()
  buildControlSwitches()
  saved.forEach((v, i) => {
    if (controlSwitchRefs[i]) {
      controlSwitchStates.value[i] = v
      moveControlSwitch(controlSwitchRefs[i], v)
    }
  })
}

// ─── 压板下方 12 长方条热区 ───
function buildTerminalStrips(w, h) {
  const { x, y, w: sw, h: sh, names } = TERMINAL_STRIPS
  const bx = w * x
  const by = h * y
  const bw = w * sw
  const bh = h * sh
  const stripH = bh / names.length
  names.forEach((name, i) => {
    const rect = new Rect({
      x: bx,
      y: by + i * stripH,
      width: bw,
      height: stripH - 1,
      fill: 'rgba(0, 150, 255, 0.2)',
      stroke: 'rgba(0, 150, 255, 0.8)',
      strokeWidth: 1,
      zIndex: 3
    })
    hitLayer.add(rect)
    rect.on(PointerEvent.ENTER, () => showTooltip(name, rect))
    rect.on(PointerEvent.LEAVE, () => {
      tooltipVisible.value = false
    })
    stripRects.push(rect)
  })
}

/** 悬浮层显示在热区上方居中（画布局部坐标 → 屏幕坐标） */
function showTooltip(text, rect) {
  tooltipText.value = text
  tooltipVisible.value = true
  const view = leaferViewRef.value?.getBoundingClientRect()
  if (!view) return
  const cx = view.left + rect.x + rect.width / 2
  const cy = view.top + rect.y
  tooltipStyle.value = {
    left: cx + 'px',
    top: cy - 12 + 'px',
    transform: 'translate(-50%, -100%)'
  }
}

// ─── 挂表热区（步骤13，蓝色可视化） ───
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

// ─── 终端 36 孔热区（挂表后显示，编号 13-48） ───
function buildTerminalHoles(w, h) {
  holeRects.forEach(r => r.remove())
  holeRects.length = 0
  const sz = w * TERMINAL_HOLES.size
  const rows = [TERMINAL_HOLES.row1, TERMINAL_HOLES.row2]
  rows.forEach(row => {
    let x = w * row.x0
    row.groups.forEach((g, gi) => {
      for (let k = 0; k < g.count; k++) {
        const rect = new Rect({
          x: x,
          y: h * row.y,
          width: sz,
          height: sz,
          fill: 'rgba(0, 150, 255, 0.3)',
          stroke: 'rgba(0, 150, 255, 0.9)',
          strokeWidth: 1,
          zIndex: 3
        })
        hitLayer.add(rect)
        rect.on(PointerEvent.ENTER, () => showTooltip(String(g.start + k), rect))
        rect.on(PointerEvent.LEAVE, () => {
          tooltipVisible.value = false
        })
        holeRects.push(rect)
        x += sz + w * g.gap
      }
      if (gi < row.groups.length - 1) x += w * row.gapBetween[gi]
    })
  })
}

// ─── 确认键（照搬计量小室：正方形，右下角，无激活态；hover 换图放大） ───
function updateConfirmBtn(w, h) {
  const size = Math.round(w * 0.18)
  confirmBtnStyle.value = {
    width: size + 'px',
    height: size + 'px',
    left: Math.round(w * 0.96 - size) + 'px',
    top: Math.round(h * 1.04 - size) + 'px'
  }
}

function onConfirmClick() {
  ElMessage.warning('请先完成当前步骤')
  emit('error')
}

// ─── 右侧工具栏（挂表用第 2 个：三相三线专变终端） ───
function onRightToolClick(idx, e) {
  emit('operation')
  if (idx !== 1) {
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  if (props.stepOrder !== 13) {
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

// ─── 存档 ───
// 提交只写入当前步骤记录（resultData），刷新到后续步骤时后端查不到前序状态，
// 故将完成状态同步持久化到 localStorage（按实验 ID 隔离），恢复时兜底合并
const LS_KEY = () => 'terminalRoom_' + props.experimentId
function persistState() {
  try {
    localStorage.setItem(
      LS_KEY(),
      JSON.stringify({
        meterPlaced: meterPlaced.value,
        switchStates: [...switchStates.value],
        controlSwitchStates: [...controlSwitchStates.value]
      })
    )
  } catch (_) {}
}

function getDraftState() {
  persistState()
  return {
    meterPlaced: meterPlaced.value,
    switchStates: [...switchStates.value],
    controlSwitchStates: [...controlSwitchStates.value],
    stepOrder: props.stepOrder
  }
}

let pendingDraft = null
function restoreDraft(d) {
  // 后端草稿优先，缺失字段（如挂表状态）从前序提交的 localStorage 兜底
  let local = {}
  try {
    local = JSON.parse(localStorage.getItem(LS_KEY()) || '{}')
  } catch (_) {}
  const merged = { ...local, ...(d || {}) }
  if (merged.meterPlaced) {
    meterPlaced.value = true
    switchBackground(bgForStep(props.stepOrder))
  }
  if (merged.switchStates?.length) switchStates.value = [...merged.switchStates]
  if (merged.controlSwitchStates?.length)
    controlSwitchStates.value = [...merged.controlSwitchStates]
  if (leafer) {
    applyDraft()
  } else {
    pendingDraft = merged
  }
}

function applyDraft() {
  if (!leafer) return
  const w = leafer.width
  const h = leafer.height
  // 重建全部元素（保留状态）
  hitLayer.removeAll()
  switchRefs.length = 0
  controlSwitchRefs.length = 0
  controlStripRects.length = 0
  stripRects.length = 0
  holeRects.length = 0
  dropZoneRect = null
  junctionBoxImg = null
  controlBoardImg = null
  buildAll(w, h)
  // 恢复开关状态位置
  switchStates.value.forEach((v, i) => {
    if (switchRefs[i]) moveSwitch(switchRefs[i], v)
  })
  controlSwitchStates.value.forEach((v, i) => {
    if (controlSwitchRefs[i]) moveControlSwitch(controlSwitchRefs[i], v)
  })
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
    updateConfirmBtn(w, h)
    bgLayer.removeAll()
    bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
    applyDraft({})
  }, 200)
}

// 步骤切换：步骤14 进入时若开关未构建（回档/直跳）补构建；步骤14 检查完成后不再重置
watch(
  () => props.stepOrder,
  order => {
    // 同组件导航/异常跳转：步骤14+ 未挂表 → 补上挂表状态（背景按步骤推断）
    if (order >= 14 && !meterPlaced.value) {
      meterPlaced.value = true
      switchBackground(bgForStep(order))
    }
    if (order === 14 && switchRefs.length === 0 && leafer) {
      buildSwitches()
    }
    if (order === 14) {
      // 已符合目标（如回档时已调整过）直接完成
      checkSwitches()
    }
  }
)

onMounted(() => {
  const img = bgImgRef.value
  if (!img) return
  if (img.complete) createCanvas()
  else img.addEventListener('load', createCanvas, { once: true })
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

/* 鼠标跟随（HTML 层） */
.meter-following {
  position: fixed;
  transform: translate(-50%, -50%);
  z-index: 100;
  pointer-events: none;
  width: 90px;
}

.meter-following img {
  width: 100%;
  display: block;
  pointer-events: none;
}

/* 孔位信息悬浮层 */
.hole-tooltip {
  position: fixed;
  transform: translate(-50%, -130%);
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  font-size: 13px;
  padding: 3px 8px;
  border-radius: 4px;
  pointer-events: none;
  z-index: 101;
  white-space: nowrap;
}

/* 确认键：与 Leafer 画布绝对定位（left/top/宽高由 updateConfirmBtn 按画布像素计算），绿色版 */
.seal-confirm-btn {
  position: absolute;
  z-index: 200;
  cursor: pointer;
  background-image: var(--img-confirm-btn-green);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
}

.seal-confirm-btn:hover {
  background-image: var(--img-confirm-btn-green-hover);
  transform: scale(1.05);
}
</style>
