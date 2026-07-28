<!-- 柜体局部操作：进线柜+计量柜+出线柜一体化操作界面 -->
<template>
  <div class="cabinet-local-page" :class="{ 'is-following': isFollowing || vtActive }" @mousemove="onPageMouseMove"
    @click="onPageClick">
    <div class="cabinet-group" ref="cabinetGroupRef">
      <img :src="localBg" alt="柜体局部操作" class="cabinet-img" draggable="false" />
      <img :src="powerSocket" alt="电源插座" class="power-socket-img" draggable="false" />
      <!-- 工作牌（相对柜体图定位，保证不超出 CabinetGroupOverview.png） -->
      <img v-if="placedSignWorkingVisible" :src="signPersonWorking" class="placed-fence placed-on-cabinet"
        :style="SIGN_WORKING_STYLE" draggable="false" />
      <img v-if="placedSafetyNoticeVisible" :src="safetyNotice" class="placed-fence placed-on-cabinet"
        :style="SAFETY_NOTICE_STYLE" draggable="false" />
    </div>

    <!-- 中间区域（用于放置围栏，基于物品栏边界） -->
    <div class="middle-area" :style="middleAreaStyle" @click="onMiddleAreaClick"></div>

    <!-- 已放置的围栏与警示牌（独立层级，避免被中间区域遮挡） -->
    <div class="placed-layer" :style="middleAreaStyle">
      <img v-if="placedFenceVisible" :src="leftFence" class="placed-fence" :style="LEFT_FENCE_STYLE"
        draggable="false" />
      <img v-if="placedFenceVisible" :src="rightFence" class="placed-fence" :style="RIGHT_FENCE_STYLE"
        draggable="false" />
      <img v-if="placedSignHVVisible" :src="signStopHighVoltage" class="placed-fence" :style="LEFT_SIGN_HV_STYLE"
        draggable="false" />
      <img v-if="placedSignHVVisible" :src="signStopHighVoltage" class="placed-fence" :style="RIGHT_SIGN_HV_STYLE"
        draggable="false" />
    </div>

    <!-- 左侧物品栏 -->
    <div class="tool-bar" :style="leftBarStyle">
      <div class="tool-item tool-item-img" v-for="(img, idx) in leftTools" :key="'L-img' + idx"
        :class="{ 'tool-selected': followingToolIdx === idx }" @click="selectTool(idx, $event)">
        <img :src="img" alt="" draggable="false" />
      </div>
      <div class="tool-item" v-for="i in 17" :key="'L' + i"></div>
    </div>

    <!-- 右侧物品栏 -->
    <div class="tool-bar" :style="rightBarStyle">
      <div class="tool-item tool-item-img" :class="{ 'tool-selected': vtActive }" @click="selectVoltageTester($event)">
        <img :src="voltageTesterNormal" alt="验电笔" draggable="false" />
      </div>
      <div class="tool-item" v-for="i in 19" :key="'R' + i"></div>
    </div>

    <!-- 鼠标跟随的缩略图片 -->
    <div v-if="showFollowing" class="cursor-following" :style="cursorFollowingStyle">
      <img :src="followImg" alt="" draggable="false" />
    </div>

    <!-- 右下角保存进度 -->
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgress" title="保存进度" />

    <!-- 查看工作任务按钮（左下角） -->
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img src="@/assets/images/HighWorkBackground.png" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import localBg from '@/assets/images/CabinetGroupOverview.png'
import powerSocket from '@/assets/images/PowerSocket.png'
import leftFence from '@/assets/images/LeftFence.png'
import rightFence from '@/assets/images/RightFence.png'
import signStopHighVoltage from '@/assets/images/SignStopHighVoltage.png'
import signPersonWorking from '@/assets/images/SignPersonWorking.png'
import safetyNotice from '@/assets/images/safetyNotice.png'
import voltageTesterNormal from '@/assets/images/voltageTesterNormal.png'
import voltageTesterWarning from '@/assets/images/voltageTesterWarning.png'
import PromptModal from '@/components/PromptModal.vue'

