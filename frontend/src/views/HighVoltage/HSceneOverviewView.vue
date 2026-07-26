<!-- 配电室总览：全景背景(contain) + 梯形热区 + 工作背景弹窗 + 保存按钮(占位) -->
<template>
  <div class="scene-page">
    <!-- 梯形热区 — 覆盖进线柜+计量柜+出线柜，左底长右底短 -->
    <div class="cabinet-hotspot" title="点击进入设备区操作" @click="enterCabinet">
      <span class="hotspot-label">点击进入设备区操作</span>
    </div>

    <!-- 查看工作背景按钮（左下角） -->
    <div class="work-bg-btn" @click="showWorkBg = true" title="查看工作背景" />

    <!-- 保存进度按钮（右下角，仅样式占位，不实际保存） -->
    <div class="save-bar-fixed">
      <el-button type="info" size="default" disabled>
        <el-icon><FolderOpened /></el-icon> 保存进度
      </el-button>
    </div>

    <!-- 高压工作背景弹窗 -->
    <PromptModal :visible="showWorkBg" @close="showWorkBg = false">
      <img src="@/assets/images/HighWorkBackground.png" alt="高压工作背景" class="work-bg-img" />
    </PromptModal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FolderOpened } from '@element-plus/icons-vue'
import PromptModal from '@/components/PromptModal.vue'

const route = useRoute()
const router = useRouter()
const showWorkBg = ref(false)

function enterCabinet() {
  router.push({
    path: '/HCabinetLocal',
    query: { experimentId: route.query.experimentId || '' }
  })
}
</script>

<style scoped>
.scene-page {
  width: 100vw;
  height: 100vh;
  position: relative;
  background-image: url('@/assets/images/DistributionRoomPanorama.png');
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-color: #020617;
}

/* 梯形热区 — 左底长、右底短，覆盖进线柜+计量柜+出线柜 */
.cabinet-hotspot {
  position: absolute;
  /* 坐标需根据实际图片微调 */
  top: 18%;
  left: 8%;
  width: 58%;
  height: 55%;
  clip-path: polygon(0% 15%, 58% 15%, 65% 60%, 5% 60%);
  cursor: pointer;
  z-index: 5;
  transition: background 0.25s, box-shadow 0.25s;
}

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
