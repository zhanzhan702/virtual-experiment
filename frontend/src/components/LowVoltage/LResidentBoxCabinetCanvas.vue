<!-- 居民表箱内部 Leafer 画布：步骤 5 断电挂牌 / 6 挂表接线 / 7 上电封盖 -->
<template>
  <div class="canvas-wrap">
    <div class="canvas-stage">
      <img
        ref="bgImgRef"
        :src="Images.residentBoxInnerBg"
        alt="居民表箱内部"
        class="bg-img"
        draggable="false"
        @load="onBgLoad"
      />
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
    </div>
    <!-- 右栏工具（电表/铅封）的鼠标跟随图，DOM 层不进入画布 -->
    <img
      v-if="followerSrc"
      :src="followerSrc"
      class="follower"
      alt=""
      :style="followerStyle"
      draggable="false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Leafer, Group, Image, Path, Rect, PointerEvent } from 'leafer-ui'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import { getStepDraft } from '@/api/experiment'

// ★★★ 坐标常量（相对画布百分比，按需逐个调整） ★★★
// 空开：1/2 各 4 个横排，3 号 1 个，从下到上
const BREAKER_DEFS = [
  { id: '1-1', group: 1, x: 22.1, y: 70, w: 5.5 },
  { id: '1-2', group: 1, x: 26.7, y: 70, w: 5.5 },
  { id: '1-3', group: 1, x: 31.3, y: 70, w: 5.5 },
  { id: '1-4', group: 1, x: 35.9, y: 70, w: 5.5 },
  { id: '2-1', group: 2, x: 23.7, y: 48.7, w: 3 },
  { id: '2-2', group: 2, x: 26.2, y: 48.7, w: 3 },
  { id: '2-3', group: 2, x: 28.7, y: 48.7, w: 3 },
  { id: '2-4', group: 2, x: 31.2, y: 48.7, w: 3 },
  { id: '3', group: 3, x: 21.9, y: 15, w: 13 }
]
// 空开图片宽高比（宽/高），用于按宽度换算高度
const BREAKER_RATIO = { 1: 674 / 1333, 2: 367 / 1215, 3: 789 / 1239 }
// 标识牌：悬挂在 3 号空开上（相对画布 %）
const SIGN_STYLE = { x: 23.7, y: 38, w: 9 }
// 4 个表位的左上角与宽度（相对画布 %），按左上→右上→左下→右下编号 1~4
const METER_SLOTS = [
  { x: 60.5, y: 9.5, w: 15.5 },
  { x: 78, y: 9.5, w: 15.5 },
  { x: 60.5, y: 51, w: 15.5 },
  { x: 78, y: 51, w: 15.5 }
]
// 16 个线头热区：下标 = 表序号 * 4 + 线序（0 = 最右）
// 顺序必须按「从右往左」排列：下标 0 = 每表最右侧的线（判定按 0→3 接线）
const WIRE_START_POS = [
  { x: 71.22, y: 37 },
  { x: 69.15, y: 37 },
  { x: 66.7, y: 37.3 },
  { x: 64.3, y: 37.5 },
  { x: 87.4, y: 38 },
  { x: 85.3, y: 38 },
  { x: 82.5, y: 38 },
  { x: 80.4, y: 38 },
  { x: 71.5, y: 79.6 },
  { x: 69.2, y: 79.6 },
  { x: 66.8, y: 79.6 },
  { x: 64.2, y: 79.6 },
  { x: 87.3, y: 79.6 },
  { x: 85.1, y: 79.6 },
  { x: 82.8, y: 79.6 },
  { x: 80.4, y: 79.6 }
]
// 热区尺寸（相对画布宽 %）：线头 / 端子孔（导线端点取热区中心）
const WIRE_HOTSPOT = 1.5
const HOLE_HOTSPOT = 1.4
// 导线粗细（相对画布宽的比率，0.006 约为原线宽的两倍，可直接微调）
const WIRE_STROKE_RATIO = 0.005
// 每表 4 个端子孔：相对电表左上角的比率（0~1），从右往左编号
const HOLE_RATIO = [
  { x: 0.69, y: 0.87 },
  { x: 0.54, y: 0.87 },
  { x: 0.39, y: 0.87 },
  { x: 0.26, y: 0.87 }
]
// 线色（只做视觉）：表 1/2 黄黄黑黑，表 3 绿绿黑黑，表 4 红红黑黑（从右往左）
const WIRE_COLORS = [
  ['#111', '#111', '#f5c518', '#f5c518'],
  ['#111', '#111', '#f5c518', '#f5c518'],
  ['#111', '#111', '#2e9d4f', '#2e9d4f'],
  ['#111', '#111', '#d23f3f', '#d23f3f']
]
// 表盖热区：相对电表左上角（0~1）
const COVER_RATIO = { x: 0.1, y: 0.75, w: 0.8, h: 0.2 }
// 合盖后导线截断位置（相对电表高 0~1）：只画到盖子下缘，进入盖子的线段隐藏
// 需落在导线路径上（端子孔约 0.87 与线头之间），一般取 0.9~1.0
const COVER_CUT_RATIO = 0.97
// 每表 2 处铅封：相对电表左上角（0~1）
const SEAL_RATIOS = [
  { x: 0.38, y: 0.83 },
  { x: 0.76, y: 0.57 }
]

