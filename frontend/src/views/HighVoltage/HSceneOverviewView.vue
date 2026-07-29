<!-- 配电室总览：<img> + 等比容器 + 横放等腰梯形热区 + 工作背景弹窗 + 保存按钮(占位) -->
<template>
  <div class="scene-page">
    <!-- 等比容器：始终与图片实际渲染区域重合，热区基于此定位 -->
    <div class="image-wrapper" ref="wrapperRef">
      <img ref="imgRef" src="@/assets/images/DistributionRoomPanorama.jpg" alt="配电室总览" class="scene-bg"
        draggable="false" />
      <!-- 横放等腰梯形热区：左底长(高)、右底短(低) -->
      <div class="cabinet-hotspot" title="点击进入设备区操作" @click="enterCabinet">
        <span class="hotspot-label">点击进入设备区操作</span>
      </div>
    </div>

    <!-- 查看工作任务按钮（左下角） -->
    <div class="work-task-btn" @click="showWorkBg = true" title="查看工作任务" />

    <div class="save-bar-fixed disabled" title="保存进度" />

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img src="@/assets/images/HighWorkBackground.png" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FolderOpened } from '@element-plus/icons-vue'
import PromptModal from '@/components/PromptModal.vue'

const route = useRoute()
const router = useRouter()
const showWorkBg = ref(false)
const imgRef = ref(null)
const wrapperRef = ref(null)

function enterCabinet() {
  const steps = JSON.parse(sessionStorage.getItem('experimentSteps') || '[]')
  const step3 = steps.find(s => s.stepOrder === 3)
  router.push({
    path: '/HCL',
    query: { experimentId: route.query.experimentId || '', stepId: step3?.stepId || '' }
  })
}

/** 根据 object-fit:contain 的实际渲染区域注入 CSS 变量 */
function alignHotspot() {
  const img = imgRef.value
  const wrapper = wrapperRef.value
  if (!img || !wrapper) return
  const wW = wrapper.clientWidth, wH = wrapper.clientHeight
  const ratio = img.naturalWidth / img.naturalHeight
  const wrapRatio = wW / wH
  let rW, rH, oX, oY
  if (ratio > wrapRatio) {
    rW = wW; rH = wW / ratio; oX = 0; oY = (wH - rH) / 2
  } else {
    rH = wH; rW = wH * ratio; oX = (wW - rW) / 2; oY = 0
  }
  wrapper.style.setProperty('--rx', rW + 'px')
  wrapper.style.setProperty('--ry', rH + 'px')
  wrapper.style.setProperty('--ox', oX + 'px')
  wrapper.style.setProperty('--oy', oY + 'px')
}

let observer = null
onMounted(() => {
  // 等待图片加载完成后再测量（否则 naturalWidth/Height 为 0）
  const img = imgRef.value
  if (img) {
    if (img.complete) {
      alignHotspot()
    } else {
      img.addEventListener('load', alignHotspot, { once: true })
    }
  }
  observer = new ResizeObserver(alignHotspot)
  if (imgRef.value) observer.observe(imgRef.value)
  if (wrapperRef.value) observer.observe(wrapperRef.value)
  window.addEventListener('resize', alignHotspot)
})
onUnmounted(() => {
  if (observer) observer.disconnect()
  window.removeEventListener('resize', alignHotspot)
})
</script>

<style scoped>
.scene-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFFFFF;
}

/* 容器紧贴 img 实际渲染尺寸，热区百分比与图片等比例缩放 */
.image-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  line-height: 0;
}

.scene-bg {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

/* 横放等腰梯形 — JS注入CSS变量对齐img内容区，坐标沿用用户调好的百分比 */
.cabinet-hotspot {
  position: absolute;
  left: calc(var(--ox, 0px) + 10 * var(--rx, 1px) / 100);
  top: calc(var(--oy, 0px) + 10 * var(--ry, 1px) / 100);
  width: calc(80 * var(--rx, 1px) / 100);
  height: calc(80 * var(--ry, 1px) / 100);
  clip-path: polygon(28% 27%, 62% 30%, 62% 73%, 28% 89%);
  cursor: pointer;
  z-index: 5;
  transition: background 0.25s, box-shadow 0.25s;
}

/* 以下样式不变 */

.cabinet-hotspot:hover {
  background: rgba(0, 210, 255, 0.12);
  box-shadow: inset 0 0 0 3px #00d2ff, 0 0 24px rgba(0, 210, 255, 0.25);
}

.cabinet-hotspot:hover .hotspot-label {
  opacity: 1;
}

.hotspot-label {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  color: #00d2ff;
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, .7);
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  user-select: none;
}

/* 查看工作任务按钮（左下角） */
.work-task-btn {
  position: fixed;
  bottom: 24px;
  left: 24px;
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

.save-bar-fixed {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
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

.save-bar-fixed.disabled {
  opacity: .4;
  pointer-events: none;
}

.save-bar-fixed.disabled {
  opacity: .4;
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
