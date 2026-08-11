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
      <!-- 步骤15 十字螺丝刀跟随 -->
      <div v-if="screwdriverFollowing" class="meter-following" :style="screwdriverStyle">
        <img :src="Images.barCrossScrewdriver" alt="螺丝刀" draggable="false" />
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
import { Leafer, Group, Image, Rect, Path, PointerEvent } from 'leafer-ui'
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
// 步骤15 接线状态机
const wiringStep = ref('idle') // idle | plier_selected | wire_selected | screwdriver_active | wire_drawing
const selectedWire = ref(null)
const wireStart = ref(null) // { type: 'box' | 'terminal', hole: n }
const wireStartPos = ref(null)
const connectedWires = ref([]) // [{ boxHole, terminalHole, spec }]
const activeToolIdxs = ref([]) // 剥线钳+当前导线持续高亮，螺丝刀不高亮（仅跟随）
const screwdriverFollowing = ref(false)
const screwdriverStyle = ref({})
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
const boxHoleRects = [] // 接线盒 13 孔热区（步骤15）
const terminalHoleRects = [] // 终端下方 12 孔 + 3 倒三角孔热区（步骤15）
const wirePaths = [] // 已连接导线 Path
let wireFollowPaths = [] // 跟随线 Path
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

// ★ 接线盒顶部 13 孔（与计量小室一致，编号从右往左：孔1 最右、孔13 最左）
const BOX_HOLES = { count: 13, x0: 0.13, x1: 0.866, y: 0.03, size: 0.05 }

// ★ 终端下方左侧 9 孔 = 3 组正三角水平排列（编号与计量小室一致从右往左：
//   右组=孔1-3、中组=孔4-6、左组=孔7-9；组内 k=1 左底、k=2 顶点上、k=3 右底）
//   cx/y0/spanW = 画布比率（位置/组间距），triW/triH/size = 像素（三角形宽/高、孔径）
//   占位，用户按背景图微调；右侧 3 孔见 EXTRA_HOLES
const TERMINAL_HOLES_12 = { cx: 0.3085, spanW: 0.18, triW: 33, y0: 0.576, triH: 4, size: 13 }

// ★ 终端下方右侧 3 孔 = 1 组倒三角（孔10-12，无需连线，占位，用户按背景图微调）
//   cx/y0 = 画布比率（中心位置），spanW/triH/size = 像素（三角形总宽、高、孔径）
//   组内：k=1 左底、k=2 顶点(下)、k=3 右底
const EXTRA_HOLES = { cx: 0.423, y0: 0.57, spanW: 33, triH: 4, size: 13 }

// ★ 7 根导线固定配对（与计量小室一致：接线盒孔 → 终端孔）；双色线用两条半宽线并排模拟
const WIRE_CONNECTIONS = [
  { spec: '4.0红黑', boxHole: 3, terminalHole: 3, pathColor: '#e60000', secondColor: '#000000' },
  { spec: '4.0红', boxHole: 4, terminalHole: 1, pathColor: '#e60000' },
  { spec: '2.5红', boxHole: 5, terminalHole: 2, pathColor: '#e60000' },
  { spec: '2.5绿', boxHole: 9, terminalHole: 5, pathColor: '#00a650' },
  { spec: '4.0黄黑', boxHole: 11, terminalHole: 9, pathColor: '#FFFF00', secondColor: '#000000' },
  { spec: '4.0黄', boxHole: 12, terminalHole: 7, pathColor: '#FFFF00' },
  { spec: '2.5黄', boxHole: 13, terminalHole: 8, pathColor: '#FFFF00' }
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
    rebuildHoles15()
    redrawConnectedWires()
  }
  probe.src = Images.junctionBox
}

