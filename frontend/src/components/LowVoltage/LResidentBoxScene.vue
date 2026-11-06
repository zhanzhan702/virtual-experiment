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

    <!-- 左侧工具栏：步骤 3 -->
    <aside v-if="stage === 3" class="toolbar toolbar-left">
      <button
        v-for="tool in LEFT_TOOLS"
        :key="tool.idx"
        type="button"
        class="tool-btn"
        :class="{ active: followingTool === tool.idx }"
        :aria-label="tool.name"
        @click.stop="selectTool(tool.idx)"
      >
        <img :src="tool.img" :alt="tool.name" draggable="false" />
      </button>
    </aside>

    <!-- 右侧工具栏：步骤 4 -->
    <aside v-if="stage === 4" class="toolbar toolbar-right">
      <button
        type="button"
        class="tool-btn"
        :class="{ active: vtActive }"
        aria-label="验电笔"
        @click.stop="toggleVoltageTester"
      >
        <img :src="Images.barVoltageTesterNormal" alt="验电笔" draggable="false" />
      </button>
    </aside>

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
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import {
  LEFT_FENCE_STYLE,
  RIGHT_FENCE_STYLE,
  SIGN_DANGER_STYLE,
  SIGN_WORKING_STYLE,
  POWER_SOCKET_STYLE,
  DOOR_CHECK_ZONE,
  FENCE_HIT_ZONE,
  SIGN_HIT_ZONE
} from '@/constants/resident-box-scene'

const props = defineProps({
  /** 当前阶段：3 = 架设围栏并悬挂标识牌，4 = 三步式验电 */
  stage: { type: Number, required: true },
  /** 恢复失败等场景下禁止交互 */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['complete', 'operation', 'error'])

const groupRef = ref(null)

// ── 步骤 3：放置状态 ──────────────────────────────────────
// 顺序：0 围栏、1 有电危险牌、2 有人工作牌
const placed = reactive([false, false, false])
const followingTool = ref(null)

const LEFT_TOOLS = [
  { idx: 0, name: '围栏', img: Images.barLeftFence },
  { idx: 1, name: '有电危险标识牌', img: Images.barSignStopHighVoltage },
  { idx: 2, name: '有人工作标识牌', img: Images.barSignPersonWorking }
]

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

/** 百分比矩形是否命中光标（相对场景容器） */
function hitZone(e, zone) {
  const g = groupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  const x = r.left + (r.width * zone.left) / 100
  const y = r.top + (r.height * zone.top) / 100
  const w = (r.width * zone.width) / 100
  const h = (r.height * zone.height) / 100
  return e.clientX >= x && e.clientX <= x + w && e.clientY >= y && e.clientY <= y + h
}

function zoneStyle(zone) {
  return {
    left: zone.left + '%',
    top: zone.top + '%',
    width: zone.width + '%',
    height: zone.height + '%'
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
  const ok = idx === 0 ? hitZone(e, FENCE_HIT_ZONE) : hitZone(e, SIGN_HIT_ZONE)
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

  const onSocket = hitZone(e, POWER_SOCKET_STYLE)
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
  if (Array.isArray(state.placed) && state.placed.length === 3) {
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

/* 工具栏 */
.toolbar {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 10px;
  background: rgba(0, 141, 133, 0.9);
  border-radius: 16px;
  z-index: 20;
}

.toolbar-left {
  left: 2vw;
}

.toolbar-right {
  right: 2vw;
}

.tool-btn {
  width: 64px;
  height: 64px;
  padding: 6px;
  border: 2px solid transparent;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition:
    border-color 0.2s,
    transform 0.2s;
}

.tool-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.tool-btn:hover {
  transform: scale(1.05);
}

.tool-btn.active {
  border-color: #ffd400;
  box-shadow: 0 0 12px rgba(255, 212, 0, 0.8);
}

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
