<template>
  <div class="scene-stage" :class="{ following: isFollowing }" @mousemove="moveCursor">
    <!-- 场景容器：尺寸由内部背景图决定，所有叠加元素按百分比定位 -->
    <div ref="groupRef" class="scene-group" @click="onSceneClick">
      <img :src="Images.poleTransformerBg" alt="杆上变场景" class="scene-bg" draggable="false" />

      <!-- 已放置：整体围栏（复用低压通用围栏图） -->
      <img v-if="placed[0]" :src="Images.residentBoxFence" alt="围栏" class="overlay" :style="FENCE_STYLE"
        draggable="false" />

      <!-- 已放置：两块标识牌，各自独立位置 -->
      <img v-if="placed[1]" :src="Images.barSignStopHighVoltage" alt="有电危险标识牌" class="overlay"
        :style="SIGN_DANGER_STYLE" draggable="false" />
      <img v-if="placed[2]" :src="Images.barSignPersonWorking" alt="有人工作标识牌" class="overlay" :style="SIGN_WORKING_STYLE"
        draggable="false" />

      <!-- 三步验电第二步：门把热区高亮（同时是调坐标的可视化参照） -->
      <div v-if="vtActive && vtStep === 1" class="check-zone" :style="zoneStyle(DOOR_CHECK_ZONE)" />
    </div>

    <!--
      左右工具栏与高压共用同一套组件和样式。
      左栏第 5 项是插座：杆上变背景没有排插，验电时直接点击左栏插座代替「点排插」。
    -->
    <ToolBarLeft :items="LEFT_TOOLS" :placed-mask="placed" :active-idx="followingTool" @select="selectTool" />
    <ToolBarRight :items="RIGHT_TOOLS" :vt-idx="VT_IDX" :vt-active="vtActive" :vt-done="vtDone" :active-idxs="[]"
      @click="onRightToolClick" />

    <!-- 鼠标跟随：围栏/标识牌中心对准光标；验电笔尖端对准光标 -->
    <img v-if="showFollower" class="follower" :src="followerImg" alt="" :style="followerStyle" draggable="false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import ToolBarLeft from '@/components/ToolBarLeft.vue'
import ToolBarRight from '@/components/ToolBarRight.vue'
import { categories as lvCategories } from '@/constants/low-voltage-tool-selection-config'

// ★★★ 放置坐标（相对场景图尺寸的 %，按需调整） ★★★
// 围栏：跨越两根电线杆，宽度与高度需要按 29.jpg 实际透视微调
const FENCE_STYLE = { left: '12%', top: '57%', width: '72%', height: 'auto' }
// 两块标识牌：分别挂在左右两根电线杆上
const SIGN_DANGER_STYLE = { left: '21.5%', top: '62%', width: '6%', height: 'auto' }
const SIGN_WORKING_STYLE = { left: '43%', top: '62%', width: '6%', height: 'auto' }
// 三步验电第二步：柜门门把热区（只做命中判定，验电到第二步时显示高亮框便于调坐标）
const DOOR_CHECK_ZONE = { left: '45.5%', top: '62%', width: '8%', height: '8%' }

