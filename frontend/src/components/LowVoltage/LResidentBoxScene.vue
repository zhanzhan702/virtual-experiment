<template>
  <div
    class="scene-stage"
    :class="{ following: isFollowing }"
    @mousemove="moveCursor"
    @mouseup="vtProbing = false"
  >
    <!-- 场景容器：尺寸由内部背景图决定，所有叠加元素按百分比定位 -->
    <div ref="groupRef" class="scene-group" @click="onSceneClick">
      <img :src="Images.residentBoxBg" alt="居民表箱场景" class="scene-bg" draggable="false" />

      <!-- 排插：叠在背景左上角，与高压一致 -->
      <img
        ref="socketRef"
        :src="Images.powerSocket"
        alt="排插"
        class="overlay power-socket"
        :style="POWER_SOCKET_STYLE"
        draggable="false"
      />

      <!-- 已放置：围栏左右两半 -->
      <img
        v-if="placed[0]"
        :src="Images.barLeftFence"
        alt="围栏（左）"
        class="overlay"
        :style="LEFT_FENCE_STYLE"
        draggable="false"
      />
      <img
        v-if="placed[0]"
        :src="Images.barRightFence"
        alt="围栏（右）"
        class="overlay"
        :style="RIGHT_FENCE_STYLE"
        draggable="false"
      />

      <!-- 已放置：两块标识牌，各自独立位置 -->
      <img
        v-if="placed[1]"
        :src="Images.barSignStopHighVoltage"
        alt="有电危险标识牌"
        class="overlay"
        :style="SIGN_DANGER_STYLE"
        draggable="false"
      />
      <img
        v-if="placed[2]"
        :src="Images.barSignPersonWorking"
        alt="有人工作标识牌"
        class="overlay"
        :style="SIGN_WORKING_STYLE"
        draggable="false"
      />

      <!-- 三步验电第二步：门把热区高亮（同时是调坐标的可视化参照） -->
      <div v-if="vtActive && vtStep === 1" class="check-zone" :style="zoneStyle(DOOR_CHECK_ZONE)" />
    </div>

    <!--
      左右工具栏与高压共用同一套组件和样式（@/components/ToolBarLeft | ToolBarRight，
      样式在 assets/styles/toolbar.css）。两侧常驻显示：已放置的物品会置灰，
      不在当前阶段时可点击但会给出对应提示，与高压的交互一致。
    -->
    <ToolBarLeft
      :items="LEFT_TOOLS"
      :placed-mask="placed"
      :active-idx="followingTool"
      @select="selectTool"
    />
    <ToolBarRight
      :items="RIGHT_TOOLS"
      :vt-idx="VT_IDX"
      :vt-active="vtActive"
      :vt-done="vtDone"
      @click="onRightToolClick"
    />

    <!-- 鼠标跟随：围栏/标识牌中心对准光标；验电笔尖端对准光标 -->
    <img
      v-if="showFollower"
      class="follower"
      :src="followerImg"
      alt=""
      :style="followerStyle"
      draggable="false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import ToolBarLeft from '@/components/ToolBarLeft.vue'
import ToolBarRight from '@/components/ToolBarRight.vue'

// ★★★ 放置坐标（相对场景图尺寸的 %，按需调整） ★★★
// 围栏：左右两半各自独立落位
const LEFT_FENCE_STYLE = { left: '-12%', top: '58%', width: '56%', height: 'auto' }
const RIGHT_FENCE_STYLE = { left: '56%', top: '63%', width: '46%', height: 'auto' }
// 两块标识牌：分开挂，各自独立的位置与大小
const SIGN_DANGER_STYLE = { left: '33%', top: '67%', width: '10%', height: 'auto' }
const SIGN_WORKING_STYLE = { left: '53%', top: '67%', width: '10%', height: 'auto' }
// 排插：叠在背景左上角（与高压一致，不画进背景图）
// left 不能小于 14%：左侧工具栏 fixed 宽 10vw，会盖住场景最左约 11% 的宽度
const POWER_SOCKET_STYLE = { left: '14%', top: '12%', width: '7%', height: 'auto' }
// 三步验电第二步：表箱门把热区（只做命中判定，验电到第二步时显示高亮框便于调坐标）
const DOOR_CHECK_ZONE = { left: '52%', top: '30%', width: '8%', height: '12%' }

