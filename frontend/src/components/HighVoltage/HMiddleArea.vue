<!-- 中间交互区域：围栏/告示牌放置（步骤3）+ 三步验电（步骤4）+ 鼠标跟随 -->
<template>
  <!-- 中间交互区域（cabinet-group 固定图像宽高比，所有物品 % 定位） -->
  <div class="middle-area" :class="{ 'is-following': showFollowing }" :style="middleAreaStyle"
    @click="onMiddleAreaClick">
    <div class="cabinet-group" ref="cabinetGroupRef">
      <img :src="Images.cabinetGroupOverview" alt="柜体局部" class="cabinet-img" draggable="false" />
      <!-- 三步验电第2步：柜体验电区域点击热区可视化（区域由 CABINET_CHECK_ZONE 控制） -->
      <div v-if="vtActive && vtStep === 1" class="cabinet-hit-zone" :style="hitZoneStyle" />
      <img :src="Images.powerSocket" alt="电源插座" class="power-socket-img" draggable="false" />
      <img v-if="itemPlaced[0]" :src="Images.barLeftFence" class="placed-img" :style="LEFT_FENCE_STYLE"
        draggable="false" />
      <img v-if="itemPlaced[0]" :src="Images.barRightFence" class="placed-img" :style="RIGHT_FENCE_STYLE"
        draggable="false" />
      <img v-if="itemPlaced[1]" :src="Images.barSignStopHighVoltage" class="placed-img" :style="LEFT_SIGN_HV_STYLE"
        draggable="false" />
      <img v-if="itemPlaced[1]" :src="Images.barSignStopHighVoltage" class="placed-img" :style="RIGHT_SIGN_HV_STYLE"
        draggable="false" />
      <img v-if="itemPlaced[2]" :src="Images.barSignPersonWorking" class="placed-img" :style="SIGN_WORKING_STYLE"
        draggable="false" />
      <img v-if="itemPlaced[3]" :src="Images.barSafetyNotice" class="placed-img" :style="SAFETY_NOTICE_STYLE"
        draggable="false" />
      <!-- 步骤12 终端小室：地板 3 张垃圾占位（后续替换为垃圾 png） -->
      <div v-if="props.stepOrder === 12" class="trash-placeholder" v-for="t in 3" :key="t" :style="trashStyle(t - 1)" />
    </div>
  </div>

  <!-- 鼠标跟随 -->
  <div v-if="showFollowing" class="cursor-following" :style="cursorFollowingStyle">
    <img :src="followImg" alt="" draggable="false" />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'

const props = defineProps({
  stepOrder: { type: Number, required: true }
})
const emit = defineEmits(['operation', 'error', 'fencesDone', 'voltageCheckDone'])

const isStep4 = computed(() => props.stepOrder === 4 || props.stepOrder === 12)

// 步骤12 终端小室：3 张垃圾占位位置（相对柜体背景比率，占位微调）
const TRASH_STYLES = [
  { left: '-22%', top: '100%', width: '6%', height: '6%' },
  { left: '43%', top: '121%', width: '7%', height: '7%' },
  { left: '92%', top: '120%', width: '5%', height: '5%' }
]
function trashStyle(i) {
  return TRASH_STYLES[i]
}

// ─── 4 物品：[围栏, 高压警示牌, 工作牌, 安全须知] ───
const itemPlaced = reactive([false, false, false, false])
const allItemsPlaced = computed(() => itemPlaced.every(v => v))

// ─── 验电笔 ───
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
// 鼠标是否位于插座区域：在插座上显示验电状态图，离开插座切回正常图
const vtProbing = ref(false)
const vtImg = computed(() =>
  vtProbing.value ? Images.barVoltageTesterWarning : Images.barVoltageTesterNormal
)

// ─── 跟随 ───
const followingToolIdx = ref(null)
const isFollowing = ref(false)
const cursorFollowingStyle = ref({})
const leftToolImgs = [
  Images.barLeftFence,
  Images.barSignStopHighVoltage,
  Images.barSignPersonWorking,
  Images.barSafetyNotice
]
const followingImg = computed(() =>
  followingToolIdx.value != null ? leftToolImgs[followingToolIdx.value] : Images.barLeftFence
)
const showFollowing = computed(() => isFollowing.value || vtActive.value)
const followImg = computed(() => (vtActive.value ? vtImg.value : followingImg.value))
function moveCursorTo(e) {
  if (e) cursorFollowingStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
}

// ─── 中间区域 ───
const cabinetGroupRef = ref(null)
const middleAreaStyle = ref({})