const props = defineProps({
  /** 当前阶段：8 = 架设围栏并悬挂标识牌，9 = 三步式验电 */
  stage: { type: Number, required: true },
  /** 恢复失败等场景下禁止交互 */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['complete', 'operation', 'error'])

const groupRef = ref(null)

// ── 步骤 8：放置状态 ──────────────────────────────────────
// 顺序：0 围栏、1 有电危险牌、2 有人工作牌、3 请勿合闸牌、4 插座
// 请勿合闸牌在后续集中器安装内部操作中使用；插座用于三步验电
const placed = reactive([false, false, false, false, false])
const followingTool = ref(null)

/*
 * 进入步骤 9 时围栏与标识牌必然已经放好（步骤 8 的完成条件就是三样齐全），
 * 但步骤 9 有自己的存档记录，里面没有放置状态。这里强制标记为已放置，
 * 否则验电前会被「请先完成围栏与标识牌放置」挡住。与居民表箱的处理一致。
 */
watch(
  () => props.stage,
  s => {
    if (s === 9) placed.splice(0, 3, true, true, true)
    if (s !== 8) followingTool.value = null
  },
  // immediate：直接从步骤 9 刷新/恢复进入时，watch 不会触发，
  // placed 会停在未放置状态，验电第一步就被「请先完成围栏与标识牌放置」挡住
  { immediate: true }
)

// ── 左栏物品 ──────────────────────────────────────────────
const LEFT_TOOLS = [
  { name: '围栏', img: Images.barLeftFence },
  { name: '有电危险标识牌', img: Images.barSignStopHighVoltage },
  { name: '有人工作标识牌', img: Images.barSignPersonWorking },
  { name: '请勿合闸标识牌', img: Images.barSafetyNotice },
  // 插座图透明边距很小，按 0.7 缩小以与其他图标视觉一致
  { name: '插座', img: Images.powerSocket, scale: 0.7 }
]

const SOCKET_IDX = 4

// ── 右栏物品：与居民表箱一致（工器具选择的标准答案），本阶段只用验电笔 ──
const BAR_IMG = {
  24: Images.barConcentrator,
  32: Images.barCrossScrewdriver,
  36: Images.barWireStripper,
  37: Images.barVoltageTesterNormal,
  38: Images.barSeal,
  51: Images.barWireYellow,
  53: Images.barWireGreen,
  54: Images.barWireRed,
  56: Images.barWire25mmBlack,
  57: Images.barWireYellow,
  58: Images.barWire4mm2YellowBlack,
  59: Images.barWireGreen,
  60: Images.barWireRed,
  61: Images.barWire4mm2RedBlack,
  63: Images.barWireGreenBlack,
  67: Images.barCommunicationModule,
  68: Images.barSimCard,
  69: Images.barAntenna
}

function pickCorrectItems(key) {
  const cat = lvCategories.find(c => c.key === key)
  if (!cat) return []
  return cat.correctIds
    .map(id => cat.tools.find(t => t.id === id))
    .filter(Boolean)
    .map(tool => ({ name: tool.name, img: BAR_IMG[tool.id] || Images[tool.image] }))
}

const RIGHT_TOOLS = [
  ...pickCorrectItems('terminal'),
  ...pickCorrectItems('tool'),
  ...pickCorrectItems('wire')
]

const VT_IDX = RIGHT_TOOLS.findIndex(t => t.name === '验电笔')

// ── 步骤 9：三步验电 ──────────────────────────────────────
// 0 待验电 → 1 已点插座 → 2 已点门把 → 3 完成
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
const vtProbing = ref(false)

const vtImg = computed(() =>
  vtProbing.value ? Images.barVoltageTesterWarning : Images.barVoltageTesterNormal
)

// ── 鼠标跟随 ──────────────────────────────────────────────
const isFollowing = computed(
  () => followingTool.value !== null || (!props.disabled && vtActive.value)
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

/** 点左栏插座验电时短暂切换为「验电中」状态图，模拟探针接触（与高压一致） */
let probingTimer = null
function flashProbing() {
  vtProbing.value = true
  clearTimeout(probingTimer)
  probingTimer = setTimeout(() => {
    vtProbing.value = false
  }, 200)
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
const fenceAspect = reactive({ fence: 1 })

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

// ── 步骤 8 交互 ───────────────────────────────────────────
function selectTool(idx) {
  if (props.disabled) return

  // 左栏插座：验电步骤中代替「点排插」，不进入放置流程
  if (idx === SOCKET_IDX) {
    clickSocket()
    return
  }

  emit('operation')
  vtActive.value = false
  // 请勿合闸牌本阶段不使用，后续集中器安装内部操作才用
  if (idx === 3) {
    ElMessage.info('「请勿合闸」标识牌将在后续内部操作中使用')
    return
  }
  if (placed[idx]) {
    fail('该物品已放置')
    return
  }
  followingTool.value = followingTool.value === idx ? null : idx
}

function onSceneClick(e) {
  if (props.disabled) return
  if (props.stage === 8) handlePlace(e)
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
  const ok = idx === 0 ? hitFenceFromStyle(e, FENCE_STYLE, 'fence') : hitScene(e)
  if (!ok) {
    fail('请选择正确的放置位置')
    return
  }

  placed[idx] = true
  followingTool.value = null
  emit('operation')
  // 完成条件只看前三项：请勿合闸牌与插座留到后续步骤
  if (placed.slice(0, 3).every(Boolean)) emit('complete')
}

// ── 步骤 9 交互 ───────────────────────────────────────────
/** 右栏点击：只有验电笔在本阶段可用，其余工具留待内部场景 */
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
  if (placed.slice(0, 3).some(v => !v)) {
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

/**
 * 点击左栏插座：代替背景里的排插。
 *
 * 正确顺序：插座 → 门把 → 插座；顺序错时归零并计一次错误（与高压一致）。
 */
function clickSocket() {
  if (props.stage !== 9) {
    ElMessage.info('插座将在三步验电时使用')
    return
  }
  if (!vtActive.value) {
    ElMessage.warning('请先从右侧选择「验电笔」')
    return
  }
  if (vtDone.value) return
  emit('operation')

  if (vtStep.value === 0 || vtStep.value === 2) {
    flashProbing()
    vtStep.value += 1
    if (vtStep.value === 3) {
      vtDone.value = true
      vtActive.value = false
      emit('complete')
    }
  } else {
    vtStep.value = 0
    fail('验电顺序不正确，请按「插座 → 门把 → 插座」操作')
  }
}

function handleVoltageTest(e) {
  if (!vtActive.value || vtDone.value) return

  const onDoor = hitZone(e, DOOR_CHECK_ZONE)
  if (!onDoor) {
    fail('请点击柜门门把')
    return
  }

  if (vtStep.value === 1) {
    vtStep.value = 2
    emit('operation')
  } else {
    vtStep.value = 0
    fail('验电顺序不正确，请按「插座 → 门把 → 插座」操作')
  }
}

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
  // 步骤 9 的放置状态恒为「已完成」，不采纳存档里的旧值
  if (props.stage === 8 && Array.isArray(state.placed)) {
    state.placed.slice(0, 3).forEach((v, i) => {
      placed[i] = Boolean(v)
    })
  }
  if (typeof state.vtStep === 'number') vtStep.value = state.vtStep
  if (typeof state.vtDone === 'boolean') vtDone.value = state.vtDone
}

defineExpose({ getState, restore })

onMounted(() => {
  // 命中检测需要围栏的真实宽高比
  loadImageAspect(Images.residentBoxFence, 'fence')
})

onUnmounted(() => {
  clearTimeout(probingTimer)
})
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
