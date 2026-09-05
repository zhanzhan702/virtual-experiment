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
      <div v-if="cableFollowing" class="meter-following" :style="cableFollowStyle">
        <img :src="Images.barSignalCable6Core" alt="6芯信号线" draggable="false" />
      </div>
      <div v-if="tieFollowing" class="meter-following" :style="tieFollowStyle">
        <img :src="Images.barCableTieLabel" alt="扎带标识牌" draggable="false" />
      </div>
      <div v-if="sealFollowing" class="meter-following" :style="sealFollowStyle">
        <img :src="Images.barSeal" alt="铅封" draggable="false" />
      </div>
      <!-- 孔位信息悬浮层（步骤8） -->
      <div v-if="tooltipVisible" class="hole-tooltip" :style="tooltipStyle">{{ tooltipText }}</div>
      <!-- 确认键（计量小室全流程常驻，仅步骤11 铅封完成后激活；绝对定位按画布像素） -->
      <div class="seal-confirm-btn" :class="{ active: sealsDone }" :style="confirmBtnStyle" @click="onConfirmClick" />
      <!-- 终端编号提示面板（步骤5-11 常驻；CSS 百分比相对画布定位，画布上方、随画布缩放） -->
      <div v-if="showTerminalGuide" class="terminal-guide-overlay">
        <HMeteringRoomGuide />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Leafer, Group, Image, Rect, Path, PointerEvent } from 'leafer-ui'
import Images from '@/constants/images'
import HMeteringRoomGuide from '@/components/HighVoltage/HMeteringRoomGuide.vue'
import { getStepDraft } from '@/api/experiment'

const props = defineProps({
  stepOrder: { type: Number, required: true },
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' }
})
const emit = defineEmits(['operation', 'error', 'stepCompleted', 'confirm'])

// 步骤5 挂表完成后至步骤11 结束显示终端编号提示面板（步骤11 铅封完成后消失）
const showTerminalGuide = computed(
  () => props.stepOrder >= 5 && props.stepOrder <= 11 && meterPlaced.value
)

// ─── 状态 ───
const currentBg = ref(Images.meteringRoomNoMeter)
const meterPlaced = ref(false)
const isMeterFollowing = ref(false)
const followStyle = ref({})
const canvasStyle = ref({})
// 确认键绝对定位样式（按画布实际像素计算，右下角，保持原有相对比例视觉）
const confirmBtnStyle = ref({})
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
// 步骤8 6芯信号线状态机
const signalStep = ref('idle') // idle | cable_following | cable_placed | core_following
const phase = ref('right') // right=长方体端 | left=电表端
const selectedCore = ref(null) // { side: 'right' | 'left', idx, core }
const connectedCores = ref([]) // [{ side, idx, terminal, color }]
const cablePlaced = ref(false)
const cableFollowing = ref(false)
const cableFollowStyle = ref({})
const tooltipVisible = ref(false)
const tooltipText = ref('')
const tooltipStyle = ref({})
// 步骤9 扎带标识牌
const tieFollowing = ref(false)
const tieFollowStyle = ref({})
const tiePlaced = ref(false)
// 步骤11 铅封
const sealPlaced = ref([])
const sealFollowing = ref(false)
const sealFollowStyle = ref({})

// 步骤6+ 必然已挂表：初始化背景（不依赖草稿，刷新/重挂载均正确）
// 步骤5=NoMeter、6-8=WithMeter、9=Wired（步骤8 完成时切）、10=WithCableTies（步骤9 完成时切）、11+=Covered（步骤10 完成时切）
function bgForStep(order) {
  if (order >= 11) return Images.meteringRoomCovered
  if (order >= 10) return Images.meteringRoomWithCableTies
  if (order >= 9) return Images.meteringRoomWired
  return Images.meteringRoomWithMeter
}
if (props.stepOrder >= 6) {
  meterPlaced.value = true
  currentBg.value = bgForStep(props.stepOrder)
}

// ─── Leafer 实例与层 ───
let leafer = null
let bgLayer = null
let hitLayer = null

// ★ 挂表区域热区（相对画布宽高的比率，用户按背景图微调；带颜色便于调整定位）
const DROP_ZONE = { x: 0.15, y: 0.04, w: 0.37, h: 0.53 }

// ★ 接线盒（左下角贴底，宽固定为画布宽的 1/2，高度按图片比例 auto，用户按背景图微调）
//   步骤5（未挂表）起显示，直到背景图切换为盖盖子的计量小室后隐藏
const JUNCTION_BOX = { x: 0.202, y: 0.746, w: 0.27 }

// ★ 接线盒开关（10 个，一行排列：竖 双横 竖 双横 竖 双横 竖，竖作为双横的间隔）
//   orient: v=竖(顺时针90°), hU=上排横(0°), hD=下排横(180°)
//   target: 目标状态（按数组顺序 1/4/5 断开 off、2/3/6/7 闭合 on，8/9/10 待用户补充）
//   on/off: 两个状态各自独立的坐标（相对接线盒左上角的比率 0~1），切换时直接定位173
const SWITCHES = [
  { orient: 'v', target: 'off', on: { x: 0.145, y: 0.41 }, off: { x: 0.145, y: 0.55 } },
  { orient: 'hU', target: 'on', on: { x: 0.173, y: 0.4 }, off: { x: 0.225, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.314, y: 0.67 }, off: { x: 0.26, y: 0.67 } },
  { orient: 'v', target: 'off', on: { x: 0.395, y: 0.41 }, off: { x: 0.395, y: 0.55 } },
  { orient: 'hU', target: 'on', on: { x: 0.433, y: 0.4 }, off: { x: 0.485, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.574, y: 0.67 }, off: { x: 0.52, y: 0.67 } },
  { orient: 'v', target: 'off', on: { x: 0.654, y: 0.41 }, off: { x: 0.654, y: 0.55 } },
  { orient: 'hU', target: 'on', on: { x: 0.688, y: 0.4 }, off: { x: 0.74, y: 0.4 } },
  { orient: 'hD', target: 'on', on: { x: 0.829, y: 0.67 }, off: { x: 0.775, y: 0.67 } },
  { orient: 'v', target: 'off', on: { x: 0.909, y: 0.41 }, off: { x: 0.909, y: 0.55 } }
]
// 开关图宽（相对接线盒宽度的比率），高度按图片比例 auto（不压缩）
const SWITCH_SIZE = { w: 0.1 }
// 步骤10 第二次调整目标（按开关顺序 1-10：1on 2off 3on 4on 5on 6on 7on 8off 9on 10on）
const SWITCH_TARGETS_2 = ['on', 'off', 'on', 'on', 'on', 'on', 'on', 'off', 'on', 'on']
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
  cx: 0.309,
  spanW: 0.176,
  y0: 0.528,
  triH: 0.005,
  size: 0.015
}

