<!-- 柜体局部操作：设围栏 + 挂告示牌(步骤3) + 三步验电(步骤4) -->
<template>
  <div class="cabinet-local-page" :class="{ 'is-following': isFollowing || vtActive }" @mousemove="onPageMouseMove"
    @click="onPageClick">

    <!-- 左侧物品栏 -->
    <div class="tool-bar tool-bar-left">
      <div v-for="(item, idx) in leftTools" :key="'L' + idx" class="tool-item tool-item-img"
        :class="{ 'tool-selected': followingToolIdx === idx, 'tool-placed': itemPlaced[idx] }"
        @click="selectTool(idx, $event)">
        <img :src="item.img" alt="" draggable="false" />
      </div>
    </div>

    <!-- 中间交互区域（cabinet-group 固定图像宽高比，所有物品 % 定位） -->
    <div class="middle-area" :style="middleAreaStyle" @click="onMiddleAreaClick">
      <div class="cabinet-group" ref="cabinetGroupRef">
        <img :src="Images.cabinetGroupOverview" alt="柜体局部" class="cabinet-img" draggable="false" />
        <img :src="Images.powerSocket" alt="电源插座" class="power-socket-img" draggable="false" />
        <img v-if="itemPlaced[0]" :src="Images.leftFence" class="placed-img" :style="LEFT_FENCE_STYLE" draggable="false" />
        <img v-if="itemPlaced[0]" :src="Images.rightFence" class="placed-img" :style="RIGHT_FENCE_STYLE" draggable="false" />
        <img v-if="itemPlaced[1]" :src="Images.signStopHighVoltage" class="placed-img" :style="LEFT_SIGN_HV_STYLE"
          draggable="false" />
        <img v-if="itemPlaced[1]" :src="Images.signStopHighVoltage" class="placed-img" :style="RIGHT_SIGN_HV_STYLE"
          draggable="false" />
        <img v-if="itemPlaced[2]" :src="Images.signPersonWorking" class="placed-img" :style="SIGN_WORKING_STYLE"
          draggable="false" />
        <img v-if="itemPlaced[3]" :src="Images.safetyNotice" class="placed-img" :style="SAFETY_NOTICE_STYLE"
          draggable="false" />
      </div>
    </div>

    <!-- 右侧物品栏（17 槽位，第 1 个为验电笔，其余留空） -->
    <div class="tool-bar tool-bar-right">
      <div v-for="i in 17" :key="'R' + i" class="tool-item tool-item-img"
        :class="{ 'tool-selected': i === 1 && vtActive, 'tool-placed': i === 1 && vtDone }"
        @click="i === 1 && selectVoltageTester($event)">
        <img v-if="i === 1" :src="Images.voltageTesterNormal" alt="验电笔" draggable="false" />
      </div>
    </div>

    <!-- 鼠标跟随 -->
    <div v-if="showFollowing" class="cursor-following" :style="cursorFollowingStyle">
      <img :src="followImg" alt="" draggable="false" />
    </div>

    <!-- 视频占位 -->
    <div v-if="showVideo" class="video-overlay">
      <div class="video-placeholder">
        <span>教学视频（待制作）</span>
        <el-button type="primary" size="large" @click="closeVideo">关闭，进入下一步</el-button>
      </div>
    </div>

    <!-- 按钮 -->
    <div class="save-bar-fixed" :class="{ saving }" @click="saveProgressDraft" title="保存进度" />
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img :src="Images.highWorkBg" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'
import PromptModal from '@/components/PromptModal.vue'
import Images from '@/assets/images'

const route = useRoute()
const router = useRouter()
const experimentId = ref(route.query.experimentId || '')
const stepId = ref(route.query.stepId || '')
const startedAt = ref(formatLocalTime(new Date()))
const showWorkBg = ref(false)
const showVideo = ref(false)
const hasSubmitted = ref(false)
const saving = ref(false)

// ─── 步骤模式判定（步骤3=放置物品+验电，步骤4=仅验电） ───
const stepsFromStore = JSON.parse(sessionStorage.getItem('experimentSteps') || '[]')
const currentStepOrder = computed(() => {
  if (!stepId.value) return 3 // 缺 stepId 默认按步骤3
  const s = stepsFromStore.find(s => s.stepId === stepId.value)
  return s ? s.stepOrder : 3
})
const isStep4 = computed(() => currentStepOrder.value === 4)

// ─── 4 物品：[围栏, 高压警示牌, 工作牌, 安全须知] ───
const leftTools = [
  { img: Images.leftFence }, { img: Images.signStopHighVoltage },
  { img: Images.signPersonWorking }, { img: Images.safetyNotice }
]
const itemPlaced = reactive([false, false, false, false])
const allItemsPlaced = computed(() => itemPlaced.every(v => v))

// ─── 验电笔 ───
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
const vtImg = computed(() => (vtStep.value === 1 || vtStep.value === 3) ? Images.voltageTesterWarning : Images.voltageTesterNormal)