const props = defineProps({
  /** 当前阶段：3 = 架设围栏并悬挂标识牌，4 = 三步式验电 */
  stage: { type: Number, required: true },
  /** 恢复失败等场景下禁止交互 */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['complete', 'operation', 'error'])

const groupRef = ref(null)
const socketRef = ref(null)

// ── 步骤 3：放置状态 ──────────────────────────────────────
// 顺序：0 围栏、1 有电危险牌、2 有人工作牌
const placed = reactive([false, false, false])
const followingTool = ref(null)

/*
 * 进入步骤 4 时围栏与标识牌必然已经放好（步骤 3 的完成条件就是三样齐全），
 * 但步骤 4 有自己的存档记录，里面没有放置状态。这里强制标记为已放置，
 * 否则验电前会被「请先完成围栏与标识牌放置」挡住。与高压的处理一致。
 */
watch(
  () => props.stage,
  s => {
    if (s === 4) placed.splice(0, 3, true, true, true)
  },
  { immediate: true }
)

// 顺序即 placed 数组的下标：0 围栏、1 有电危险牌、2 有人工作牌
const LEFT_TOOLS = [
  { name: '围栏', img: Images.barLeftFence },
  { name: '有电危险标识牌', img: Images.barSignStopHighVoltage },
  { name: '有人工作标识牌', img: Images.barSignPersonWorking }
]

/**
 * 右栏：镜像**低压**工器具选择页的目录（`low-voltage-tool-selection-config.js`），
 * 顺序与选择页一致。
 *
 * 注意与高压的区别：低压的终端是集中器、线材是「4.0mm²绿黑线 + 0.5mm²遥控/遥信线 +
 * RS485 线」，没有高压的 6 芯 / 8 芯信号线。不要把高压的清单抄过来。
 *
 * 有 1:1 工具栏版素材（`bar*`）的优先用；集中器、2.5mm²黑线、RS485 线没有工具栏版，
 * 直接复用选择页的图。
 */
const RIGHT_TOOLS = [
  { name: '集中器', img: Images.concentrator },

  { name: '十字螺丝刀', img: Images.barCrossScrewdriver },
  { name: '剥线钳', img: Images.barWireStripper },
  { name: '验电笔', img: Images.barVoltageTesterNormal },
  { name: '铅封', img: Images.barSeal },

  { name: '2.5MM²黄色导线', img: Images.barWireYellow },
  { name: '2.5MM²绿色导线', img: Images.barWireGreen },
  { name: '2.5MM²红色导线', img: Images.barWireRed },
  { name: '2.5MM²黑色导线', img: Images.wire25mmBlack },

  { name: '4.0MM²黄色导线', img: Images.barWireYellow, imgWidth: '95%' },
  { name: '4.0MM²黄黑色导线', img: Images.barWire4mm2YellowBlack },
  { name: '4.0MM²绿色导线', img: Images.barWireGreen, imgWidth: '95%' },
  { name: '4.0MM²红色导线', img: Images.barWireRed, imgWidth: '95%' },
  { name: '4.0MM²红黑色导线', img: Images.barWire4mm2RedBlack },
  { name: '4.0MM²绿黑色导线', img: Images.barWireGreenBlack },
  { name: '扎带', img: Images.barCableTieLabel },

  { name: '0.5MM²遥控线', img: Images.barWire2Core },
  { name: '0.5MM²遥信线', img: Images.barWire2Core },
  { name: 'RS485线', img: Images.wireRS485 },
  { name: '通信模块', img: Images.barCommunicationModule },
  { name: 'SIM卡', img: Images.barSimCard },
  { name: '天线', img: Images.barAntenna }
]

/** 验电笔在右栏里的下标（ToolBarRight 据此高亮） */
const VT_IDX = RIGHT_TOOLS.findIndex(t => t.name === '验电笔')

// ── 步骤 4：三步验电 ──────────────────────────────────────
// 0 待验电 → 1 已点排插 → 2 已点门把 → 3 完成
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
const vtProbing = ref(false)

const vtImg = computed(() =>
  vtProbing.value ? Images.barVoltageTesterWarning : Images.barVoltageTesterNormal
)

// ── 鼠标跟随 ──────────────────────────────────────────────
const isFollowing = computed(
  () => !props.disabled && (followingTool.value !== null || vtActive.value)
)
const showFollower = computed(() => isFollowing.value)
const cursor = reactive({ x: 0, y: 0 })
// 验电笔的尖端相对图片中心的偏移（正 = 右/下），让尖端对准光标
const VOLTAGE_TESTER_TIP = { x: -28, y: 28 }

const followerImg = computed(() => {
  if (vtActive.value) return vtImg.value
  return LEFT_TOOLS[followingTool.value]?.img || Images.barLeftFence
})

const followerStyle = computed(() => {
  const style = { left: cursor.x + 'px', top: cursor.y + 'px' }
  if (vtActive.value) {
    style['--follow-ox'] = -VOLTAGE_TESTER_TIP.x + 'px'
    style['--follow-oy'] = -VOLTAGE_TESTER_TIP.y + 'px'
  }
  return style
})

function moveCursor(e) {
  if (!isFollowing.value) return
  cursor.x = e.clientX
  cursor.y = e.clientY
}

// ── 命中判定 ──────────────────────────────────────────────

/**
 * 百分比矩形是否命中光标（相对场景容器）。
 *
 * 配置里的值是带 `%` 的字符串（`:style` 要直接用它），做数学运算前必须 parseFloat ——
 * 直接相乘会得到 NaN，判定永远为假。
 */
function hitZone(e, zone) {
  const g = groupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  const x = r.left + (r.width * parseFloat(zone.left)) / 100
  const y = r.top + (r.height * parseFloat(zone.top)) / 100
  const w = (r.width * parseFloat(zone.width)) / 100
  const h = (r.height * parseFloat(zone.height)) / 100
  return e.clientX >= x && e.clientX <= x + w && e.clientY >= y && e.clientY <= y + h
}

/** 围栏图片宽高比（运行时从实际图片获取，用于命中检测） */
const fenceAspect = reactive({ left: 1, right: 1 })

function loadImageAspect(src, key) {
  const img = new window.Image()
  img.onload = () => {
    fenceAspect[key] = img.naturalWidth / img.naturalHeight
  }
  img.src = src
}

/** 围栏命中：点击必须落在围栏将要出现的矩形内（与高压一致） */
function hitFenceFromStyle(e, style, aspectKey) {
  const g = groupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  const imgLeft = r.left + (r.width * parseFloat(style.left)) / 100
  const imgTop = r.top + (r.height * parseFloat(style.top)) / 100
  const imgW = (r.width * parseFloat(style.width)) / 100
  const imgH = imgW / fenceAspect[aspectKey]
  return (
    e.clientX >= imgLeft &&
    e.clientX <= imgLeft + imgW &&
    e.clientY >= imgTop &&
    e.clientY <= imgTop + imgH
  )
}

/** 标识牌命中：点场景图任意位置即可（与高压一致） */
function hitScene(e) {
  const g = groupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
}

/**
 * 元素本体是否被点中。
 *
 * 排插的 width 是百分比、height 是 auto（跟着图片比例走），没法用百分比矩形算，
 * 直接量它的实际渲染尺寸最准 —— 与高压的 hitTest 一致。
 */
function hitEl(e, el) {
  if (!el) return false
  const r = el.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
}

/** 热区可视化：配置值本身已带 %，直接透传，不能再拼一次 */
function zoneStyle(zone) {
  return {
    left: zone.left,
    top: zone.top,
    width: zone.width,
    height: zone.height
  }
}

function fail(message) {
  ElMessage.warning(message)
  emit('error')
}

// ── 步骤 3 交互 ───────────────────────────────────────────
function selectTool(idx) {
  if (props.disabled) return
  emit('operation')
  vtActive.value = false
  if (placed[idx]) {
    fail('该物品已放置')
    return
  }
  followingTool.value = followingTool.value === idx ? null : idx
}

function onSceneClick(e) {
  if (props.disabled) return
  if (props.stage === 3) handlePlace(e)
  else handleVoltageTest(e)
}

function handlePlace(e) {
  const idx = followingTool.value
  if (idx === null) {
    fail('请先从左侧工具栏选择工具')
    return
  }

  // 围栏与标识牌各自只认自己的命中区域
  // 与高压一致：围栏必须落在围栏将要出现的位置，标识牌点场景任意处即可
  const ok =
    idx === 0
      ? hitFenceFromStyle(e, LEFT_FENCE_STYLE, 'left') ||
        hitFenceFromStyle(e, RIGHT_FENCE_STYLE, 'right')
      : hitScene(e)
  if (!ok) {
    fail('请选择正确的放置位置')
    return
  }

  placed[idx] = true
  followingTool.value = null
  emit('operation')
  if (placed.every(Boolean)) emit('complete')
}

// ── 步骤 4 交互 ───────────────────────────────────────────
/** 右栏点击：只有验电笔在本阶段可用，其余工具留待后续场景（与高压一致） */
function onRightToolClick(idx) {
  if (props.disabled) return
  if (idx !== VT_IDX) {
    emit('operation')
    ElMessage.info('该工具将在后续步骤中使用')
    return
  }
  toggleVoltageTester()
}

function toggleVoltageTester() {
  if (props.disabled) return
  emit('operation')
  if (placed.some(v => !v)) {
    fail('请先完成围栏与标识牌放置')
    return
  }
  if (vtDone.value) {
    fail('验电已完成')
    return
  }
  if (vtActive.value) {
    vtActive.value = false
    vtStep.value = 0
    vtProbing.value = false
    return
  }
  vtActive.value = true
  vtStep.value = 0
}

function handleVoltageTest(e) {
  if (!vtActive.value || vtDone.value) return

  const onSocket = hitEl(e, socketRef.value)
  const onDoor = hitZone(e, DOOR_CHECK_ZONE)

  // 顺序错：归零并计一次错误（与高压一致）
  if (vtStep.value === 0 && onSocket) {
    vtStep.value = 1
  } else if (vtStep.value === 1 && onDoor) {
    vtStep.value = 2
  } else if (vtStep.value === 2 && onSocket) {
    vtStep.value = 3
    vtDone.value = true
    vtActive.value = false
    emit('operation')
    emit('complete')
  } else {
    vtStep.value = 0
    fail('验电顺序不正确，请按「排插 → 门把 → 排插」操作')
  }
}

onMounted(() => {
  // 命中检测需要围栏的真实宽高比
  loadImageAspect(Images.barLeftFence, 'left')
  loadImageAspect(Images.barRightFence, 'right')
})

// ── 对外状态读写 ──────────────────────────────────────────
function getState() {
  return {
    placed: [...placed],
    vtStep: vtStep.value,
    vtDone: vtDone.value
  }
}

function restore(state) {
  if (!state) return
  // 步骤 4 的放置状态恒为「已完成」，不采纳存档里的旧值
  if (props.stage === 3 && Array.isArray(state.placed) && state.placed.length === 3) {
    placed.splice(0, 3, ...state.placed.map(Boolean))
  }
  if (typeof state.vtStep === 'number') vtStep.value = state.vtStep
  if (typeof state.vtDone === 'boolean') vtDone.value = state.vtDone
}

defineExpose({ getState, restore })
</script>

<style scoped>
.scene-stage {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.scene-stage.following {
  cursor: crosshair;
}

/* 容器尺寸由背景图决定，叠加元素的百分比因此始终对齐图片 */
.scene-group {
  position: relative;
  display: inline-block;
  line-height: 0;
}

.scene-bg {
  max-width: 90vw;
  max-height: 85vh;
  display: block;
  user-select: none;
}

.overlay {
  position: absolute;
  height: auto;
  user-select: none;
  pointer-events: none;
}

/* 排插在验电时需要被点到，单独放开指针事件 */
.power-socket {
  pointer-events: auto;
}

/* 门把热区高亮：既是操作提示，也是调 DOOR_CHECK_ZONE 时的可视化参照 */
.check-zone {
  position: absolute;
  border: 2px dashed #00d2ff;
  background: rgba(0, 210, 255, 0.12);
  box-shadow: 0 0 18px rgba(0, 210, 255, 0.35);
  pointer-events: none;
}

/* 工具栏样式由 @/assets/styles/toolbar.css 提供（与高压共用） */

/* 鼠标跟随图：fixed 定位，中心（或验电笔尖端）对准光标 */
.follower {
  position: fixed;
  width: 72px;
  height: auto;
  pointer-events: none;
  z-index: 50;
  transform: translate(-50%, -50%) translate(var(--follow-ox, 0), var(--follow-oy, 0));
  user-select: none;
}
</style>