/** 构建 10 个开关小图 + 热区（照搬计量小室：初始状态 on，位置相对接线盒，按 orient 旋转） */
// 热区生命周期：步骤13（挂表）起可点击 → 步骤14 结束后移除 → 步骤20（第二次调整）再出现
function buildSwitches() {
  switchRefs.length = 0
  switchStates.value = []
  const showRects = props.stepOrder === 13 || props.stepOrder === 14 || props.stepOrder === 20
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
    let rect = null
    if (showRects) {
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

/** 步骤14 完成检测：开关状态全部匹配目标 → 销毁开关热区并提交 */
function checkSwitches() {
  if (switchStates.value.length !== SWITCH_TARGETS_1.length) return
  const ok = SWITCH_TARGETS_1.every((t, i) => switchStates.value[i] === t)
  if (ok) {
    // 销毁开关热区（保留开关小图），进入步骤15 接线
    switchRefs.forEach(s => {
      s.rect?.remove()
      s.rect = null
    })
    emit('stepCompleted', props.stepOrder)
  }
}

// ─── 步骤15：接电压、电流进出线（7 根导线） ───

/** 接线盒孔 i（1~13，右起）绝对坐标 */
function boxHolePos(i) {
  const rx =
    BOX_HOLES.x0 + ((BOX_HOLES.count - i) / (BOX_HOLES.count - 1)) * (BOX_HOLES.x1 - BOX_HOLES.x0)
  return {
    x: junctionBoxRect.x + rx * junctionBoxRect.w,
    y: junctionBoxRect.y + BOX_HOLES.y * junctionBoxRect.h
  }
}

/** 终端左侧孔 i（1~9，右起）绝对坐标：3 组正三角水平排列（编号与计量小室一致从右往左） */
function terminalHolePos(i) {
  const g = Math.ceil(i / 3) // 1=右组 2=中组 3=左组
  const k = ((i - 1) % 3) + 1 // 组内 1=左底 2=顶点(上) 3=右底
  const groupStep = TERMINAL_HOLES_12.spanW / 3 // 组间距（跨度/3，画布比率）
  const gc = TERMINAL_HOLES_12.cx + (g === 1 ? groupStep : g === 2 ? 0 : -groupStep)
  const half = TERMINAL_HOLES_12.triW / 2 // 组内三角形半宽（像素）
  const xBase = gc * leafer.width
  const y0 = TERMINAL_HOLES_12.y0 * leafer.height
  if (k === 1) return { x: xBase - half, y: y0 }
  if (k === 2) return { x: xBase, y: y0 - TERMINAL_HOLES_12.triH }
  return { x: xBase + half, y: y0 }
}

/** 右侧倒三角孔 k（1=左底、2=顶点下、3=右底）绝对坐标（spanW/triH 为像素） */
function extraHolePos(k) {
  const half = EXTRA_HOLES.spanW / 2
  const cx = EXTRA_HOLES.cx * leafer.width
  const y0 = EXTRA_HOLES.y0 * leafer.height
  if (k === 1) return { x: cx - half, y: y0 }
  if (k === 2) return { x: cx, y: y0 + EXTRA_HOLES.triH }
  return { x: cx + half, y: y0 }
}

/** 步骤15：接线盒就绪后构建孔热区 */
function ensureHoles15() {
  if (props.stepOrder !== 15 || junctionBoxRect.w <= 0 || !leafer) return
  buildBoxHoles()
  buildTerminalHoles12()
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

function buildTerminalHoles12() {
  if (terminalHoleRects.length > 0) return
  // 左侧 9 孔（1-9）：3 组正三角，可连线
  const sz = TERMINAL_HOLES_12.size // 像素
  for (let j = 1; j <= 9; j++) {
    const p = terminalHolePos(j)
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
    rect.on(PointerEvent.CLICK, () => onHoleClick('terminal', j))
    hitLayer.add(rect)
    terminalHoleRects.push(rect)
  }
  // 右侧 3 孔（10-12）：倒三角，无需连线（点击提示）
  const esz = EXTRA_HOLES.size // 像素
  for (let k = 1; k <= 3; k++) {
    const p = extraHolePos(k)
    const rect = new Rect({
      x: p.x - esz / 2,
      y: p.y - esz / 2,
      width: esz,
      height: esz,
      fill: 'rgba(0, 150, 255, 0.2)',
      stroke: 'rgba(0, 150, 255, 0.7)',
      strokeWidth: 1,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => {
      ElMessage.warning('该孔无需连线')
      emit('error')
    })
    hitLayer.add(rect)
    terminalHoleRects.push(rect)
  }
}

/** 接线盒比例校正后重建孔热区 */
function rebuildHoles15() {
  boxHoleRects.forEach(r => r.remove())
  boxHoleRects.length = 0
  terminalHoleRects.forEach(r => r.remove())
  terminalHoleRects.length = 0
  ensureHoles15()
}

/** 2.5 细线基准宽 */
function thinWireWidth() {
  return junctionBoxRect.w * 0.006
}

/** 生成导线 Path 列表（单色 1 条；4.0=2 倍细线宽，双色线=两条细线并排拼接，法向统一屏幕左侧） */
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
  let nx = -dy / len
  let ny = dx / len
  if (nx > 0) {
    nx = -nx
    ny = -ny
  }
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
function drawWirePath(boxHole, terminalHole, wire) {
  const from = boxHolePos(boxHole)
  const to = terminalHolePos(terminalHole)
  const paths = makeWirePaths(from, to, wire)
  paths.forEach(p => hitLayer.add(p))
  wirePaths.push(...paths)
}

/** 按已接导线重绘（画布重建/比例校正后恢复视觉） */
function redrawConnectedWires() {
  wirePaths.forEach(p => p.remove())
  wirePaths.length = 0
  if (junctionBoxRect.w <= 0) return
  connectedWires.value.forEach(w => {
    const conn = WIRE_CONNECTIONS.find(
      c => c.boxHole === w.boxHole && c.terminalHole === w.terminalHole
    )
    if (conn) drawWirePath(w.boxHole, w.terminalHole, conn)
  })
}

/** 孔绝对像素坐标 */
function holeAbsPos(type, hole) {
  if (type === 'box') return boxHolePos(hole)
  return terminalHolePos(hole)
}

/** 点孔：起点固定为接线盒（screwdriver_active 只接受 box），终点为终端下方孔 */
function onHoleClick(type, hole) {
  if (props.stepOrder !== 15) return
  if (wiringStep.value === 'screwdriver_active') {
    if (type !== 'box') {
      ElMessage.warning('请先点击接线盒孔作为起点')
      emit('error')
      return
    }
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
    if (type === 'box') {
      ElMessage.warning('请点击终端下方的接线孔作为终点')
      emit('error')
      return
    }
    stopWireFollow()
    const w = selectedWire.value
    if (w && start.hole === w.boxHole && hole === w.terminalHole) {
      drawWirePath(start.hole, hole, w)
      connectedWires.value.push({ boxHole: start.hole, terminalHole: hole, spec: w.spec })
      emit('operation')
      resetWiring()
      checkWires15()
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

/** 7 根全部接完 → 销毁孔热区并提交（仅步骤15） */
function checkWires15() {
  if (props.stepOrder !== 15) return
  const allOk = WIRE_CONNECTIONS.every(c =>
    connectedWires.value.some(w => w.boxHole === c.boxHole && w.terminalHole === c.terminalHole)
  )
  if (allOk) {
    boxHoleRects.forEach(r => r.remove())
    boxHoleRects.length = 0
    terminalHoleRects.forEach(r => r.remove())
    terminalHoleRects.length = 0
    emit('stepCompleted', props.stepOrder)
  }
}

/** 步骤15 接线状态机（剥线钳→导线→螺丝刀→接线盒孔→终端孔）；剥线钳与当前导线持续高亮 */
function onWiringToolClick(idx, e) {
  if (idx === 3) {
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

// ─── 右侧工具栏 ───
function onRightToolClick(idx, e) {
  emit('operation')
  // 步骤15：接线状态机（剥线钳→导线→螺丝刀→接线盒孔→终端孔）
  if (props.stepOrder === 15) {
    onWiringToolClick(idx, e)
    return
  }
  // 步骤13 挂表：第 2 个为三相三线专变终端
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
  if (screwdriverFollowing.value) {
    screwdriverStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
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
        controlSwitchStates: [...controlSwitchStates.value],
        connectedWires: connectedWires.value.map(w => ({ ...w }))
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
    connectedWires: connectedWires.value.map(w => ({ ...w })),
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
  if (merged.connectedWires?.length)
    connectedWires.value = merged.connectedWires.map(w => ({ ...w }))
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
  boxHoleRects.length = 0
  terminalHoleRects.length = 0
  wirePaths.length = 0
  wireFollowPaths = []
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
  // 步骤15：重建孔热区 + 已接导线
  if (props.stepOrder === 15) {
    ensureHoles15()
    redrawConnectedWires()
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
    // 步骤15：构建接线孔热区（接线盒/终端孔）
    if (order === 15 && leafer) {
      ensureHoles15()
      redrawConnectedWires()
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
      .then(async d => {
        if (d) {
          // 当前步骤草稿缺步骤15 接线结果（HMR 后未保存过）→ 从前序步骤记录补充（仿照计量小室）
          if (props.stepOrder >= 15 && (!d.connectedWires || d.connectedWires.length === 0)) {
            const steps = JSON.parse(
              localStorage.getItem('experimentSteps_' + props.experimentId) || '[]'
            )
            const prevId = steps.find(s => s.stepOrder === props.stepOrder - 1)?.stepId || ''
            if (prevId && prevId !== props.stepId) {
              try {
                const prev = await getStepDraft(props.experimentId, prevId)
                if (prev?.connectedWires?.length) d.connectedWires = prev.connectedWires
              } catch (_) {}
            }
          }
          restoreDraft(d)
        }
      })
      .catch(() => {})
  }
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  stopWireFollow()
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