const leftTools = [leftFence, signStopHighVoltage, signPersonWorking, safetyNotice]

const route = useRoute()
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)
const showWorkBg = ref(false)

const cabinetGroupRef = ref(null)
const leftBarStyle = ref({})
const rightBarStyle = ref({})

// ====== 围栏显示状态 ======
const placedFenceVisible = ref(false)       // 是否显示围栏（左+右同时显示）
const placedSignHVVisible = ref(false)      // 是否显示高压警示牌（左+右同时显示）
const placedSignWorkingVisible = ref(false) // 是否显示工作牌（SignPersonWorking）
const placedSafetyNoticeVisible = ref(false) // 是否显示安全须知（safetyNotice）
const followingToolIdx = ref(null)          // 当前跟随的工具索引（null=未选中）
const isFollowing = ref(false)              // 鼠标是否在跟随状态
const cursorFollowingStyle = ref({})        // 鼠标跟随图片样式

// 跟随图片源（根据工具索引）
const TOOL_IMGS = [leftFence, signStopHighVoltage, signPersonWorking, safetyNotice]
const followingImg = computed(() => TOOL_IMGS[followingToolIdx.value] || leftFence)

// ====== 验电笔交互状态机 ======
const vtActive = ref(false)   // 验电笔跟随模式是否激活
const vtStep = ref(0)         // 当前步骤：0→点电源插座 / 1→点柜体 / 2→点电源插座 / 3→完成
// 验电笔跟随缩略图（根据步骤切换 normal / warning）
const vtImg = computed(() =>
  (vtStep.value === 1 || vtStep.value === 3) ? voltageTesterWarning : voltageTesterNormal
)
// 鼠标跟随缩略图是否显示（左侧工具 或 验电笔 任一种模式）
const showFollowing = computed(() => isFollowing.value || vtActive.value)
// 当前跟随缩略图图片源
const followImg = computed(() => vtActive.value ? vtImg.value : followingImg.value)

// 中间区域样式（基于物品栏边界动态计算）
const middleAreaStyle = ref({})

// 左围栏位置（相对中间区域百分比）
// 左上角(16%, 51.2%)，右下角(42.5%, 84.9%)
const LEFT_FENCE_STYLE = {
  left: '16%',
  top: '51.2%',
  width: '26.5%',
  height: '33.7%'
}
// 右围栏位置（相对中间区域百分比）
const RIGHT_FENCE_STYLE = {
  left: '54.7%',
  top: '50.5%',
  width: '26.4%',
  height: '34%'
}

// 左围栏矩形（用于点击命中判断，百分比数值）
const LEFT_FENCE_RECT = { x1: 16, y1: 51.2, x2: 42.5, y2: 84.9 }
// 右围栏矩形（用于点击命中判断，百分比数值）
const RIGHT_FENCE_RECT = { x1: 54.7, y1: 50.5, x2: 81.1, y2: 84.5 }

// 高压警示牌位置：absolute 相对 .placed-layer（中间区域）定位
// left/top 百分比定位 + transform 像素级微调
const LEFT_SIGN_HV_STYLE = {
  position: 'absolute',
  left: '27.5%',
  top: '60%',
  width: '3.5%',
  height: '10%',
  transform: 'translateX(-25px) translateY(20px)'
}
const RIGHT_SIGN_HV_STYLE = {
  position: 'absolute',
  left: '66.15%',
  top: 'calc(59% + 20px)',
  width: '3.5%',
  height: '10%',
  transform: 'translateX(-5px)'
}

// 工作牌（SignPersonWorking）位置：相对 .cabinet-group（柜体背景图）定位
// 中间偏上、水平居中，尺寸缩小以保证不超出柜体图
// 水平居中：left = (100% - width) / 2
const SIGN_WORKING_STYLE = {
  position: 'absolute',
  left: '42.5%',
  top: 'calc(8% + 70px)',
  width: '12%',
  height: 'auto'
}