const props = defineProps({
  stage: { type: Number, required: true },
  disabled: { type: Boolean, default: false },
  leftTool: { type: Number, default: null },
  rightTool: { type: String, default: null },
  // 用于 HMR 重新挂载时自行恢复草稿（父组件的 handleRestore 不会重跑）
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' }
})
const emit = defineEmits(['operation', 'error', 'complete', 'box-done', 'tool-used'])

const bgImgRef = ref(null)
const leaferViewRef = ref(null)
const canvasStyle = ref({})

// ── 右栏工具的鼠标跟随图（电表 / 铅封） ──
const cursor = reactive({ x: 0, y: 0 })

const followerSrc = computed(() => {
  if (props.disabled) return null
  if (props.stage === 6 && props.rightTool === 'meter') return Images.barSinglePhaseMeter
  if (props.stage === 7 && props.rightTool === 'seal') return Images.barSeal
  return null
})

const followerStyle = computed(() => ({ left: `${cursor.x}px`, top: `${cursor.y}px` }))

function onWindowMouseMove(e) {
  if (!followerSrc.value) return
  cursor.x = e.clientX
  cursor.y = e.clientY
}

let leafer = null
let bgLayer = null
let breakerLayer = null
let meterLayer = null
let wireLayer = null
let sealLayer = null
let hitLayer = null
let resizeTimer = null
let resizeObserver = null
// HMR 重新挂载时画布可能尚未就绪，草稿先暂存，createCanvas 完成后再恢复
let pendingDraft = null

// 空开状态：id -> 'on' | 'off'
const breakerStates = reactive({})
// 阶段 5 子状态：0/1/2 = 正在断 1/2/3 号排，3 = 待挂牌，4 = 已挂牌
const stage5Step = ref(0)
// 是否已悬挂「禁止合闸」标识牌
const signPlaced = ref(false)
let signNode = null
// 阶段 6：4 个表位是否已挂表
const meterPlaced = reactive([false, false, false, false])
let slotRects = []
let meterNodes = []
// 已接线：`${meterIndex}-${lineIndex}` -> true
const wires = reactive({})
// 当前选中的线：{ meter, line } | null
const selectedWire = ref(null)
// 阶段 7 子状态：0 待合 3 号排，1 待合 2 号排，2 待合 1 号排，3 上电完成
const stage7Step = ref(0)
// 4 个表盖是否已合上
const covers = reactive([false, false, false, false])
// 8 处表体铅封：下标 = 表序号 * 2 + 铅封序号
const seals = reactive([false, false, false, false, false, false, false, false])

function breakerImg(def, state) {
  if (def.group === 1) return state === 'off' ? Images.breaker1Off : Images.breaker1On
  if (def.group === 2) return state === 'off' ? Images.breaker2Off : Images.breaker2On
  return state === 'off' ? Images.breaker3Off : Images.breaker3On
}

