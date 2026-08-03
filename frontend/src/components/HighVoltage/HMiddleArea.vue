<!-- 中间交互区域：围栏/告示牌放置（步骤3）+ 三步验电（步骤4）+ 鼠标跟随 -->
<template>
  <!-- 中间交互区域（cabinet-group 固定图像宽高比，所有物品 % 定位） -->
  <div
    class="middle-area"
    :class="{ 'is-following': showFollowing }"
    :style="middleAreaStyle"
    @click="onMiddleAreaClick"
  >
    <div class="cabinet-group" ref="cabinetGroupRef">
      <img
        :src="Images.cabinetGroupOverview"
        alt="柜体局部"
        class="cabinet-img"
        draggable="false"
      />
      <img :src="Images.powerSocket" alt="电源插座" class="power-socket-img" draggable="false" />
      <img
        v-if="itemPlaced[0]"
        :src="Images.barLeftFence"
        class="placed-img"
        :style="LEFT_FENCE_STYLE"
        draggable="false"
      />
      <img
        v-if="itemPlaced[0]"
        :src="Images.barRightFence"
        class="placed-img"
        :style="RIGHT_FENCE_STYLE"
        draggable="false"
      />
      <img
        v-if="itemPlaced[1]"
        :src="Images.barSignStopHighVoltage"
        class="placed-img"
        :style="LEFT_SIGN_HV_STYLE"
        draggable="false"
      />
      <img
        v-if="itemPlaced[1]"
        :src="Images.barSignStopHighVoltage"
        class="placed-img"
        :style="RIGHT_SIGN_HV_STYLE"
        draggable="false"
      />
      <img
        v-if="itemPlaced[2]"
        :src="Images.barSignPersonWorking"
        class="placed-img"
        :style="SIGN_WORKING_STYLE"
        draggable="false"
      />
      <img
        v-if="itemPlaced[3]"
        :src="Images.barSafetyNotice"
        class="placed-img"
        :style="SAFETY_NOTICE_STYLE"
        draggable="false"
      />
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

const isStep4 = computed(() => props.stepOrder === 4)

// ─── 4 物品：[围栏, 高压警示牌, 工作牌, 安全须知] ───
const itemPlaced = reactive([false, false, false, false])
const allItemsPlaced = computed(() => itemPlaced.every(v => v))

// ─── 验电笔 ───
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
const vtImg = computed(() =>
  vtStep.value === 1 || vtStep.value === 3
    ? Images.barVoltageTesterWarning
    : Images.barVoltageTesterNormal
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
const LEFT_SIGN_HV_STYLE = { left: '1.5%', top: '98.5%', width: '10.6%', height: 'auto' }
const RIGHT_SIGN_HV_STYLE = { left: '78.3%', top: '97.2%', width: '10.6%', height: 'auto' }
const SIGN_WORKING_STYLE = { left: '43.5%', top: '43%', width: '10%', height: 'auto' }
const SAFETY_NOTICE_STYLE = { left: '22.5%', top: '42%', width: '15%', height: 'auto' }
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
    ? ((vtActive.value = false), (vtStep.value = 0))
    : ((vtActive.value = true), (vtStep.value = 0), moveCursorTo(e))
}

function onPageMouseMove(e) {
  if (isFollowing.value || vtActive.value) moveCursorTo(e)
}

/** 柜体图像区域内点击命中检测 */
function hitCabinetImg(e) {
  const cab = cabinetGroupRef.value?.querySelector('.cabinet-img')
  if (!cab) return false
  const r = cab.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
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
  const c = g.querySelector('.cabinet-img')
  if (vtStep.value === 0) hitTest(e, s) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  else if (vtStep.value === 1) hitTest(e, c) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  else if (vtStep.value === 2) hitTest(e, s) ? vtStep.value++ : ((vtStep.value = 0), emit('error'))
  if (vtStep.value === 3) {
    vtDone.value = true
    vtActive.value = false
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
  // 步骤4：没有草稿时自动标记物品已放置（步骤3已提交过）
  if (isStep4.value) {
    const skipFlag = sessionStorage.getItem('_hcl_step4_skip_placement')
    if (skipFlag) {
      markPlacedForStep4()
      sessionStorage.removeItem('_hcl_step4_skip_placement')
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
</style>