// 安全须知（safetyNotice）位置：相对 .cabinet-group 定位
// 位于 SignPersonWorking 左侧 80px（在 40px 基础上再向左平移 40px），高度一致，图片尺寸一致
const SAFETY_NOTICE_STYLE = {
  position: 'absolute',
  left: 'calc(42.5% - 80px)',
  top: 'calc(8% + 80px)',
  width: '12%',
  height: 'auto'
}

// 鼠标跟随缩略图定位到指针右下角（多处复用）
function moveCursorTo(e) {
  if (!e) return
  cursorFollowingStyle.value = {
    left: (e.clientX + 12) + 'px',
    top: (e.clientY + 12) + 'px'
  }
}

// 进入/退出左侧工具的鼠标跟随模式（依据当前是否已选中该工具做开关切换）
function setFollowing(idx, e) {
  const turnOn = followingToolIdx.value !== idx
  followingToolIdx.value = turnOn ? idx : null
  isFollowing.value = turnOn
  if (turnOn) moveCursorTo(e)
}

// 选中物品栏工具
function selectTool(idx, e) {
  vtActive.value = false  // 切换左侧工具时退出验电笔模式
  if (idx === 0) {
    // 左围栏工具
    if (placedFenceVisible.value) {
      // 已放置 → 取消放置（同时取消警示牌）
      placedFenceVisible.value = false
      placedSignHVVisible.value = false
      followingToolIdx.value = null
      isFollowing.value = false
    } else {
      setFollowing(0, e)
    }
  } else if (idx === 1) {
    // 高压警示牌工具（需先放置围栏；未放置则静默不进入跟随）
    if (!placedFenceVisible.value) return
    if (placedSignHVVisible.value) {
      placedSignHVVisible.value = false
      followingToolIdx.value = null
      isFollowing.value = false
    } else {
      setFollowing(1, e)
    }
  } else if (idx === 2) {
    // 工作牌（SignPersonWorking）工具
    if (placedSignWorkingVisible.value) {
      // 已放置 → 取消放置
      placedSignWorkingVisible.value = false
      followingToolIdx.value = null
      isFollowing.value = false
    } else {
      setFollowing(2, e)
    }
  } else if (idx === 3) {
    // 安全须知（safetyNotice）工具
    if (placedSafetyNoticeVisible.value) {
      // 已放置 → 取消放置
      placedSafetyNoticeVisible.value = false
      followingToolIdx.value = null
      isFollowing.value = false
    } else {
      setFollowing(3, e)
    }
  }
}

// 选中验电笔：进入/退出跟随模式
function selectVoltageTester(e) {
  e?.stopPropagation?.()
  if (vtActive.value) {
    vtActive.value = false
    vtStep.value = 0
  } else {
    vtActive.value = true
    vtStep.value = 0
    // 关闭左侧工具跟随，避免两模式冲突
    isFollowing.value = false
    followingToolIdx.value = null
    moveCursorTo(e)
  }
}

// 鼠标移动：更新跟随图片位置（鼠标右下角）
function onPageMouseMove(e) {
  if (!isFollowing.value && !vtActive.value) return
  moveCursorTo(e)
}

// 在柜体背景图区域内点击则放置对应图片；点错区域不提示、不放置
function tryPlaceOnCabinet(visibleRef, e) {
  const img = e.currentTarget?.querySelector?.('.cabinet-img') ||
    cabinetGroupRef.value?.querySelector('.cabinet-img')
  if (img) {
    const imgRect = img.getBoundingClientRect()
    if (e.clientX >= imgRect.left && e.clientX <= imgRect.right &&
      e.clientY >= imgRect.top && e.clientY <= imgRect.bottom) {
      visibleRef.value = true
      followingToolIdx.value = null
      isFollowing.value = false
    }
  } else {
    visibleRef.value = true
    followingToolIdx.value = null
    isFollowing.value = false
  }
}