function resetBreakers(initState) {
  BREAKER_DEFS.forEach(def => {
    breakerStates[def.id] = initState
  })
}

function createCanvas() {
  const img = bgImgRef.value
  if (!img) return
  const r = img.getBoundingClientRect()
  const w = Math.round(r.width)
  const h = Math.round(r.height)
  if (w <= 0 || h <= 0) return

  canvasStyle.value = { width: `${w}px`, height: `${h}px` }
  if (leafer) {
    leafer.destroy()
    leafer = null
  }
  // HMR 热更新时旧 Leafer 实例可能已丢失引用，残留的 canvas 会与背景图错位；
  // 创建新画布前先清空容器，保证只渲染一个画布
  if (leaferViewRef.value) leaferViewRef.value.replaceChildren()

  leafer = new Leafer({ view: leaferViewRef.value, width: w, height: h })
  bgLayer = new Group()
  breakerLayer = new Group()
  meterLayer = new Group()
  wireLayer = new Group()
  sealLayer = new Group()
  hitLayer = new Group()
  leafer.add(bgLayer)
  leafer.add(breakerLayer)
  leafer.add(meterLayer)
  leafer.add(wireLayer)
  leafer.add(sealLayer)
  leafer.add(hitLayer)
  // 接线选中后，预览线跟随画布内的鼠标移动
  leafer.on(PointerEvent.MOVE, updateWirePreview)
  bgLayer.add(new Image({ url: Images.residentBoxInnerBg, x: 0, y: 0, width: w, height: h }))

  buildBreakers(w, h)
  buildMeterSlots(w, h)
  renderMeters(w, h)
  buildWireHotspots(w, h)
  buildSealHotspots(w, h)
  redrawWires()
  redrawSeals()
  renderSign()
  applyStageInit()
  // 画布就绪后应用暂存的草稿（HMR 自行恢复）
  if (pendingDraft) {
    restore(pendingDraft)
    pendingDraft = null
  }
}

function buildBreakers(w, h) {
  breakerLayer.removeAll()
  BREAKER_DEFS.forEach(def => {
    const width = (w * def.w) / 100
    const node = new Image({
      url: breakerImg(def, breakerStates[def.id]),
      x: (w * def.x) / 100,
      y: (h * def.y) / 100,
      width,
      height: width / BREAKER_RATIO[def.group]
    })
    node.on(PointerEvent.CLICK, () => onBreakerClick(def.id))
    breakerLayer.add(node)
    def.node = node
  })
}

function refreshBreaker(def) {
  if (def.node) def.node.url = breakerImg(def, breakerStates[def.id])
}

/** 构建未挂表的表位热区（挂表后销毁） */
function buildMeterSlots(w, h) {
  if (!hitLayer) return
  METER_SLOTS.forEach((slot, i) => {
    if (meterPlaced[i]) return
    const rect = new Rect({
      x: (w * slot.x) / 100,
      y: (h * slot.y) / 100,
      width: (w * slot.w) / 100,
      height: (w * slot.w) / 100 / 0.7,
      fill: 'rgba(0,210,255,0.08)'
    })
    rect.on(PointerEvent.CLICK, () => onSlotClick(i))
    hitLayer.add(rect)
    slotRects.push(rect)
  })
}

/** 点击表位：阶段 6 且右栏选中电表时挂表 */
function onSlotClick(i) {
  if (props.disabled || props.stage !== 6) return
  if (props.rightTool !== 'meter') {
    ElMessage.warning('请先从右侧选择「单相智能电表」')
    emit('error')
    return
  }
  if (meterPlaced[i]) return
  meterPlaced[i] = true
  renderMeters()
  // 挂表后重建热区：该表位热区销毁，同时补上其线头/端子孔热区
  rebuildHitLayer()
  emit('operation')
  // 一次只放一块表：通知父组件取消右栏选中，跟随图随之消失
  emit('tool-used')
}