// ★ 7 根导线固定配对（接线顺序不限）；双色线（红黑/黄黑）用两条半宽线并排模拟
//   elbowOffset：同色组（pathColor）内直角拐弯的高度增量，叠加在基准 bendRatio 上
//   （如基准 0.55，组内可设 0 / 0.02 / 0.04 错开，防止同色组相邻线水平段重叠；越大拐弯越靠电表孔）
const WIRE_CONNECTIONS = [
  {
    spec: '4.0红黑',
    boxHole: 3,
    meterHole: 3,
    pathColor: '#e60000',
    secondColor: '#000000',
    elbowOffset: 0.04
  },
  { spec: '4.0红', boxHole: 4, meterHole: 1, pathColor: '#e60000', elbowOffset: 0 },
  { spec: '2.5红', boxHole: 5, meterHole: 2, pathColor: '#e60000', elbowOffset: 0.02 },
  { spec: '2.5绿', boxHole: 9, meterHole: 5, pathColor: '#00a650', elbowOffset: 0 },
  {
    spec: '4.0黄黑',
    boxHole: 11,
    meterHole: 9,
    pathColor: '#FFFF00',
    secondColor: '#000000',
    elbowOffset: 0.02
  },
  { spec: '4.0黄', boxHole: 12, meterHole: 7, pathColor: '#FFFF00', elbowOffset: 0 },
  { spec: '2.5黄', boxHole: 13, meterHole: 8, pathColor: '#FFFF00', elbowOffset: 0.04 }
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

// ─── 接线盒→电表 7 根进出线：直角折线（非斜直线）与盖盖遮挡 ───
// 原网页接线为「竖直-水平-竖直」直角折线，拐弯靠近电表孔；bendRatio 为水平段高度占
// from(接线盒,下)→to(电表,上) 竖直间距的比例（0~1，越大水平段越贴近电表孔），可按背景图微调
const WIRE_ELBOW = { bendRatio: 0.52 }
// 盖盖（Covered 背景）后被盖板下缘截断的遮挡线 y（相对画布比率，占位，用户按背景图微调）
// 导线最高只画到此处，隐藏被电表/终端盖板盖住的部分；未盖盖时不生效
const WIRE_COVER_Y = 0.55

// ─── 步骤8：6芯信号线 ───
// 信号线放置热区（画布中间偏右，占位，用户按背景图微调）
const CABLE_DROP_ZONE = { x: 0.48, y: 0.2, w: 0.2, h: 0.45 }
// 信号线图片（521×718 竖条）：位置与放置热区对齐，宽相对画布，高按比例 auto
const SIGNAL_CABLE = { w: 0.23 }

// 长方体（右上角，8 横条上下贴排）；每格热区在块内部左侧
const TERMINAL_BLOCK = {
  x: 0.727,
  y: 0.123,
  w: 0.32,
  h: 0.22,
  names: [
    '前门禁1',
    '前门禁2',
    'RS485 A',
    'RS485 B',
    '正向无功+',
    '正向无功-',
    '正向有功+',
    '正向有功-'
  ]
}

// 电表 16 孔（编号 13-28，同一水平线；13-15 大间隔、16-28 小间隔、15→16 过渡间隔占位）
const METER_TERMINALS = {
  x0: 0.2315,
  y: 0.4225,
  gapBig: 0.0175,
  gapSmall: 0.01227,
  gapMid: 0.0254,
  size: 0.009
}

// 芯点（相对信号线图片比率，每个点独立 x/y 坐标，后期单独调整）
// 右上角 6 点 → 长方体孔；左下角 6 点 → 电表孔
const CORE_TIPS = {
  right: [
    { color: '#FFFFFF', label: '白', terminal: 3, pos: { x: 0.73, y: 0.025 } },
    { color: '#FF0000', label: '红', terminal: 4, pos: { x: 0.79, y: 0.09 } },
    { color: '#0D8068', label: '绿', terminal: 5, pos: { x: 0.86, y: 0.152 } },
    { color: '#FF9900', label: '橙', terminal: 6, pos: { x: 0.915, y: 0.217 } },
    { color: '#663300', label: '棕', terminal: 7, pos: { x: 0.955, y: 0.28 } },
    { color: '#000000', label: '黑', terminal: 8, pos: { x: 0.98, y: 0.33 } }
  ],
  left: [
    { color: '#663300', label: '棕', terminal: 19, pos: { x: 0.04, y: 0.97 } },
    { color: '#000000', label: '黑', terminal: 21, pos: { x: 0.02, y: 0.89 } },
    { color: '#0D8068', label: '绿', terminal: 20, pos: { x: 0.04, y: 0.82 } },
    { color: '#FF9900', label: '橙', terminal: 21, pos: { x: 0.08, y: 0.77 } },
    { color: '#FFFFFF', label: '白', terminal: 27, pos: { x: 0.14, y: 0.71 } },
    { color: '#FF0000', label: '红', terminal: 28, pos: { x: 0.22, y: 0.7 } }
  ]
}
// 芯点热区统一大小（相对信号线图片宽度）
const CORE_TIP_SIZE = 0.05

// ─── 步骤9：扎带标识牌 ───
// 扎带热区 = 信号线放置热区位置/大小（用户确认保持一致）；扎带小图（相对热区宽，占位微调）
const CABLE_TIE_SIZE = 0.3

// ─── 步骤11：铅封 ───
// 5 个铅封：独立位置（相对画布比率）+ 旋转角度（占位，用户微调）；电表 3 处 + 接线盒两端 2 处
const SEALS = [
  { x: 0.222, y: 0.46, rotation: 0 },
  { x: 0.462, y: 0.35, rotation: 0 },
  { x: 0.472, y: 0.46, rotation: 0 },
  { x: 0.224, y: 0.818, rotation: 0 },
  { x: 0.468, y: 0.85, rotation: 0 }
]
// 铅封大小一致（相对画布宽）；Sealed.png 为 1.54:1 横向，高度按比例
const SEAL_SIZE = 0.04
const SEAL_ASPECT = 1.5417

const switchRefs = []
let junctionBoxImg = null
let junctionBoxRect = { x: 0, y: 0, w: 0, h: 0 }
let dropZoneRect = null
let switchesCompleted = false
let switchesCompletedStep = 0 // 完成标志按步骤隔离：步骤6 完成后不影响步骤10 的第二次检测
let wiresCompleted = false
let pendingDraft = null
// 步骤8 热区/元素引用
const cableDropZoneRect = []
const terminalRects = []
const meterTerminalRects = []
const coreTipRects = []
const coreTipLinks = [] // 已连芯线 Path（步骤8）
let signalCableImg = null
let signalCableRect = { x: 0, y: 0, w: 0, h: 0 }
let cableDone = false
let coreFollowPaths = []
let tieDropZoneRect = null
let tieImg = null
const sealRects = []
const sealImgs = []
let sealsDone = false

/** 是否盖盖（已接线/封闭）背景：此时导线需按被遮挡形态绘制（截断于盖板下缘） */
function isCoveredBg(url = currentBg.value) {
  return (
    url === Images.meteringRoomWired ||
    url === Images.meteringRoomWithCableTies ||
    url === Images.meteringRoomCovered
  )
}

function switchBackground(url) {
  currentBg.value = url
  if (!leafer) return
  const w = leafer.width
  const h = leafer.height
  bgLayer.removeAll()
  bgLayer.add(new Image({ url, x: 0, y: 0, width: w, height: h }))
  // 背景切换后按盖盖与否重绘导线（Wired 起已盖盖→被遮挡；步骤8 完成切 Wired 即生效）
  redrawConnectedWires(isCoveredBg(url))
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
  if (meterPlaced.value) return
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
  // 挂表后：右侧长方体 8 格 + 电表下方 6 芯连接热区（16 孔）出现
  buildTerminalBlocks()
  buildMeterTerminals()
  // 挂表后显示电表下方芯孔热区（接线完成后销毁）
  ensureHoles()
  persistState()
  emit('stepCompleted', props.stepOrder)
}

/** 点击热区以外（仅步骤5） */
function handleMiss() {
  if (props.stepOrder !== 5) return
  if (meterPlaced.value) return
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
      redrawCableCores()
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
    // 初始状态：步骤6 从闭合（on）开始；7-10 为步骤6 结束状态（target），
    // 内置在构建时保证开关就绪即状态正确，避免 watch 重置时序导致无法完成
    const initState = props.stepOrder >= 7 ? cfg.target : 'on'
    const pos = initState === 'on' ? cfg.on : cfg.off
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
    // 热区与开关图位置尺寸一致并同步旋转（zIndex 3），蓝色半透明便于调整定位
    // 热区生命周期：步骤5（挂表）起可点击 → 步骤6 结束后移除 → 步骤10（第二次调整）再出现
    let rect = null
    if (props.stepOrder === 5 || props.stepOrder === 6 || props.stepOrder === 10) {
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
    switchStates.value.push(initState)
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

/** 步骤5-10 且接线盒就绪时构建开关（首次构建，保留草稿恢复）；步骤11+ 接线盒已销毁 */
function ensureSwitches() {
  if (
    props.stepOrder >= 5 &&
    props.stepOrder < 11 &&
    switchRefs.length === 0 &&
    junctionBoxRect.w > 0
  ) {
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

/** 当前接线盒矩形：盖盖后接线盒图已销毁（背景自带端子），盒孔按 JUNCTION_BOX 常量推算兜底 */
function activeBoxRect() {
  if (junctionBoxRect && junctionBoxRect.w > 0) return junctionBoxRect
  const w = leafer.width
  const bw = w * JUNCTION_BOX.w
  return {
    x: w * JUNCTION_BOX.x,
    y: leafer.height * JUNCTION_BOX.y,
    w: bw,
    h: bw / (junctionBoxAspect || JUNCTION_BOX_ASPECT)
  }
}

/** 接线盒孔 i（1~13，右起）绝对坐标 */
function boxHolePos(i) {
  const jb = activeBoxRect()
  const rx =
    BOX_HOLES.x0 + ((BOX_HOLES.count - i) / (BOX_HOLES.count - 1)) * (BOX_HOLES.x1 - BOX_HOLES.x0)
  return {
    x: jb.x + rx * jb.w,
    y: jb.y + BOX_HOLES.y * jb.h
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

/** 挂表后构建孔热区（步骤5 挂表完成即显示，接线完成后销毁） */
function ensureHoles() {
  if (!meterPlaced.value || props.stepOrder < 5 || props.stepOrder > 7) return
  if (junctionBoxRect.w <= 0 || !leafer) return
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
  return activeBoxRect().w * 0.006
}

/** 接线盒→电表导线：生成「竖直-水平-竖直」直角折线 Path（非斜直线）。
 *  双色线为两条细线沿水平方向并排（dx 偏移）；covered=true 时按盖盖遮挡形态：
 *  线被盖板下缘截断（最高只到 WIRE_COVER_Y），隐藏被盖住部分。 */
function makeElbowPaths(from, to, wire, covered = false) {
  const thin = thinWireWidth()
  const w = wire.spec.startsWith('4.0') ? thin * 2 : thin
  // Z 形拐弯点：与未盖盖一致（基准 bendRatio + 同色组增量 elbowOffset），保证盖盖样式连续
  const bendRatio = WIRE_ELBOW.bendRatio + (wire.elbowOffset || 0)
  const bendY = to.y + (from.y - to.y) * (1 - bendRatio)
  // 盖盖：顶部被盖住的竖段截断到盖板下缘（WIRE_COVER_Y），隐藏伸入盖板的部分
  const coverY = WIRE_COVER_Y * leafer.height
  const pts = (dx, dy) => {
    const arr = [
      { x: from.x + dx, y: from.y + dy },
      { x: from.x + dx, y: bendY + dy },
      { x: to.x + dx, y: bendY + dy }
    ]
    arr.push({ x: to.x + dx, y: covered ? coverY : to.y + dy }) // 末段：盖盖截到盖板下缘 / 未盖盖入孔
    return arr
  }
  const mk = (list, color, width) =>
    new Path({
      path: 'M ' + list.map(p => `${p.x} ${p.y}`).join(' L '),
      stroke: color,
      strokeWidth: width,
      lineCap: 'round',
      lineJoin: 'round',
      zIndex: 4,
      hittable: false
    })
  if (!wire.secondColor) return [mk(pts(0, 0), wire.pathColor, w)]
  const off = thin / 2
  return [mk(pts(-off, 0), wire.pathColor, thin), mk(pts(off, 0), wire.secondColor, thin)]
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
  // 法向统一为屏幕绝对基准：pathColor 恒偏移在屏幕左侧（法向 x 分量 ≤ 0），
  // secondColor 在右侧——不随接线起点/方向变化（跟随与完成绘制一致）
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

/** 绘制一根导线（持久，zIndex 4 在孔热区之上；covered=true 为盖盖遮挡形态） */
function drawWirePath(boxHole, meterHole, wire, covered = false) {
  const from = boxHolePos(boxHole)
  const r = meterHoleRatio(meterHole)
  const to = { x: r.x * leafer.width, y: r.y * leafer.height }
  const paths = makeElbowPaths(from, to, wire, covered)
  paths.forEach(p => hitLayer.add(p))
  wirePaths.push(...paths)
}

/** 按已接导线重绘（画布重建/比例校正/盖盖后恢复视觉）；covered=true 画被遮挡形态 */
function redrawConnectedWires(covered = false) {
  wirePaths.forEach(p => p.remove())
  wirePaths.length = 0
  if (!leafer || activeBoxRect().w <= 0) return
  connectedWires.value.forEach(w => {
    const conn = WIRE_CONNECTIONS.find(c => c.boxHole === w.boxHole && c.meterHole === w.meterHole)
    if (conn) drawWirePath(w.boxHole, w.meterHole, conn, covered)
  })
}

/** 孔绝对像素坐标（盒孔=接线盒坐标换算；表孔=画布比率换算） */
function holeAbsPos(type, hole) {
  if (type === 'box') return boxHolePos(hole)
  const r = meterHoleRatio(hole)
  return { x: r.x * leafer.width, y: r.y * leafer.height }
}

/** 点孔：screwdriver_active → 记录起点并开始跟随；wire_drawing → 起点终点一起校验 */
// 起点固定为接线盒（box），终点为电表孔（meter）
function onHoleClick(type, hole) {
  if (props.stepOrder !== 7) return
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
    wireFollowPaths = makeElbowPaths(p, { x: p.x, y: p.y }, selectedWire.value)
    wireFollowPaths.forEach(ph => hitLayer.add(ph))
    leafer.on(PointerEvent.MOVE, onWireMove)
    return
  }
  if (wiringStep.value === 'wire_drawing') {
    const start = wireStart.value
    if (type === 'box') {
      ElMessage.warning('请点击电表侧的接线孔作为终点')
      emit('error')
      return
    }
    const boxHole = start.type === 'box' ? start.hole : hole
    const meterHole = start.type === 'meter' ? start.hole : hole
    stopWireFollow()
    const w = selectedWire.value
    // 防重复接线：同一配对已连接过则拒绝（否则存档/绘制会出现重复线）
    const already = connectedWires.value.some(
      x => x.boxHole === boxHole && x.meterHole === meterHole
    )
    if (w && boxHole === w.boxHole && meterHole === w.meterHole && !already) {
      drawWirePath(boxHole, meterHole, w)
      connectedWires.value.push({ boxHole, meterHole, spec: w.spec })
      persistState()
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
  const paths = makeElbowPaths(s, { x: p.x, y: p.y }, selectedWire.value)
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

// ─── 步骤8：6芯信号线 ───

/** 信号线图片绝对坐标（与放置热区对齐） */
function cableImgPos() {
  const w = leafer.width
  const h = leafer.height
  const cw = w * SIGNAL_CABLE.w
  const ch = cw * (718 / 521)
  return { x: w * CABLE_DROP_ZONE.x, y: h * CABLE_DROP_ZONE.y, w: cw, h: ch }
}

/** 芯点热区绝对坐标（相对信号线图片，每点独立 pos） */
function coreTipPos(side, idx) {
  const core = (side === 'right' ? CORE_TIPS.right : CORE_TIPS.left)[idx]
  return {
    x: signalCableRect.x + core.pos.x * signalCableRect.w,
    y: signalCableRect.y + core.pos.y * signalCableRect.h
  }
}

/** 步骤8 热区构建（接线盒就绪后即可；长方体8格/电表16孔生命周期不在此——全流程/挂表后构建） */
function ensureCable() {
  if (props.stepOrder !== 8 || !leafer || junctionBoxRect.w <= 0) return
  if (cableDone) return
  buildCableDropZone()
  if (cablePlaced.value) buildCoreTips()
}

// ─── 步骤9：扎带标识牌 ───

/** 步骤9 扎带热区（位置/大小与信号线放置热区一致） */
function buildTieDropZone() {
  if (props.stepOrder !== 9 || tiePlaced.value || tieDropZoneRect) return
  const w = leafer.width
  const h = leafer.height
  const rect = new Rect({
    x: w * CABLE_DROP_ZONE.x,
    y: h * CABLE_DROP_ZONE.y,
    width: w * CABLE_DROP_ZONE.w,
    height: h * CABLE_DROP_ZONE.h,
    fill: 'rgba(0, 150, 255, 0.25)',
    stroke: 'rgba(0, 150, 255, 0.9)',
    strokeWidth: 2,
    zIndex: 3
  })
  rect.on(PointerEvent.CLICK, () => onTieDropZoneClick())
  hitLayer.add(rect)
  tieDropZoneRect = rect
}

// ─── 步骤11：铅封 ───

/** 步骤11 铅封热区（5 处，蓝色半透明可视化；已放置位置跳过） */
function buildSealHotspots() {
  if (props.stepOrder !== 11 || sealsDone || sealRects.length > 0) return
  const w = leafer.width
  const h = leafer.height
  const sz = Math.max(w * SEAL_SIZE * 2, 20)
  SEALS.forEach((cfg, i) => {
    if (sealPlaced.value[i]) return
    const rect = new Rect({
      x: w * cfg.x - sz / 2,
      y: h * cfg.y - sz / 2,
      width: sz,
      height: sz,
      fill: 'rgba(0, 150, 255, 0.25)',
      stroke: 'rgba(0, 150, 255, 0.9)',
      strokeWidth: 1,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => onSealClick(i))
    hitLayer.add(rect)
    sealRects.push(rect)
  })
}

/** 点铅封热区：放置铅封小图（大小一致、独立旋转） */
function onSealClick(i) {
  if (props.stepOrder !== 11 || sealsDone) return
  if (!sealFollowing.value) {
    ElMessage.warning('请先在右侧工具栏选择铅封')
    emit('error')
    return
  }
  if (sealPlaced.value[i]) {
    ElMessage.warning('该位置已放置铅封')
    emit('error')
    return
  }
  const w = leafer.width
  const h = leafer.height
  const cfg = SEALS[i]
  const sz = w * SEAL_SIZE
  const img = new Image({
    url: Images.sealed,
    x: w * cfg.x - sz / 2,
    y: h * cfg.y - sz / SEAL_ASPECT / 2,
    width: sz,
    height: sz / SEAL_ASPECT,
    rotation: cfg.rotation,
    zIndex: 4
  })
  hitLayer.add(img)
  sealImgs.push(img)
  sealPlaced.value[i] = true
  persistState()
  emit('operation')
  if (sealPlaced.value.filter(Boolean).length === SEALS.length) {
    sealsDone = true
    sealFollowing.value = false
    sealRects.forEach(r => r.remove())
    sealRects.length = 0
    emit('stepCompleted', props.stepOrder)
  }
}

/** 重绘已放置铅封（画布重建后恢复视觉） */
function redrawSeals() {
  sealImgs.forEach(img => img.remove())
  sealImgs.length = 0
  // 不能以 sealsDone 拦截：restoreDraft 双保险会调用两次，第二次时 sealsDone 已为 true，
  // 若直接 return 会导致"先移除全部图标再跳过重绘"→ 铅封消失。重绘本身幂等，无需守卫
  if (!leafer) return
  const w = leafer.width
  const h = leafer.height
  const sz = w * SEAL_SIZE
  sealPlaced.value.forEach((placed, i) => {
    if (!placed) return
    const cfg = SEALS[i]
    const img = new Image({
      url: Images.sealed,
      x: w * cfg.x - sz / 2,
      y: h * cfg.y - sz / SEAL_ASPECT / 2,
      width: sz,
      height: sz / SEAL_ASPECT,
      rotation: cfg.rotation,
      zIndex: 4
    })
    hitLayer.add(img)
    sealImgs.push(img)
  })
}

/** 确认键：仅步骤11 铅封完成后可点（emit confirm 由 HCL 弹窗跳转） */
function onConfirmClick() {
  if (props.stepOrder === 11 && sealsDone) {
    emit('confirm')
    return
  }
  ElMessage.warning('请先完成当前步骤')
  emit('error')
}

/** 点击扎带热区（需扎带跟随中）→ 放置 + 切背景 + 提交 */
function onTieDropZoneClick() {
  if (props.stepOrder !== 9 || tiePlaced.value) return
  if (!tieFollowing.value) {
    ElMessage.warning('请先在右侧工具栏选择扎带标识牌')
    emit('error')
    return
  }
  tieFollowing.value = false
  tiePlaced.value = true
  persistState()
  tieDropZoneRect?.remove()
  tieDropZoneRect = null
  // 扎带小图（热区中心，占位尺寸）短暂显示
  const w = leafer.width
  const h = leafer.height
  const sz = w * CABLE_DROP_ZONE.w * CABLE_TIE_SIZE
  tieImg = new Image({
    url: Images.barCableTieLabel,
    x: w * CABLE_DROP_ZONE.x + (w * CABLE_DROP_ZONE.w - sz) / 2,
    y: h * CABLE_DROP_ZONE.y + (h * CABLE_DROP_ZONE.h - sz) / 2,
    width: sz,
    height: sz,
    zIndex: 4
  })
  hitLayer.add(tieImg)
  // 立即切背景（背景图已含扎带）并清理；完成提示由 HCL 统一弹出
  switchBackground(Images.meteringRoomWithCableTies)
  tieImg.remove()
  tieImg = null
  emit('stepCompleted', props.stepOrder)
}

/** 信号线放置热区（蓝色半透明可视化），放置后销毁 */
function buildCableDropZone() {
  if (cablePlaced.value || cableDropZoneRect.length > 0) return
  const w = leafer.width
  const h = leafer.height
  const rect = new Rect({
    x: w * CABLE_DROP_ZONE.x,
    y: h * CABLE_DROP_ZONE.y,
    width: w * CABLE_DROP_ZONE.w,
    height: h * CABLE_DROP_ZONE.h,
    fill: 'rgba(0, 150, 255, 0.25)',
    stroke: 'rgba(0, 150, 255, 0.9)',
    strokeWidth: 2,
    zIndex: 3
  })
  rect.on(PointerEvent.CLICK, () => onCableDropZoneClick())
  hitLayer.add(rect)
  cableDropZoneRect.push(rect)
}

/** 长方体 8 格热区（块内部左侧，全流程保留） */
function buildTerminalBlocks() {
  if (terminalRects.length > 0) return
  const w = leafer.width
  const h = leafer.height
  const bx = w * TERMINAL_BLOCK.x
  const by = h * TERMINAL_BLOCK.y
  const bw = w * TERMINAL_BLOCK.w
  const bh = h * TERMINAL_BLOCK.h
  const rowH = bh / 8
  TERMINAL_BLOCK.names.forEach((name, i) => {
    const rect = new Rect({
      x: bx,
      y: by + i * rowH,
      width: bw * 0.4,
      height: rowH,
      fill: 'rgba(0, 150, 255, 0.25)',
      stroke: 'rgba(0, 150, 255, 0.9)',
      strokeWidth: 1,
      strokeAlign: 'inside', // 描边向内，相邻格边界不重叠成双线
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => onTerminalClick(i + 1))
    hitLayer.add(rect)
    terminalRects.push({ rect, name, num: i + 1 })
  })
}

/** 电表 16 孔热区（编号 13-28，同一水平线） */
function buildMeterTerminals() {
  if (meterTerminalRects.length > 0) return
  const w = leafer.width
  const h = leafer.height
  const y = h * METER_TERMINALS.y
  // 热区大小与调试值（w * size）一致，不加最小下限
  const sz = w * METER_TERMINALS.size
  let x = w * METER_TERMINALS.x0
  for (let n = 13; n <= 28; n++) {
    const rect = new Rect({
      x: x - sz / 2,
      y: y - sz / 2,
      width: sz,
      height: sz,
      fill: 'rgba(0, 150, 255, 0.25)',
      stroke: 'rgba(0, 150, 255, 0.9)',
      strokeWidth: 1,
      zIndex: 3
    })
    rect.on(PointerEvent.CLICK, () => onMeterTerminalClick(n))
    hitLayer.add(rect)
    meterTerminalRects.push({ rect, num: n })
    // 间隔：13→14、14→15 两个大间隔（13-15 三个一组）；15→16 中间间隔；16→28 小间隔
    if (n === 13) x += w * METER_TERMINALS.gapBig
    else if (n === 14) x += w * METER_TERMINALS.gapBig
    else if (n === 15) x += w * METER_TERMINALS.gapMid
    else if (n >= 16) x += w * METER_TERMINALS.gapSmall
  }
}

/** 芯点热区（信号线图片上，12 个） */
function buildCoreTips() {
  if (coreTipRects.length > 0) return
  const sz = signalCableRect.w * CORE_TIP_SIZE
  const mk = (side, idx) => {
    const p = coreTipPos(side, idx)
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
    rect.on(PointerEvent.CLICK, () => onCoreTipClick(side, idx))
    hitLayer.add(rect)
    coreTipRects.push({ rect, side, idx })
  }
  CORE_TIPS.right.forEach((_, i) => mk('right', i))
  CORE_TIPS.left.forEach((_, i) => mk('left', i))
}

/** 步骤8 完成：销毁电表孔/芯点热区、已连芯线与信号线图片（长方体热区保留） */
function destroyCableHoles() {
  meterTerminalRects.forEach(t => t.rect.remove())
  meterTerminalRects.length = 0
  coreTipRects.forEach(t => t.rect.remove())
  coreTipRects.length = 0
  coreTipLinks.forEach(p => p.remove())
  coreTipLinks.length = 0
  signalCableImg?.remove()
  signalCableImg = null
}

/** 点击放置热区（需信号线跟随中） */
function onCableDropZoneClick() {
  if (props.stepOrder !== 8) return
  if (cablePlaced.value) return
  if (!cableFollowing.value) {
    ElMessage.warning('请先在右侧工具栏选择6芯信号线')
    emit('error')
    return
  }
  cableFollowing.value = false
  cablePlaced.value = true
  cableDropZoneRect.forEach(r => r.remove())
  cableDropZoneRect.length = 0
  signalCableRect = cableImgPos()
  signalCableImg = new Image({
    url: Images.strippedSignalCable6Core,
    x: signalCableRect.x,
    y: signalCableRect.y,
    width: signalCableRect.w,
    height: signalCableRect.h,
    zIndex: 2
  })
  hitLayer.add(signalCableImg)
  buildCoreTips()
  persistState()
}

/** 点芯点：选芯并开始跟随（右端阶段只响应右端芯） */
function onCoreTipClick(side, idx) {
  if (props.stepOrder !== 8 || !cablePlaced.value) return
  if (signalStep.value === 'core_following') return
  if (connectedCores.value.some(c => c.side === side && c.idx === idx)) {
    ElMessage.warning('该芯已连接')
    emit('error')
    return
  }
  if (phase.value === 'right' && side !== 'right') {
    ElMessage.warning('请先完成长方体端的接线')
    emit('error')
    return
  }
  const core = (side === 'right' ? CORE_TIPS.right : CORE_TIPS.left)[idx]
  selectedCore.value = { side, idx, core }
  signalStep.value = 'core_following'
  const p = coreTipPos(side, idx)
  coreFollowPaths = makeWirePaths(p, { x: p.x, y: p.y }, { spec: '2.5', pathColor: core.color })
  coreFollowPaths.forEach(ph => hitLayer.add(ph))
  leafer.on(PointerEvent.MOVE, onCoreMove)
}

/** 点长方体格（1-8）：校验芯对应孔号 */
function onTerminalClick(num) {
  if (props.stepOrder !== 8 || signalStep.value !== 'core_following') return
  const sel = selectedCore.value
  stopCoreFollow()
  if (sel.side !== 'right' || sel.core.terminal !== num) {
    ElMessage.warning('接线位置错误')
    emit('error')
    resetCable()
    return
  }
  connectCore(sel, num)
}

/** 点电表孔（13-28）：校验芯对应孔号 */
function onMeterTerminalClick(num) {
  if (props.stepOrder !== 8 || signalStep.value !== 'core_following') return
  const sel = selectedCore.value
  stopCoreFollow()
  if (sel.side !== 'left' || sel.core.terminal !== num) {
    ElMessage.warning('接线位置错误')
    emit('error')
    resetCable()
    return
  }
  connectCore(sel, num)
}

/** 芯连接成功：落定细线 + 记录 + 阶段/完成检查 */
function connectCore(sel, terminal) {
  const from = coreTipPos(sel.side, sel.idx)
  const to = sel.side === 'right' ? terminalAbsPos(terminal) : meterTerminalAbsPos(terminal)
  const paths = makeWirePaths(from, to, { spec: '2.5', pathColor: sel.core.color })
  paths.forEach(p => hitLayer.add(p))
  coreTipLinks.push(...paths)
  connectedCores.value.push({ side: sel.side, idx: sel.idx, terminal, color: sel.core.color })
  emit('operation')
  resetCable()
  // 右端 6 根完成 → 进入左端阶段
  if (
    phase.value === 'right' &&
    connectedCores.value.filter(c => c.side === 'right').length === 6
  ) {
    phase.value = 'left'
    ElMessage.info('长方体端接线完成，请开始连接电表端')
  }
  // 阶段切换完成后再持久化（否则 localStorage 存的是旧 phase，恢复时以最高优先级覆盖草稿）
  persistState()
  // 12 条全部完成
  if (connectedCores.value.length === 12) {
    finishSignalCable()
  }
}

/** 长方体格连接点（热区左边缘中点） */
function terminalAbsPos(num) {
  const w = leafer.width
  const h = leafer.height
  const rowH = (h * TERMINAL_BLOCK.h) / 8
  return {
    x: w * TERMINAL_BLOCK.x,
    y: h * TERMINAL_BLOCK.y + (num - 1) * rowH + rowH / 2
  }
}

/** 电表孔绝对坐标（按间隔计算） */
function meterTerminalAbsPos(num) {
  const w = leafer.width
  const h = leafer.height
  let x = w * METER_TERMINALS.x0
  for (let n = 13; n < num; n++) {
    if (n === 13) x += w * METER_TERMINALS.gapBig
    else if (n === 14) x += w * METER_TERMINALS.gapBig
    else if (n === 15) x += w * METER_TERMINALS.gapMid
    else if (n >= 16) x += w * METER_TERMINALS.gapSmall
  }
  return { x, y: h * METER_TERMINALS.y }
}

function resetCable() {
  signalStep.value = 'idle'
  selectedCore.value = null
}

function stopCoreFollow() {
  leafer.off(PointerEvent.MOVE, onCoreMove)
  coreFollowPaths.forEach(ph => ph.remove())
  coreFollowPaths = []
}

function onCoreMove(e) {
  if (coreFollowPaths.length === 0 || !selectedCore.value) return
  const p = e.getLocalPoint()
  const from = coreTipPos(selectedCore.value.side, selectedCore.value.idx)
  const paths = makeWirePaths(
    from,
    { x: p.x, y: p.y },
    { spec: '2.5', pathColor: selectedCore.value.core.color }
  )
  coreFollowPaths.forEach((ph, i) => {
    if (paths[i]) ph.path = paths[i].path
  })
}

/** 6 芯两端全接完：停顿（让用户看到接线效果）→ 切 Wired 背景 → 销毁热区 → 提交 */
// （接线完成销毁电表 16 孔热区：destroyCableHoles 内清理 meterTerminalRects；长方体 8 格全流程保留）
function finishSignalCable() {
  if (cableDone) return
  cableDone = true
  persistState()
  setTimeout(() => {
    switchBackground(Images.meteringRoomWired)
    destroyCableHoles()
    emit('stepCompleted', props.stepOrder)
  }, 1500)
}

/** 重绘已连芯线（画布重建后恢复视觉） */
function redrawCableCores() {
  coreTipLinks.forEach(p => p.remove())
  coreTipLinks.length = 0
  // 不能以 cableDone 拦截：restoreDraft 双保险第二次调用时 cableDone 已为 true，
  // 直接 return 会导致"先移除连线再跳过重绘"→ 连线消失。重绘本身幂等，无需守卫
  if (!cablePlaced.value) return
  connectedCores.value.forEach(c => {
    const core = (c.side === 'right' ? CORE_TIPS.right : CORE_TIPS.left)[c.idx]
    const from = coreTipPos(c.side, c.idx)
    const to = c.side === 'right' ? terminalAbsPos(c.terminal) : meterTerminalAbsPos(c.terminal)
    const paths = makeWirePaths(from, to, { spec: '2.5', pathColor: core.color })
    paths.forEach(p => hitLayer.add(p))
    coreTipLinks.push(...paths)
  })
}

/** 步骤7 接线孔热区销毁（进入步骤8 时清理） */
function destroyBoxHoles() {
  boxHoleRects.forEach(r => r.remove())
  boxHoleRects.length = 0
  meterHoleRects.forEach(r => r.remove())
  meterHoleRects.length = 0
}

/** 画布级移动：悬浮显示孔位信息（热区上方）
 *  8 格长方体：全流程浮窗（5-11）；16 孔：挂表后-接线完成前浮窗（5-8） */
function onCanvasMove(e) {
  const p = e.getLocalPoint()
  for (const t of terminalRects) {
    if (isPointInRect(p, t.rect)) {
      showTooltip(t.name, t.rect)
      return
    }
  }
  if (meterPlaced.value && props.stepOrder <= 8) {
    for (const t of meterTerminalRects) {
      if (isPointInRect(p, t.rect)) {
        showTooltip(`孔 ${t.num}`, t.rect)
        return
      }
    }
  }
  tooltipVisible.value = false
}

function isPointInRect(p, rect) {
  return p.x >= rect.x && p.x <= rect.x + rect.width && p.y >= rect.y && p.y <= rect.y + rect.height
}

/** 悬浮层显示在热区上方居中 */
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
  if (props.stepOrder !== 5 && props.stepOrder !== 6 && props.stepOrder !== 10) return
  emit('operation')
  const s = switchRefs[i]
  if (!s) return
  const cur = switchStates.value[i]
  const next = cur === 'on' ? 'off' : 'on'
  switchStates.value[i] = next
  moveSwitch(s, next)
  // 每次切换即持久化，保证 localStorage 兜底是最新状态（否则旧值会以最高优先级覆盖草稿）
  persistState()
  checkSwitches()
}

/** 全部开关达到目标状态 → 提交（步骤6/10），移除开关热区；步骤10 额外销毁接线盒/开关并切 Covered 背景 */
function checkSwitches() {
  if (props.stepOrder !== 6 && props.stepOrder !== 10) return
  // 完成标志按步骤隔离：步骤6 完成置 true 后，步骤10 重新检测不被拦截
  if (switchesCompleted && switchesCompletedStep === props.stepOrder) return
  const targets = props.stepOrder === 10 ? SWITCH_TARGETS_2 : SWITCHES.map(s => s.target)
  const allOk = targets.every((t, i) => switchStates.value[i] === t)
  if (allOk) {
    switchesCompleted = true
    switchesCompletedStep = props.stepOrder
    persistState()
    // 移除热区并置 null：步骤9→10 需重建热区，watch 以 rect 为空判断
    switchRefs.forEach(s => {
      s.rect?.remove()
      s.rect = null
    })
    if (props.stepOrder === 10) {
      // 盖盖：销毁接线盒/开关图，切换 Covered 背景（switchBackground 内部按盖盖重绘被遮挡导线）
      switchRefs.forEach(s => s.img.remove())
      switchRefs.length = 0
      junctionBoxImg?.remove()
      junctionBoxImg = null
      switchBackground(Images.meteringRoomCovered)
    }
    emit('stepCompleted', props.stepOrder)
  }
}

/** 恢复开关状态（位置/视觉）与已接导线 */
function applyDraft(d) {
  // 右侧长方体 8 格：画布全流程存在（5-11）
  buildTerminalBlocks()
  // 电表下方 6 芯连接热区（16 孔）：挂表后出现，接线完成（12 根全连）销毁
  if (meterPlaced.value && props.stepOrder <= 8 && connectedCores.value.length < 12) {
    buildMeterTerminals()
  }
  // 回档到挂表后状态时补构建芯孔热区（挂表后-接线完成前显示）
  ensureHoles()
  if (Array.isArray(d?.switchStates)) {
    d.switchStates.forEach((v, i) => {
      if (!switchRefs[i]) return
      switchStates.value[i] = v
      moveSwitch(switchRefs[i], v)
    })
  }
  if (Array.isArray(d?.connectedWires) && d.connectedWires.length > 0) {
    connectedWires.value = d.connectedWires.map(w => ({
      boxHole: w.boxHole,
      meterHole: w.meterHole,
      spec: w.spec
    }))
    redrawConnectedWires(isCoveredBg())
  }
  // 步骤8：恢复信号线/阶段/已连芯线（仅步骤8 有效；步骤9+ 信号线已销毁不应重建）
  if (d?.cablePlaced && props.stepOrder === 8) {
    cablePlaced.value = true
    // 恢复已放置状态时移除放置热区（画布先于草稿构建，需同步清理）
    cableDropZoneRect.forEach(r => r.remove())
    cableDropZoneRect.length = 0
    phase.value = d.phase === 'left' ? 'left' : 'right'
    signalCableRect = cableImgPos()
    signalCableImg = new Image({
      url: Images.strippedSignalCable6Core,
      x: signalCableRect.x,
      y: signalCableRect.y,
      width: signalCableRect.w,
      height: signalCableRect.h,
      zIndex: 2
    })
    hitLayer.add(signalCableImg)
    buildCoreTips()
    if (Array.isArray(d?.connectedCores) && d.connectedCores.length > 0) {
      connectedCores.value = d.connectedCores.map(c => ({ ...c }))
      redrawCableCores()
      if (connectedCores.value.length === 12) {
        cableDone = true
        switchBackground(Images.meteringRoomWired)
        destroyCableHoles()
        // 回档到完成态（12 根全连）：停顿后自动提交（与 finishSignalCable 一致，避免无热区卡死）
        setTimeout(() => emit('stepCompleted', props.stepOrder), 1000)
      }
    }
  }
  // 步骤9：恢复扎带放置状态（背景切换为已绑扎带）
  if (d?.tiePlaced && props.stepOrder === 9) {
    tiePlaced.value = true
    switchBackground(Images.meteringRoomWithCableTies)
  }
  // 步骤11：恢复铅封放置状态（画布先于草稿构建，需同步重绘）
  if (Array.isArray(d?.sealPlaced) && d.sealPlaced.some(Boolean)) {
    sealPlaced.value = d.sealPlaced
    sealRects.forEach(r => r.remove())
    sealRects.length = 0
    redrawSeals()
    // 重建未放置位置的热区（已放置位置由 buildSealHotspots 跳过）
    buildSealHotspots()
    if (sealPlaced.value.filter(Boolean).length === SEALS.length) {
      sealsDone = true
      emit('stepCompleted', props.stepOrder)
    }
  }
  // 恢复完成后统一刷新已绘制导线（与比例校正竞态下的双保险）
  redrawConnectedWires(isCoveredBg())
  redrawCableCores()
}

/** 确认键按画布实际像素绝对定位（宽 18% 画布宽，右下角距右缘 4%、下缘下 4%） */
function updateConfirmBtn(w, h) {
  const size = Math.round(w * 0.18)
  confirmBtnStyle.value = {
    width: size + 'px',
    height: size + 'px',
    left: Math.round(w * 0.96 - size) + 'px',
    top: Math.round(h * 1.04 - size) + 'px'
  }
}

async function createCanvas() {
  const img = bgImgRef.value
  if (!img) return
  const r = img.getBoundingClientRect()
  const w = Math.round(r.width)
  const h = Math.round(r.height)
  canvasStyle.value = { width: w + 'px', height: h + 'px' }
  updateConfirmBtn(w, h)
  leafer = new Leafer({ view: leaferViewRef.value, width: w, height: h })
  bgLayer = new Group()
  hitLayer = new Group()
  leafer.add(bgLayer)
  leafer.add(hitLayer)
  bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
  bindEvents(w, h)
  leafer.on(PointerEvent.MOVE, onCanvasMove)
  buildDropZone(w, h)
  if (props.stepOrder < 11) buildJunctionBox(w, h)
  ensureCable()
  buildTieDropZone()
  buildSealHotspots()
  redrawSeals()
  // 画布就绪后统一应用草稿（步骤11 不构建接线盒，pendingDraft 不再依赖 ensureSwitches）
  if (pendingDraft) {
    applyDraft(pendingDraft)
    pendingDraft = null
  }
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

/** 步骤8：6芯信号线选择 → 跟随放置 */
function onCableToolClick(idx, e) {
  if (idx !== 16) {
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  if (cablePlaced.value) {
    ElMessage.warning('6芯信号线已放置')
    emit('error')
    return
  }
  cableFollowing.value = true
  cableFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
}

/** 步骤9：扎带标识牌选择 → 跟随放置 */
function onTieToolClick(idx, e) {
  if (idx !== 13) {
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  if (tiePlaced.value) return
  tieFollowing.value = true
  tieFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
}

/** 步骤11：铅封选择 → 跟随放置 */
function onSealToolClick(idx, e) {
  if (idx !== 5) {
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  if (sealsDone) return
  sealFollowing.value = true
  sealFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
}

/** 右侧工具栏点击：智能电表 → 启动跟随，其余工具占位提示 */
function onRightToolClick(idx, e) {
  emit('operation')
  if (props.stepOrder === 11) {
    onSealToolClick(idx, e)
    return
  }
  if (props.stepOrder === 9) {
    onTieToolClick(idx, e)
    return
  }
  if (props.stepOrder === 8) {
    onCableToolClick(idx, e)
    return
  }
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
  if (cableFollowing.value) {
    cableFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  }
  if (tieFollowing.value) {
    tieFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  }
  if (sealFollowing.value) {
    sealFollowStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  }
}

/** 全量状态（存档用）：画布所有状态字段（localStorage 兜底内容一致） */
function getFullState() {
  return {
    stepOrder: props.stepOrder, // 标记存档所属步骤，恢复时用于丢弃跨步骤残留
    meterPlaced: meterPlaced.value,
    switchStates: [...switchStates.value],
    connectedWires: connectedWires.value.map(w => ({ ...w })),
    cablePlaced: cablePlaced.value,
    phase: phase.value,
    connectedCores: connectedCores.value.map(c => ({ ...c })),
    tiePlaced: tiePlaced.value,
    // 铅封放置状态归一化为定长数组（部分放置时为稀疏数组，JSON 序列化后长度不一致）
    sealPlaced: Array.from({ length: SEALS.length }, (_, i) => !!sealPlaced.value[i])
  }
}

/** 标准结果推断：进入步骤 order 时前序步骤成果（非当前步骤信息不依赖存档，按确定常量恢复） */
function standardStateForStep(order) {
  const s = {}
  if (order >= 6) s.meterPlaced = true // 步骤5 挂表完成
  if (order >= 7) s.switchStates = SWITCHES.map(x => x.target) // 步骤6 结束状态（第一次调整完成）
  if (order >= 8) {
    // 步骤7 结束：7 根进出线全连（固定配对）
    s.connectedWires = WIRE_CONNECTIONS.map(w => ({
      boxHole: w.boxHole,
      meterHole: w.meterHole,
      spec: w.spec
    }))
  }
  if (order >= 9) {
    // 步骤8 结束：6 芯信号线已放置并全连（固定配对）
    s.cablePlaced = true
    s.phase = 'left'
    s.connectedCores = []
      ;['right', 'left'].forEach(side => {
        CORE_TIPS[side].forEach((core, idx) => {
          s.connectedCores.push({ side, idx, terminal: core.terminal, color: core.color })
        })
      })
  }
  if (order >= 10) s.tiePlaced = true // 步骤9 结束：扎带已放（靠背景图体现）
  if (order >= 11) s.switchStates = [...SWITCH_TARGETS_2] // 步骤10 结束（第二次调整完成）
  return s
}

// localStorage 全量兜底（双保险）：操作成功时即时持久化，恢复时优先合并
const LS_KEY = () => 'meteringRoom_' + props.experimentId
function persistState() {
  try {
    localStorage.setItem(LS_KEY(), JSON.stringify(getFullState()))
  } catch (_) { }
}

// 当前步骤可交互字段（草稿/local 仅影响这些；其余前序结果字段强制按标准推断，防旧草稿污染固定结果）
const CURRENT_STEP_FIELDS = {
  5: ['meterPlaced'],
  6: ['switchStates'],
  7: ['connectedWires'],
  8: ['cablePlaced', 'phase', 'connectedCores'],
  9: ['tiePlaced'],
  10: ['switchStates'],
  11: ['sealPlaced']
}
function restoreDraft(d) {
  // 恢复优先级：标准结果推断（前序）→ 后端草稿（当前步骤中途态）→ localStorage（最新全量，优先）
  // 跨步骤残留防护：local/草稿只有步骤与当前一致才合并（否则是旧步骤数据，会导致误判完成态）
  let local = {}
  try {
    local = JSON.parse(localStorage.getItem(LS_KEY()) || '{}')
  } catch (_) { }
  if (local.stepOrder && local.stepOrder !== props.stepOrder) local = {}
  if (d?.stepOrder && d.stepOrder !== props.stepOrder) d = {}
  const merged = { ...standardStateForStep(props.stepOrder), ...(d || {}), ...local }
  // 前序结果字段强制：非当前步骤交互字段一律按标准推断覆盖（如 7+ 开关=步骤6 结束状态）
  const std = standardStateForStep(props.stepOrder)
  const cur = CURRENT_STEP_FIELDS[props.stepOrder] || []
  Object.keys(std).forEach(k => {
    if (!cur.includes(k)) merged[k] = std[k]
  })
  if (merged.meterPlaced) {
    meterPlaced.value = true
    // 步骤5 回档：保留挂表热区供再次点击提交（单步完成态，用户未提交时靠热区补提交）；步骤6+ 移除
    if (props.stepOrder >= 6) {
      dropZoneRect?.remove()
      dropZoneRect = null
    }
    // 背景按步骤推断（步骤5 回档也需 WithMeter 背景，避免看起来像未挂表）
    if (props.stepOrder >= 5) switchBackground(bgForStep(props.stepOrder))
  }
  // 画布就绪即可恢复（步骤11 无接线盒，junctionBoxRect.w 恒为 0，不能作为就绪条件）
  if (leafer) {
    applyDraft(merged)
    // 回档到完成态（标准推断/草稿恢复后已符合目标）→ 直接完成提交
    if (props.stepOrder === 6 || props.stepOrder === 10) checkSwitches()
    if (props.stepOrder === 7) checkWires() // checkWires 内部有 wiresCompleted 防重
    if (props.stepOrder === 9 && tiePlaced.value) {
      setTimeout(() => emit('stepCompleted', props.stepOrder), 500)
    }
  } else {
    pendingDraft = merged
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
    // 背景图随画布尺寸重铺
    bgLayer.removeAll()
    bgLayer.add(new Image({ url: currentBg.value, x: 0, y: 0, width: w, height: h }))
    // 步骤5-10：重建热区/接线盒/开关/孔热区（按当前状态保持视觉位置）
    if (props.stepOrder >= 5 && props.stepOrder < 11) {
      const saved = [...switchStates.value]
      hitLayer.removeAll()
      switchRefs.length = 0
      boxHoleRects.length = 0
      meterHoleRects.length = 0
      wirePaths.length = 0
      cableDropZoneRect.length = 0
      terminalRects.length = 0
      meterTerminalRects.length = 0
      coreTipRects.length = 0
      coreTipLinks.length = 0
      signalCableImg = null
      tieDropZoneRect = null
      tieImg = null
      sealRects.length = 0
      sealImgs.length = 0
      buildDropZone(w, h)
      buildJunctionBox(w, h)
      // 右侧长方体 8 格：画布全流程存在；电表 16 孔：挂表后出现，接线完成（12 根全连）销毁
      buildTerminalBlocks()
      if (meterPlaced.value && props.stepOrder <= 8 && connectedCores.value.length < 12) {
        buildMeterTerminals()
      }
      ensureCable()
      buildTieDropZone()
      buildSealHotspots()
      redrawSeals()
      // 信号线图片重建（hitLayer.removeAll 后需重新放置）
      if (props.stepOrder === 8 && cablePlaced.value && !signalCableImg && !cableDone) {
        signalCableRect = cableImgPos()
        signalCableImg = new Image({
          url: Images.strippedSignalCable6Core,
          x: signalCableRect.x,
          y: signalCableRect.y,
          width: signalCableRect.w,
          height: signalCableRect.h,
          zIndex: 2
        })
        hitLayer.add(signalCableImg)
      }
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
      redrawCableCores()
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
        // 前序步骤成果由 standardStateForStep 按标准推断，草稿只含当前步骤中途态；
        // 无论后端是否有草稿都执行恢复（restoreDraft 内部合并标准推断 + 草稿 + localStorage 兜底）
        restoreDraft(d || {})
      })
      .catch(() => { })
  }
})
// 同组件导航时组件不重新挂载，需监听步骤变化构建开关/孔热区
watch(
  () => props.stepOrder,
  order => {
    // 进入步骤6+ 且未挂表（异常跳转）→ 补上已挂表状态（背景按步骤推断）
    if (order >= 6 && !meterPlaced.value) {
      meterPlaced.value = true
      switchBackground(bgForStep(order))
    }
    if (order >= 5 && order < 11 && leafer) {
      // 步骤6/10：确保开关热区存在（步骤6 完成移除热区后置 null，9→10 跳转时需重建）
      if (order === 6 || order === 10) {
        if (switchRefs.length === 0) {
          ensureSwitches()
        } else if (!switchRefs[0].rect) {
          rebuildSwitches()
        }
        // 步骤10：无论重建路径如何（onResize 等可能残留全 on 状态），强制重置为步骤6 结束状态
        if (order === 10) {
          switchStates.value = SWITCHES.map(s => s.target)
          switchRefs.forEach((s, i) => moveSwitch(s, switchStates.value[i]))
        }
        // 已符合目标（如回档时已调整过）直接完成
        checkSwitches()
      }
      // 步骤7→8：清理步骤7 接线孔热区，构建步骤8 热区
      if (order === 8) {
        destroyBoxHoles()
      }
      ensureSwitches()
      ensureHoles()
      ensureCable()
      buildTieDropZone()
    }
    // 步骤11：构建铅封热区/重绘已放置铅封
    if (order === 11 && leafer) {
      buildSealHotspots()
      redrawSeals()
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
  getFullState,
  restoreDraft,
  activeToolIdxs
})
</script>

<style scoped>
.metering-canvas-wrap {
  position: fixed;
  left: 12vw;
  right: 12vw;
  /* 上移下移缩小画布，顶部预留空间放提示栏等元素 */
  top: 20vh;
  bottom: 5vh;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

/* 终端编号提示面板：CSS 百分比相对画布定位（宽 90% 画布、水平居中、贴画布顶边画布上方），
   内部元素以 cqw 相对本容器缩放 */
.terminal-guide-overlay {
  position: absolute;
  left: 10%;
  /* (100% - 90%) / 2，与 width 联动 */
  bottom: 100%;
  width: 90%;
  container-type: inline-size;
  z-index: 6;
  pointer-events: none;
}

.canvas-stage {
  position: relative;
  display: inline-block;
  max-width: 100%;
  max-height: 100%;
}

.bg-img {
  max-width: 66vw;
  max-height: 65vh;
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

.hole-tooltip {
  position: fixed;
  z-index: 1000;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
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
