<!-- 配电室总览：<img> + 等比容器 + 横放等腰梯形热区 + 工作背景弹窗 + 保存按钮(占位) -->
<template>
  <div class="scene-page">
    <div class="image-wrapper" ref="wrapperRef">
      <img ref="imgRef" src="@/assets/images/DistributionRoomPanorama.png" alt="配电室总览" class="scene-bg"
        draggable="false" />
      <!-- 热区：JS 动态对齐 img 实际渲染区域 -->
      <div class="cabinet-hotspot" title="点击进入设备区操作" @click="enterCabinet">
        <span class="hotspot-label">点击进入设备区操作</span>
      </div>
    </div>

    <div class="work-bg-btn" @click="showWorkBg = true" title="查看工作背景" />
    <div class="save-bar-fixed">
      <el-button type="info" size="default" disabled>
        <el-icon>
          <FolderOpened />
        </el-icon> 保存进度
      </el-button>
    </div>
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
  router.push({
    path: '/HCabinetLocal',
    query: { experimentId: route.query.experimentId || '' }
  })
}

/** 测量 img 在 object-fit:contain 下的实际内容区域，调整热区 */
function alignHotspot() {
  const img = imgRef.value
  const wrapper = wrapperRef.value
  if (!img || !wrapper) return

  const wW = wrapper.clientWidth
  const wH = wrapper.clientHeight
  const iW = img.naturalWidth
  const iH = img.naturalHeight
  const imgRatio = iW / iH
  const wrapRatio = wW / wH

  let rW, rH, offX, offY
  if (imgRatio > wrapRatio) {
    rW = wW; rH = wW / imgRatio; offX = 0; offY = (wH - rH) / 2
  } else {
    rH = wH; rW = wH * imgRatio; offX = (wW - rW) / 2; offY = 0
  }

  wrapper.style.setProperty('--rx', rW + 'px')
  wrapper.style.setProperty('--ry', rH + 'px')
  wrapper.style.setProperty('--ox', offX + 'px')
  wrapper.style.setProperty('--oy', offY + 'px')
}

let observer = null
onMounted(() => {
  alignHotspot()
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

/* 横放等腰梯形 — JS注入CSS变量(--ox/--oy/--rx/--ry)动态对齐img内容区 */
.cabinet-hotspot {
  position: absolute;
  left: calc(var(--ox, 0px) + 5 * var(--rx, 1px) / 100);
  top: calc(var(--oy, 0px) + 10 * var(--ry, 1px) / 100);
  width: calc(68 * var(--rx, 1px) / 100);
  height: calc(75 * var(--ry, 1px) / 100);
  clip-path: polygon(2% 2%,
      72% 15%,
      72% 85%,
      2% 98%);
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

/* 查看工作背景按钮（左下角，图片预留） */
.work-bg-btn {
  position: fixed;
  bottom: 24px;
  left: 24px;
  z-index: 100;
  width: 60px;
  height: 60px;
  cursor: pointer;
  background: rgba(255, 255, 255, .6);
  border: 1px dashed #73BCBB;
  border-radius: 8px;
  transition: transform 0.2s;
}

.work-bg-btn:hover {
  transform: scale(1.05);
}

.save-bar-fixed {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}

.work-bg-img {
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