/** 绘制已挂的电表：阶段 7 合盖后切换为合盖图 */
function renderMeters(w, h) {
  if (!meterLayer || !leafer) return
  meterNodes.forEach(n => n.remove())
  meterNodes = []
  const cw = w ?? leafer.width
  const ch = h ?? leafer.height
  METER_SLOTS.forEach((slot, i) => {
    if (!meterPlaced[i]) return
    const width = (cw * slot.w) / 100
    const node = new Image({
      url: covers[i] ? Images.barSinglePhaseMeter : Images.singlePhaseMeterOpen,
      x: (cw * slot.x) / 100,
      y: (ch * slot.y) / 100,
      width,
      height: width / 0.75
    })
    meterLayer.add(node)
    meterNodes.push(node)
  })
}

/** 指定表、指定线序是否已接线 */
function wireDone(meter, line) {
  return !!wires[`${meter}-${line}`]
}

/** 点击线头：选中该导线（接线顺序不限，各线仍需接到自己的对应孔） */
function onWireStartClick(meter, line) {
  if (props.disabled || props.stage !== 6) return
  if (!meterPlaced[meter]) return
  // 已接好的线不再响应（热区会随接线销毁，这里兜底重复点击）
  if (wireDone(meter, line)) return
  selectedWire.value = { meter, line }
  // 选中后从线头中心拉出一条跟随鼠标的预览线（参考高压）
  startWirePreview(meter, line)
  emit('operation')
}

/** 点击端子孔：连接已选中的导线 */
function onHoleClick(meter, hole) {
  if (props.disabled || props.stage !== 6) return
  const sel = selectedWire.value
  if (!sel) {
    ElMessage.warning('请先点击要连接的导线')
    emit('error')
    return
  }
  if (sel.meter !== meter) {
    ElMessage.warning('请连接当前电表的导线')
    emit('error')
    return
  }
  if (hole !== sel.line) {
    ElMessage.warning('请连接到对应的端子孔')
    emit('error')
    selectedWire.value = null
    clearWirePreview()
    return
  }
  wires[`${meter}-${sel.line}`] = true
  selectedWire.value = null
  clearWirePreview()
  drawWire(meter, sel.line)
  emit('operation')
  // 接好一根后重建热区：该线头与孔销毁，未完成的热区保留
  rebuildHitLayer()
  if (Object.keys(wires).length === 16) emit('complete')
}

/** 重建交互层：销毁已完成元素的热区，只保留仍需点击的 */
function rebuildHitLayer() {
  if (!hitLayer || !leafer) return
  hitLayer.removeAll()
  slotRects = []
  buildMeterSlots(leafer.width, leafer.height)
  buildWireHotspots(leafer.width, leafer.height)
  buildSealHotspots(leafer.width, leafer.height)
}

/** 在交互层重建线头与端子孔热区 */
function buildWireHotspots(w, h) {
  if (!hitLayer) return
  // 线头热区：已接好的线头不再生成
  WIRE_START_POS.forEach((pos, idx) => {
    const meter = Math.floor(idx / 4)
    const line = idx % 4
    if (wireDone(meter, line)) return
    const r = new Rect({
      x: (w * pos.x) / 100,
      y: (h * pos.y) / 100,
      width: (w * WIRE_HOTSPOT) / 100,
      height: (w * WIRE_HOTSPOT) / 100,
      // 线头热区需要可见，方便逐个调整位置
      fill: 'rgba(255,140,0,0.28)'
    })
    r.on(PointerEvent.CLICK, () => onWireStartClick(meter, line))
    hitLayer.add(r)
  })
  // 端子孔热区（相对电表，仅已挂表的表位）
  METER_SLOTS.forEach((slot, meter) => {
    if (!meterPlaced[meter]) return
    const meterW = (w * slot.w) / 100
    const meterH = meterW / 0.75
    HOLE_RATIO.forEach((ratio, line) => {
      // 已接好的孔热区销毁
      if (wireDone(meter, line)) return
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * ratio.x,
        y: (h * slot.y) / 100 + meterH * ratio.y,
        width: (w * HOLE_HOTSPOT) / 100,
        height: (w * HOLE_HOTSPOT) / 100,
        fill: 'rgba(0,210,255,0.10)'
      })
      r.on(PointerEvent.CLICK, () => onHoleClick(meter, line))
      hitLayer.add(r)
    })
  })
  // 表盖热区（阶段 7：已挂表且未合盖的表位）
  if (props.stage === 7) {
    METER_SLOTS.forEach((slot, meter) => {
      if (!meterPlaced[meter] || covers[meter]) return
      const meterW = (w * slot.w) / 100
      const meterH = meterW / 0.75
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * COVER_RATIO.x,
        y: (h * slot.y) / 100 + meterH * COVER_RATIO.y,
        width: meterW * COVER_RATIO.w,
        height: meterH * COVER_RATIO.h,
        fill: 'rgba(0,210,255,0.10)'
      })
      r.on(PointerEvent.CLICK, () => onCoverClick(meter))
      hitLayer.add(r)
    })
  }
}