// ─── 操作统计 ───
const stats = reactive({ duration_seconds: 0, operation_count: 0, error_count: 0 })
let timer = null
onMounted(() => { timer = setInterval(() => { stats.duration_seconds++ }, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })

// ─── 跟随 ───
const followingToolIdx = ref(null)
const isFollowing = ref(false)
const cursorFollowingStyle = ref({})
const followingImg = computed(() => followingToolIdx.value != null ? leftTools[followingToolIdx.value].img : Images.leftFence)
const showFollowing = computed(() => isFollowing.value || vtActive.value)
const followImg = computed(() => vtActive.value ? vtImg.value : followingImg.value)
function moveCursorTo(e) { if (e) cursorFollowingStyle.value = { left: (e.clientX + 12) + 'px', top: (e.clientY + 12) + 'px' } }

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
  img.onload = () => { fenceAspect[key] = img.naturalWidth / img.naturalHeight }
  img.src = src
}

/** 根据 STYLE + cabinet-group 实际尺寸 + 图片宽高比 命中检测 */
function hitFenceFromStyle(e, style, aspectKey) {
  const g = cabinetGroupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  const imgLeft = r.left + r.width * parseFloat(style.left) / 100
  const imgTop = r.top + r.height * parseFloat(style.top) / 100
  const imgW = r.width * parseFloat(style.width) / 100
  const imgH = imgW / fenceAspect[aspectKey]
  return e.clientX >= imgLeft && e.clientX <= imgLeft + imgW &&
    e.clientY >= imgTop && e.clientY <= imgTop + imgH
}

// ============== 方法 ==============

/** 选中工具：已放置则报错；步骤4禁止放置 */
function selectTool(idx, e) {
  if (isStep4.value) { ElMessage.warning('步骤4无需重复放置物品，请直接验电'); stats.error_count++; return }
  vtActive.value = false
  stats.operation_count++
  if (itemPlaced[idx]) { ElMessage.warning('该物品已放置'); stats.error_count++; return }
  const on = followingToolIdx.value !== idx
  followingToolIdx.value = on ? idx : null
  isFollowing.value = on
  if (on) moveCursorTo(e)
}

function selectVoltageTester(e) {
  e?.stopPropagation?.()
  stats.operation_count++
  if (!isStep4.value) { ElMessage.warning('请先观看教学视频，进入验电步骤后再操作'); stats.error_count++; return }
  if (!allItemsPlaced.value) { ElMessage.warning('请先完成围栏与标示牌放置'); stats.error_count++; return }
  if (vtDone.value) { ElMessage.warning('验电已完成'); stats.error_count++; return }
  vtActive.value ? (vtActive.value = false, vtStep.value = 0) : (vtActive.value = true, vtStep.value = 0, moveCursorTo(e))
}

function onPageMouseMove(e) { if (isFollowing.value || vtActive.value) moveCursorTo(e) }

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
    const hit = hitFenceFromStyle(e, LEFT_FENCE_STYLE, 'left') ||
      hitFenceFromStyle(e, RIGHT_FENCE_STYLE, 'right')
    hit ? (itemPlaced[idx] = true, finishPlacement()) : (ElMessage.warning('请选择正确的放置位置'), stats.error_count++)
  }
  else if (idx === 2 || idx === 3) {
    hitCabinetImg(e) ? (itemPlaced[idx] = true, finishPlacement()) : (ElMessage.warning('请选择正确的放置位置'), stats.error_count++)
  }
}

function finishPlacement() { followingToolIdx.value = null; isFollowing.value = false; checkAllDone() }

async function checkAllDone() {
  if (!allItemsPlaced.value || hasSubmitted.value) return
  hasSubmitted.value = true
  try {
    await submitStep({
      experimentId: experimentId.value, stepId: stepId.value, status: 1,
      durationSeconds: stats.duration_seconds, operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score: Math.max(0, 100 - stats.error_count * 10),
      resultData: JSON.stringify({ itemPlaced: [...itemPlaced] }),
      startedAt: startedAt.value
    })
    ElMessage.success('围栏与标示牌放置完成')
    showVideo.value = true
  } catch (err) { ElMessage.error('提交失败：' + (err.response?.data?.message || err.message)); hasSubmitted.value = false }
}

function closeVideo() {
  showVideo.value = false
  const next = stepsFromStore.find(s => s.stepOrder === 4)
  if (next) {
    sessionStorage.setItem('_hcl_step4_skip_placement', 'true')
    router.push({ path: '/HCL', query: { experimentId: experimentId.value, stepId: next.stepId } })
  }
}

// ─── 验电笔交互 ───
function hitTest(e, el) { if (!el) return false; const r = el.getBoundingClientRect(); return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom }
function onPageClick(e) {
  if (!vtActive.value || vtStep.value === 3) return
  stats.operation_count++
  const g = cabinetGroupRef.value; if (!g) return
  const s = g.querySelector('.power-socket-img'); const c = g.querySelector('.cabinet-img')
  if (vtStep.value === 0) hitTest(e, s) ? vtStep.value++ : (vtStep.value = 0, stats.error_count++)
  else if (vtStep.value === 1) hitTest(e, c) ? vtStep.value++ : (vtStep.value = 0, stats.error_count++)
  else if (vtStep.value === 2) hitTest(e, s) ? vtStep.value++ : (vtStep.value = 0, stats.error_count++)
  if (vtStep.value === 3) { vtDone.value = true; vtActive.value = false; submitVoltageCheck() }
}