// ★ 放置坐标（相对 cabinet-group 即柜体图像尺寸的 %，用户按需调整）
const LEFT_FENCE_STYLE = { left: '-15.5%', top: '60.3%', width: '58.5%', height: 'auto' }
const RIGHT_FENCE_STYLE = { left: '61.2%', top: '64.9%', width: '46.5%', height: 'auto' }
const LEFT_SIGN_HV_STYLE = { left: '1.3%', top: '98.15%', width: '10.6%', height: 'auto' }
const RIGHT_SIGN_HV_STYLE = { left: '78.1%', top: '96.85%', width: '10.6%', height: 'auto' }
const SIGN_WORKING_STYLE = { left: '43.5%', top: '43%', width: '10%', height: 'auto' }
const SAFETY_NOTICE_STYLE = { left: '22.5%', top: '42%', width: '15%', height: 'auto' }
// ★ 三步验电第2步点击区域（相对 cabinet-group 的百分比 left/top/宽/高，用户按需调整）
// ★ 三步验电第2步点击区域（相对 cabinet-group 百分比；第4步计量小室与第12步终端小室位置不同）
const CABINET_CHECK_ZONES = {
  4: { left: 59, top: 65, width: 5, height: 7 },
  12: { left: 59, top: 23.5, width: 5, height: 7 }
}
const checkZone = computed(() => CABINET_CHECK_ZONES[props.stepOrder] || CABINET_CHECK_ZONES[4])
const hitZoneStyle = computed(() => {
  const z = checkZone.value
  return {
    left: z.left + '%',
    top: z.top + '%',
    width: z.width + '%',
    height: z.height + '%'
  }
})
// ★ 围栏图片宽高比（运行时从实际图片获取，用于命中检测）
const fenceAspect = reactive({ left: 1, right: 1 })
function loadImageAspect(src, key) {
  const img = new Image()
  img.onload = () => {
    fenceAspect[key] = img.naturalWidth / img.naturalHeight
  }
  img.src = src
}

/** 根据 STYLE + cabinet-group 实际尺寸 + 图片宽高比 命中检测 */
function hitFenceFromStyle(e, style, aspectKey) {
  const g = cabinetGroupRef.value
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

// ============== 方法 ==============

/** 选中工具：已放置则报错；步骤4禁止放置 */
function selectTool(idx, e) {
  if (isStep4.value) {
    ElMessage.warning('步骤4无需重复放置物品，请直接验电')
    emit('error')
    return
  }
  vtActive.value = false
  emit('operation')
  if (itemPlaced[idx]) {
    ElMessage.warning('该物品已放置')
    emit('error')
    return
  }
  const on = followingToolIdx.value !== idx
  followingToolIdx.value = on ? idx : null
  isFollowing.value = on
  if (on) moveCursorTo(e)
}

function toggleVoltageTester(e) {
  e?.stopPropagation?.()
  emit('operation')
  if (!isStep4.value) {
    ElMessage.warning('请先观看教学视频，进入验电步骤后再操作')
    emit('error')
    return
  }
  if (!allItemsPlaced.value) {
    ElMessage.warning('请先完成围栏与标示牌放置')
    emit('error')
    return
  }
  if (vtDone.value) {
    ElMessage.warning('验电已完成')
    emit('error')
    return
  }
  vtActive.value
    ? ((vtActive.value = false), (vtStep.value = 0), (vtProbing.value = false))
    : ((vtActive.value = true), (vtStep.value = 0), moveCursorTo(e))
}

function onPageMouseMove(e) {
  if (isFollowing.value || vtActive.value) moveCursorTo(e)
}

/** 验电笔按住插座（mousedown）时切换验电状态图，松开（mouseup）恢复正常图 */
function onPageMouseDown(e) {
  if (!vtActive.value) return
  const s = cabinetGroupRef.value?.querySelector('.power-socket-img')
  vtProbing.value = !!s && hitTest(e, s)
}
function onPageMouseUp() {
  vtProbing.value = false
}

/** 柜体图像区域内点击命中检测 */
function hitCabinetImg(e) {
  const cab = cabinetGroupRef.value?.querySelector('.cabinet-img')
  if (!cab) return false
  const r = cab.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
}

/** 三步验电第2步：CABINET_CHECK_ZONE 矩形区域内命中检测（与热区可视化范围一致） */
function hitCheckZone(e) {
  const g = cabinetGroupRef.value
  if (!g) return false
  const z = checkZone.value
  const r = g.getBoundingClientRect()
  const x = r.left + (r.width * z.left) / 100
  const y = r.top + (r.height * z.top) / 100
  const w = (r.width * z.width) / 100
  const h = (r.height * z.height) / 100
  return e.clientX >= x && e.clientX <= x + w && e.clientY >= y && e.clientY <= y + h
}

function onMiddleAreaClick(e) {
  if (!isFollowing.value) return
  const idx = followingToolIdx.value
  if (idx == 0 || idx === 1) {
    // 命中检测根据 STYLE + 图片宽高比自动计算，只需维护 STYLE 即可
    const hit =
      hitFenceFromStyle(e, LEFT_FENCE_STYLE, 'left') ||
      hitFenceFromStyle(e, RIGHT_FENCE_STYLE, 'right')
    hit
      ? ((itemPlaced[idx] = true), finishPlacement())
      : (ElMessage.warning('请选择正确的放置位置'), emit('error'))
  } else if (idx === 2 || idx === 3) {
    hitCabinetImg(e)
      ? ((itemPlaced[idx] = true), finishPlacement())
      : (ElMessage.warning('请选择正确的放置位置'), emit('error'))
  }
}

function finishPlacement() {
  followingToolIdx.value = null
  isFollowing.value = false
  if (allItemsPlaced.value) emit('fencesDone')
}

// ─── 验电笔交互 ───
function hitTest(e, el) {
  if (!el) return false
  const r = el.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
}
function onPageClick(e) {
  if (!vtActive.value || vtStep.value === 3) return
  emit('operation')
  const g = cabinetGroupRef.value
  if (!g) return
  const s = g.querySelector('.power-socket-img')
  if (vtStep.value === 0) hitTest(e, s) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  else if (vtStep.value === 1)
    hitCheckZone(e) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  else if (vtStep.value === 2) hitTest(e, s) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  if (vtStep.value === 3) {
    vtDone.value = true
    vtActive.value = false
    vtProbing.value = false
    emit('voltageCheckDone')
  }
}

// ─── 供父组件调用的恢复/标记方法 ───
function markPlacedForStep4() {
  itemPlaced.splice(0, 4, true, true, true, true)
}

function restoreDraft(d) {
  if (d?.itemPlaced)
    d.itemPlaced.forEach((v, i) => {
      itemPlaced[i] = v
    })
  if (d?.vtDone) vtDone.value = true
  if (d?.vtStep != null) vtStep.value = d.vtStep
}

// 同组件导航（步骤3→4）时，onMounted 不会重跑，需 watch 监听
watch(isStep4, val => {
  if (val) {
    const skipFlag = sessionStorage.getItem('_hcl_step4_skip_placement')
    if (skipFlag) {
      markPlacedForStep4()
      sessionStorage.removeItem('_hcl_step4_skip_placement')
    } else if (!itemPlaced.some(v => v)) {
      markPlacedForStep4()
    }
  }
})

// ─── 中间区域尺寸 ───
function updateMiddleArea() {
  middleAreaStyle.value = { left: '12vw', right: '12vw', top: '5vh', bottom: '5vh' }
}
onMounted(() => {
  // 步骤4/12：物品已放置（步骤3已提交过）。步骤12 为 v-if 重挂载，watch 不触发，需在此兜底；
  // 父组件 restoreDraft 后执行，有草稿时以草稿为准
  if (isStep4.value) {
    const skipFlag = sessionStorage.getItem('_hcl_step4_skip_placement')
    if (skipFlag) {
      markPlacedForStep4()
      sessionStorage.removeItem('_hcl_step4_skip_placement')
    } else if (!itemPlaced.some(v => v)) {
      markPlacedForStep4()
    }
  }
  // 加载围栏图片宽高比（用于命中检测自动计算）
  loadImageAspect(Images.barLeftFence, 'left')
  loadImageAspect(Images.barRightFence, 'right')
  updateMiddleArea()
  window.addEventListener('resize', updateMiddleArea)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateMiddleArea)
})