/** 端子孔绝对坐标 */
function holeAbsPos(meter, line) {
  const slot = METER_SLOTS[meter]
  const w = leafer.width
  const h = leafer.height
  const meterW = (w * slot.w) / 100
  const meterH = meterW / 0.75
  const ratio = HOLE_RATIO[line]
  const half = (w * HOLE_HOTSPOT) / 200
  return {
    x: (w * slot.x) / 100 + meterW * ratio.x + half,
    y: (h * slot.y) / 100 + meterH * ratio.y + half
  }
}

/** 线头绝对坐标（取热区中心） */
function startAbsPos(meter, line) {
  const pos = WIRE_START_POS[meter * 4 + line]
  const half = (leafer.width * WIRE_HOTSPOT) / 200
  return {
    x: (leafer.width * pos.x) / 100 + half,
    y: (leafer.height * pos.y) / 100 + half
  }
}

// ── 接线跟随预览线（参考高压：选中线头后从起点跟随鼠标） ──
let wirePreview = null

function startWirePreview(meter, line) {
  clearWirePreview()
  if (!wireLayer || !leafer) return
  const from = startAbsPos(meter, line)
  wirePreview = new Path({
    x: 0,
    y: 0,
    path: `M ${from.x} ${from.y} L ${from.x} ${from.y}`,
    stroke: WIRE_COLORS[meter][line],
    strokeWidth: Math.max(2, leafer.width * WIRE_STROKE_RATIO),
    strokeCap: 'round',
    opacity: 0.7
  })
  wireLayer.add(wirePreview)
}

/** Leafer 画布级鼠标移动：更新预览线终点（e.x/e.y 为画布坐标） */
function updateWirePreview(e) {
  if (!wirePreview || !selectedWire.value || !leafer) return
  const sel = selectedWire.value
  const from = startAbsPos(sel.meter, sel.line)
  wirePreview.path = `M ${from.x} ${from.y} L ${e.x} ${e.y}`
}

function clearWirePreview() {
  if (wirePreview) {
    wirePreview.remove()
    wirePreview = null
  }
}

/** 在导线层绘制一根线；合盖后按盖子下缘截断，隐藏被盖住的部分 */
function drawWire(meter, line) {
  if (!wireLayer || !leafer) return
  const from = startAbsPos(meter, line)
  let to = holeAbsPos(meter, line)
  if (covers[meter]) {
    const slot = METER_SLOTS[meter]
    const meterW = (leafer.width * slot.w) / 100
    const meterH = meterW / 0.75
    const cutY = (leafer.height * slot.y) / 100 + meterH * COVER_CUT_RATIO
    // 截断线落在导线路径上（0<t<1）时，只画到 cutY 处；否则保持原样
    const t = (cutY - from.y) / (to.y - from.y)
    if (t > 0 && t < 1) {
      to = { x: from.x + (to.x - from.x) * t, y: cutY }
    }
  }
  const color = WIRE_COLORS[meter][line]
  wireLayer.add(
    new Path({
      x: 0,
      y: 0,
      path: `M ${from.x} ${from.y} L ${to.x} ${to.y}`,
      stroke: color,
      strokeWidth: Math.max(2, leafer.width * WIRE_STROKE_RATIO),
      strokeCap: 'round'
    })
  )
}