// 点击中间区域：根据当前工具执行放置
function onMiddleAreaClick(e) {
  if (!isFollowing.value) return
  const toolIdx = followingToolIdx.value

  if (toolIdx === 0) {
    // 放置围栏（点击任意位置即可）
    placedFenceVisible.value = true
    followingToolIdx.value = null
    isFollowing.value = false
  } else if (toolIdx === 1) {
    // 放置高压警示牌：需点击围栏区域
    const area = e.currentTarget
    const rect = area.getBoundingClientRect()
    const px = (e.clientX - rect.left) / rect.width * 100
    const py = (e.clientY - rect.top) / rect.height * 100
    const inLeft = px >= LEFT_FENCE_RECT.x1 && px <= LEFT_FENCE_RECT.x2 &&
      py >= LEFT_FENCE_RECT.y1 && py <= LEFT_FENCE_RECT.y2
    const inRight = px >= RIGHT_FENCE_RECT.x1 && px <= RIGHT_FENCE_RECT.x2 &&
      py >= RIGHT_FENCE_RECT.y1 && py <= RIGHT_FENCE_RECT.y2
    if (inLeft || inRight) {
      placedSignHVVisible.value = true
      followingToolIdx.value = null
      isFollowing.value = false
    }
  } else if (toolIdx === 2) {
    // 放置工作牌（SignPersonWorking）：点击柜体背景图区域即可
    tryPlaceOnCabinet(placedSignWorkingVisible, e)
  } else if (toolIdx === 3) {
    // 放置安全须知（safetyNotice）：点击柜体背景图区域即可
    tryPlaceOnCabinet(placedSafetyNoticeVisible, e)
  }
}

// 判断点击是否落在指定元素的可视区域内
function hitTest(e, el) {
  if (!el) return false
  const r = el.getBoundingClientRect()
  return e.clientX >= r.left && e.clientX <= r.right &&
    e.clientY >= r.top && e.clientY <= r.bottom
}

// 验电笔序列：点错一次回到起点（保持跟随模式，可立即重试）
function resetVt() {
  vtStep.value = 0
}

// 点击页面：验电笔交互状态机
function onPageClick(e) {
  if (!vtActive.value || vtStep.value === 3) return
  const group = cabinetGroupRef.value
  if (!group) return
  const socket = group.querySelector('.power-socket-img')
  const cab = group.querySelector('.cabinet-img')
  const socketHit = hitTest(e, socket)
  const cabHit = hitTest(e, cab)

  if (vtStep.value === 0) {
    // 期望点击电源插座
    if (socketHit) {
      vtStep.value = 1
    } else {
      resetVt()
    }
  } else if (vtStep.value === 1) {
    // 期望点击柜体（CabinetGroupOverview）
    if (cabHit) {
      vtStep.value = 2
    } else {
      resetVt()
    }
  } else if (vtStep.value === 2) {
    // 期望再次点击电源插座
    if (socketHit) {
      vtStep.value = 3
    } else {
      resetVt()
    }
  }
}

function updateBarPositions() {
  const group = cabinetGroupRef.value
  if (!group) return
  const socket = group.querySelector('.power-socket-img')
  if (!socket) return
  const socketRect = socket.getBoundingClientRect()

  // 左侧物品栏：右侧边距 PowerSocket 左边缘 100px
  const barWidth = 80
  const leftBarRight = socketRect.left - 100
  const leftBarLeft = leftBarRight - barWidth

  // 右侧物品栏：关于屏幕中线对称
  const screenCenter = window.innerWidth / 2
  const leftBarCenter = leftBarLeft + barWidth / 2
  const distFromCenter = screenCenter - leftBarCenter
  const rightBarCenter = screenCenter + distFromCenter
  const rightBarLeft = rightBarCenter - barWidth / 2

  // 顶部距页面上部 80px，底部距页面底部 80px
  const top = 80
  const bottom = 80

  const common = {
    position: 'fixed',
    top: top + 'px',
    height: `calc(100vh - ${top + bottom}px)`,
    width: barWidth + 'px'
  }
  const leftBarActualLeft = leftBarLeft + 20
  const rightBarActualLeft = rightBarLeft - 20
  leftBarStyle.value = { ...common, left: leftBarActualLeft + 'px' }
  rightBarStyle.value = { ...common, left: rightBarActualLeft + 'px' }

  // 中间区域：左侧物品栏右边界 到 右侧物品栏左边界，上下各80px
  const middleLeft = leftBarActualLeft + barWidth
  const middleRight = rightBarActualLeft
  middleAreaStyle.value = {
    position: 'fixed',
    left: middleLeft + 'px',
    top: top + 'px',
    width: (middleRight - middleLeft) + 'px',
    height: `calc(100vh - ${top + bottom}px)`
  }
}