/** 验电完成 → 提交当前步骤并跳转下一步 */
async function submitVoltageCheck() {
  //传递到后端的 payload
  const payload = {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status: 1,
    durationSeconds: stats.duration_seconds,
    operationCount: stats.operation_count,
    errorCount: stats.error_count,
    score: 100.00 - (stats.error_count * 10) > 0 ? 100.00 - (stats.error_count * 10) : 0,//最低得分为0分
    resultData: JSON.stringify({ vtDone: true, vtStep: 3 }),
    startedAt: startedAt.value
  }

  try {
    await submitStep(payload)
    ElMessage.success('验电操作完成！')
    setTimeout(() => {
      router.push({
        path: '/',
        query: { experimentId: experimentId.value }
      })
    }, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  }
}

// ─── 存档 ───
const saveProgressDraft = async () => {
  saving.value = true
  try {
    await saveDraft({
      experimentId: experimentId.value, stepId: stepId.value, status: 0,
      durationSeconds: stats.duration_seconds, operationCount: stats.operation_count,
      errorCount: stats.error_count,
      resultData: JSON.stringify({ itemPlaced: [...itemPlaced], vtDone: vtDone.value, vtStep: vtStep.value }),
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) { ElMessage.error('保存失败：' + (err.response?.data?.message || err.message)) } finally { saving.value = false }
}

// ─── 恢复草稿 + 中间区域 ───
function updateMiddleArea() {
  middleAreaStyle.value = { left: '12vw', right: '12vw', top: '5vh', bottom: '5vh' }
}
onMounted(async () => {
  // 步骤4：没有草稿时自动标记物品已放置（步骤3已提交过）
  if (isStep4.value) {
    const skipFlag = sessionStorage.getItem('_hcl_step4_skip_placement')
    if (skipFlag) {
      itemPlaced.splice(0, 4, true, true, true, true)
      sessionStorage.removeItem('_hcl_step4_skip_placement')
    }
  }
  if (experimentId.value && stepId.value) {
    try {
      const d = await getStepDraft(experimentId.value, stepId.value)
      if (d?.itemPlaced) d.itemPlaced.forEach((v, i) => { itemPlaced[i] = v })
      if (d?.vtDone) vtDone.value = true
      if (d?.vtStep != null) vtStep.value = d.vtStep
    } catch (_) { }
  }
  // 步骤4无草稿时确保物品显示为已放置
  if (isStep4.value && !itemPlaced.some(v => v)) {
    itemPlaced.splice(0, 4, true, true, true, true)
  }
  // 加载围栏图片宽高比（用于命中检测自动计算）
  loadImageAspect(Images.leftFence, 'left')
  loadImageAspect(Images.rightFence, 'right')
  updateMiddleArea()
  window.addEventListener('resize', updateMiddleArea)
})
onUnmounted(() => { window.removeEventListener('resize', updateMiddleArea) })
</script>

<style scoped>
.cabinet-local-page {
  width: 100vw;
  height: 100vh;
  background: #fff;
  position: relative;
  overflow: hidden;
}

/* 物品栏 */
.tool-bar {
  position: fixed;
  top: 10vh;
  height: 80vh;
  width: 10vw;
  min-width: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.2vh;
  padding: 10vh 0;
  z-index: 50;
  background: #1B7C78;
  border-radius: 1rem;
  overflow-y: auto;
  scrollbar-width: none;
}

.tool-bar::-webkit-scrollbar {
  display: none;
}

.tool-bar-left {
  left: 1vw;
}

.tool-bar-right {
  right: 1vw;
}

.tool-item {
  width: 80%;
  aspect-ratio: 1;
  border-radius: 50%;
  border: 2px solid rgba(0, 0, 0, .2);
  background: rgba(0, 0, 0, .04);
  flex-shrink: 0;
  cursor: pointer;
  transition: border-color .2s, background .2s;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.tool-item:hover {
  border-color: rgba(100, 180, 255, .8);
  background: rgba(100, 180, 255, .12);
}

.tool-item-img img {
  width: 80%;
  height: 80%;
  object-fit: contain;
  pointer-events: none;
}

.tool-selected {
  border-color: #4ade80;
  background: rgba(74, 222, 128, .2);
  box-shadow: 0 0 12px rgba(74, 222, 128, .6);
}

.tool-placed {
  opacity: .4;
  border-color: #999;
  pointer-events: auto;
}

/* 中间区域 */
.middle-area {
  position: fixed;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.is-following .middle-area {
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
  animation: fadeIn .6s ease-out forwards;
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
  opacity: .85;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, .5));
}

.cursor-following img {
  width: 90%;
  height: 90%;
  object-fit: contain;
}

/* 视频占位 */
.video-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, .85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-placeholder {
  color: #fff;
  font-size: 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
}

/* 固定按钮 */
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

.save-bar-fixed.disabled {
  opacity: .4;
  pointer-events: none;
}

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