/** 清空导线层并按已接线状态重绘（用于恢复与画布重建） */
function redrawWires() {
  if (!wireLayer) return
  wireLayer.removeAll()
  // 清空图层时预览线节点也被移除，引用同步置空
  wirePreview = null
  Object.keys(wires).forEach(k => {
    const [meter, line] = k.split('-').map(Number)
    if (wires[k]) drawWire(meter, line)
  })
}

/** 点击表盖：阶段 7 合盖，切换为合盖电表图并销毁该热区 */
function onCoverClick(meter) {
  if (props.disabled || props.stage !== 7) return
  if (covers[meter]) return
  covers[meter] = true
  renderMeters()
  // 合盖后导线按盖子下缘截断，隐藏被盖住的部分
  redrawWires()
  // 合盖后重建热区：该表盖热区销毁，同时补上其铅封热区
  rebuildHitLayer()
  emit('operation')
}

/** 构建表体铅封热区（阶段 7：已合盖且未加封的位置） */
function buildSealHotspots(w, h) {
  if (!hitLayer || props.stage !== 7) return
  METER_SLOTS.forEach((slot, meter) => {
    if (!meterPlaced[meter] || !covers[meter]) return
    const meterW = (w * slot.w) / 100
    const meterH = meterW / 0.75
    SEAL_RATIOS.forEach((ratio, k) => {
      const idx = meter * 2 + k
      if (seals[idx]) return
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * ratio.x,
        y: (h * slot.y) / 100 + meterH * ratio.y,
        width: (w * 2) / 100,
        height: (w * 2) / 100,
        fill: 'rgba(0,210,255,0.12)'
      })
      r.on(PointerEvent.CLICK, () => onSealClick(meter, k))
      hitLayer.add(r)
    })
  })
}

/** 点击铅封位：校验右栏已选中「铅封」后加封 */
function onSealClick(meter, k) {
  if (props.disabled || props.stage !== 7) return
  if (props.rightTool !== 'seal') {
    ElMessage.warning('请先从右侧选择「铅封」')
    emit('error')
    return
  }
  const idx = meter * 2 + k
  if (seals[idx]) return
  seals[idx] = true
  drawSeal(meter, k)
  // 加封后重建热区：该铅封位热区销毁
  rebuildHitLayer()
  emit('operation')
  // 8 处表体铅封完成，通知父页面卸载画布
  if (seals.every(Boolean)) emit('box-done')
}

/** 在铅封层绘制一处铅封 */
function drawSeal(meter, k) {
  if (!sealLayer || !leafer) return
  const slot = METER_SLOTS[meter]
  const meterW = (leafer.width * slot.w) / 100
  const meterH = meterW / 0.75
  const ratio = SEAL_RATIOS[k]
  const size = leafer.width * 0.025
  sealLayer.add(
    new Image({
      url: Images.barSeal,
      x: (leafer.width * slot.x) / 100 + meterW * ratio.x,
      y: (leafer.height * slot.y) / 100 + meterH * ratio.y,
      width: size,
      height: size
    })
  )
}

/** 清空铅封层并按已加封状态重绘（用于恢复与画布重建） */
function redrawSeals() {
  if (!sealLayer) return
  sealLayer.removeAll()
  seals.forEach((v, idx) => {
    if (v) drawSeal(Math.floor(idx / 2), idx % 2)
  })
}

/** 绘制/清除「禁止合闸」标识牌 */
// 标识牌图片宽高比（SafetyNotice.png 是竖长图，默认值按素材 517×866）
let signAspect = 517 / 866

function loadSignAspect() {
  const img = new window.Image()
  img.onload = () => {
    signAspect = img.naturalWidth / img.naturalHeight
    renderSign()
  }
  img.src = Images.barSafetyNotice
}