const saveProgress = async () => {
  saving.value = true
  try {
    await saveDraft({
      experimentId: experimentId.value,
      stepId: stepId.value,
      status: 0,
      durationSeconds: 0,
      resultData: null,
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  nextTick(() => {
    // 等待图片加载完成后再测量位置
    const img = cabinetGroupRef.value?.querySelector('.cabinet-img')
    if (img && !img.complete) {
      img.addEventListener('load', updateBarPositions, { once: true })
    }
    updateBarPositions()
  })
  window.addEventListener('resize', updateBarPositions)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateBarPositions)
})
</script>

<style scoped>
.cabinet-local-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  position: relative;
}

.cabinet-group {
  position: relative;
  display: inline-block;
  transform: translateY(-5vh);
}

.cabinet-img {
  display: block;
  max-width: 60vw;
  max-height: 75vh;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
}

.power-socket-img {
  position: absolute;
  left: calc(-8% - 80px);
  top: 15%;
  width: 12%;
  max-width: 120px;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
}

.middle-area {
  z-index: 10;
  pointer-events: auto;
  cursor: pointer;
}

.is-following .middle-area {
  cursor: crosshair;
}

.placed-layer {
  z-index: 11;
  pointer-events: none;
}

/* 工作牌：相对柜体背景图定位，显示在柜体图上方，不拦截点击 */
.placed-on-cabinet {
  z-index: 12;
  pointer-events: none;
  object-fit: contain;
}

.cursor-following {
  position: fixed;
  z-index: 999;
  pointer-events: none;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.85;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.5));
}

.cursor-following img {
  width: 52px;
  height: 52px;
  object-fit: contain;
}

.placed-fence {
  position: absolute;
  object-fit: fill;
  pointer-events: none;
  animation: fenceFadeIn 0.6s ease-out forwards;
}

@keyframes fenceFadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

.tool-bar {
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  z-index: 50;
  min-height: 0;
  background: #1B7C78;
  border-radius: 12px;
  /* 隐藏滚动条但保留滚动功能 */
  scrollbar-width: none;
  /* Firefox */
  -ms-overflow-style: none;
  /* IE / 旧 Edge */
}

.tool-bar::-webkit-scrollbar {
  display: none;
  /* Chrome / Safari / 新 Edge */
}

.tool-item {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 2px solid rgba(0, 0, 0, 0.2);
  background: rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.tool-item:hover {
  border-color: rgba(100, 180, 255, 0.8);
  background: rgba(100, 180, 255, 0.12);
}

.tool-selected {
  border-color: #4ade80;
  background: rgba(74, 222, 128, 0.2);
  box-shadow: 0 0 12px rgba(74, 222, 128, 0.6);
}

.tool-item-img {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.tool-item-img img {
  width: 52px;
  height: 52px;
  object-fit: contain;
  pointer-events: none;
}

.save-bar-fixed {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
  cursor: pointer;
  background-image: url('@/assets/images/SaveProgressIcon.png');
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
}

.save-bar-fixed:hover {
  background-image: url('@/assets/images/SaveProgressIconHover.png');
  transform: scale(1.05);
}

.save-bar-fixed.saving {
  opacity: .6;
  pointer-events: none;
}

/* 查看工作任务按钮（左下角） */
.work-task-btn {
  position: fixed;
  bottom: 1.5rem;
  left: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
  cursor: pointer;
  background-image: url('@/assets/images/WorkTaskButton.png');
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
}

.work-task-btn:hover {
  background-image: url('@/assets/images/WorkTaskButtonHover.png');
  transform: scale(1.05);
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