defineExpose({
  itemPlaced,
  vtActive,
  vtDone,
  vtStep,
  followingToolIdx,
  showFollowing,
  selectTool,
  toggleVoltageTester,
  onPageMouseMove,
  onPageMouseDown,
  onPageMouseUp,
  onPageClick,
  markPlacedForStep4,
  restoreDraft
})
</script>

<style scoped>
/* 中间区域 */
.middle-area {
  position: fixed;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.middle-area.is-following {
  cursor: crosshair;
}

.cabinet-group {
  position: relative;
  display: inline-block;
}

.cabinet-img {
  max-width: 100%;
  max-height: 100%;
  display: block;
  user-select: none;
  pointer-events: auto;
}

/* 三步验电第2步点击热区（范围由 CABINET_CHECK_ZONE 百分比控制） */
.cabinet-hit-zone {
  position: absolute;
  border: 2px dashed rgba(0, 150, 255, 0.9);
  background: rgba(0, 150, 255, 0.12);
  pointer-events: none;
  z-index: 4;
}

.power-socket-img {
  position: absolute;
  left: -15%;
  top: 15%;
  width: 12%;
  max-width: 120px;
  object-fit: contain;
  pointer-events: auto;
}

/* 已放置物品（全部位于 cabinet-group 内，% 定位） */
.placed-img {
  position: absolute;
  object-fit: contain;
  pointer-events: none;
  z-index: 2;
  animation: fadeIn 0.6s ease-out forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

/* 鼠标跟随 */
.cursor-following {
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

.cursor-following img {
  width: 90%;
  height: 90%;
  object-fit: contain;
}

/* 步骤12 垃圾占位（后续替换为垃圾 png） */
.trash-placeholder {
  position: absolute;
  background: rgba(120, 120, 120, 0.5);
  border: 1px dashed rgba(80, 80, 80, 0.8);
  border-radius: 4px;
}
</style>