function renderSign() {
  if (signNode) {
    signNode.remove()
    signNode = null
  }
  if (!signPlaced.value || !breakerLayer || !leafer) return
  const width = (leafer.width * SIGN_STYLE.w) / 100
  signNode = new Image({
    url: Images.barSafetyNotice,
    x: (leafer.width * SIGN_STYLE.x) / 100,
    y: (leafer.height * SIGN_STYLE.y) / 100,
    width,
    height: width / signAspect
  })
  breakerLayer.add(signNode)
}

// 已执行过初始化的阶段：窗口缩放/HMR 重建画布时只刷新视觉，不重置状态
let initedStage = 0

/** 按阶段初始化空开状态：阶段 5 全 on；阶段 6/7 延续断电并保持挂牌 */
function applyStageInit() {
  // 同一阶段内重建（resize/HMR）：保留当前状态，只重绘
  if (initedStage === props.stage) {
    BREAKER_DEFS.forEach(refreshBreaker)
    renderSign()
    renderMeters()
    redrawWires()
    rebuildHitLayer()
    return
  }
  initedStage = props.stage

  if (props.stage === 5) {
    resetBreakers('on')
  } else if (props.stage === 6) {
    // 阶段 5 已完成断电与挂牌
    resetBreakers('off')
    signPlaced.value = true
  } else if (props.stage === 7) {
    // 阶段 6 已完成挂表接线，阶段 5 的断电/挂牌状态继续沿用
    resetBreakers('off')
    signPlaced.value = true
    if (meterPlaced.every(v => !v)) {
      meterPlaced.splice(0, 4, true, true, true, true)
      for (let m = 0; m < 4; m++) {
        for (let l = 0; l < 4; l++) wires[`${m}-${l}`] = true
      }
    }
  }
  BREAKER_DEFS.forEach(refreshBreaker)
  renderSign()
  renderMeters()
  redrawWires()
  rebuildHitLayer()
}

function onBreakerClick(id) {
  if (props.disabled) return
  // 阶段 7：上电（顺序 3 → 2 → 1）
  if (props.stage === 7) return onBreakerPowerOn(id)
  if (props.stage !== 5) return
  const def = BREAKER_DEFS.find(d => d.id === id)
  if (!def) return

  if (stage5Step.value < 3) {
    if (def.group !== stage5Step.value + 1) {
      ElMessage.warning('请按 1 → 2 → 3 的顺序断电')
      emit('error')
      return
    }
    if (breakerStates[id] === 'off') return
    breakerStates[id] = 'off'
    refreshBreaker(def)
    emit('operation')
    const groupDone = BREAKER_DEFS.filter(d => d.group === def.group).every(
      d => breakerStates[d.id] === 'off'
    )
    if (groupDone) {
      stage5Step.value++
      if (stage5Step.value === 3) ElMessage.info('断电完成，请悬挂「禁止合闸」标识牌')
    }
    return
  }

  // stage5Step === 3：必须先用左栏选中「禁止合闸」，再点 3 号空开
  if (props.leftTool !== 3) {
    ElMessage.warning('请先从左侧选择「禁止合闸」标识牌')
    emit('error')
    return
  }
  if (id !== '3') {
    ElMessage.warning('请将标识牌悬挂在 3 号空开上')
    emit('error')
    return
  }
  if (signPlaced.value) return
  signPlaced.value = true
  renderSign()
  emit('operation')
  stage5Step.value = 4
  emit('complete')
}

/** 阶段 7 上电：按 3 → 2 → 1 排顺序，同排空开任意顺序 */
function onBreakerPowerOn(id) {
  const def = BREAKER_DEFS.find(d => d.id === id)
  if (!def) return

  const expected = [3, 2, 1][stage7Step.value]
  if (stage7Step.value < 3 && def.group !== expected) {
    ElMessage.warning('请按 3 → 2 → 1 的顺序上电')
    emit('error')
    return
  }
  if (breakerStates[id] === 'on') return
  breakerStates[id] = 'on'
  refreshBreaker(def)
  emit('operation')
  // 第一个 3 号排空开推上时销毁标识牌，只销毁一次；后续不再重新生成
  if (def.group === 3 && signPlaced.value) {
    signPlaced.value = false
    if (signNode) {
      signNode.remove()
      signNode = null
    }
  }
  const groupDone = BREAKER_DEFS.filter(d => d.group === def.group).every(
    d => breakerStates[d.id] === 'on'
  )
  if (groupDone) stage7Step.value++
}

function onBgLoad() {
  createCanvas()
}

function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (leafer) createCanvas()
  }, 200)
}

onMounted(() => {
  if (bgImgRef.value?.complete) createCanvas()
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onWindowMouseMove)
  // 背景图尺寸变化（窗口缩放、布局/HMR 重排）时重建画布，避免画布与背景错位
  if (bgImgRef.value) {
    resizeObserver = new ResizeObserver(() => onResize())
    resizeObserver.observe(bgImgRef.value)
  }
  // 标识牌按素材真实比例绘制，避免被压成正方形
  loadSignAspect()
  // HMR 重新挂载时父组件的 handleRestore 不会重跑，画布自行拉一次草稿恢复
  if (props.experimentId && props.stepId) {
    getStepDraft(props.experimentId, props.stepId)
      .then(d => {
        if (!d || Object.keys(d).length === 0) return
        if (leafer) restore(d)
        else pendingDraft = d
      })
      .catch(() => {})
  }
})

onBeforeUnmount(() => {
  clearTimeout(resizeTimer)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onWindowMouseMove)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (leafer) {
    leafer.destroy()
    leafer = null
  }
})

watch(
  () => props.stage,
  () => {
    if (!leafer) return
    applyStageInit()
    // 阶段切换后重建热区（阶段 7 需要补上表盖/铅封热区）
    rebuildHitLayer()
    redrawSeals()
  }
)

function getState() {
  return {
    stage: props.stage,
    breakers: { ...breakerStates },
    signPlaced: signPlaced.value,
    meters: [...meterPlaced],
    wires: { ...wires },
    covers: [...covers],
    seals: [...seals]
  }
}

/** 依据已合上的空开还原阶段 7 上电子进度（3 → 2 → 1） */
function deriveStage7Step() {
  const groupOn = group =>
    BREAKER_DEFS.filter(d => d.group === group).every(d => breakerStates[d.id] === 'on')
  let step = 0
  for (const group of [3, 2, 1]) {
    if (!groupOn(group)) break
    step++
  }
  stage7Step.value = step
}

function restore(state) {
  if (!state) return
  if (state.breakers) {
    Object.keys(state.breakers).forEach(id => {
      if (breakerStates[id] !== undefined) breakerStates[id] = state.breakers[id]
    })
  }
  if (typeof state.signPlaced === 'boolean') signPlaced.value = state.signPlaced
  if (Array.isArray(state.meters)) state.meters.forEach((v, i) => (meterPlaced[i] = !!v))
  if (state.wires) {
    Object.keys(state.wires).forEach(k => (wires[k] = !!state.wires[k]))
  }
  if (Array.isArray(state.covers)) state.covers.forEach((v, i) => (covers[i] = !!v))
  if (Array.isArray(state.seals)) state.seals.forEach((v, i) => (seals[i] = !!v))
  if (props.stage === 7) deriveStage7Step()
  BREAKER_DEFS.forEach(refreshBreaker)
  renderSign()
  if (!leafer) return
  renderMeters()
  // 重建热区（已完成项不再生成），并重绘已接好的导线与已加封的铅封
  rebuildHitLayer()
  redrawWires()
  redrawSeals()
}

defineExpose({ getState, restore })
</script>

<style scoped>
.canvas-wrap {
  position: fixed;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-stage {
  position: relative;
  display: inline-block;
}

.bg-img {
  max-width: 90vw;
  max-height: 82vh;
  display: block;
  user-select: none;
}

.leafer-view {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

/* 右栏工具跟随图：fixed 定位、中心对准光标，位于画布与工具栏之上 */
.follower {
  position: fixed;
  width: 72px;
  height: auto;
  pointer-events: none;
  z-index: 60;
  transform: translate(-50%, -50%);
  user-select: none;
}
</style>
