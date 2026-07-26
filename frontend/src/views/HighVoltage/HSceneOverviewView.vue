<!-- 配电室总览：全景背景 + 设备区热区 -->
<template>
  <div class="scene-page">
    <SceneFrame :src="sceneBg" alt="配电室总览" aspect-ratio="16 / 9">
      <!-- 覆盖进线柜+计量柜+出线柜的梯形热区 -->
      <div
        class="cabinet-hotspot"
        title="点击进入设备区操作"
        @click="enterCabinet"
      >
        <span class="hotspot-label">点击进入设备区操作</span>
      </div>
    </SceneFrame>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import SceneFrame from '@/components/HighVoltage/SceneFrame.vue'
import sceneBg from '@/assets/images/scene-distribution-room.png'

const router = useRouter()

function enterCabinet() {
  // TODO: 根据后续需求决定跳转目标
  router.push('/experiment')
}
</script>

<style scoped>
.scene-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #020617;
}

/* 梯形热区 — 覆盖进线柜+计量柜+出线柜 */
.cabinet-hotspot {
  position: absolute;
  /* 坐标需根据实际图片调试 */
  top: 18%;
  left: 25%;
  width: 50%;
  height: 55%;
  clip-path: polygon(20% 0%, 80% 0%, 100% 100%, 0% 100%);
  cursor: pointer;
  z-index: 5;
  transition: background 0.25s, box-shadow 0.25s;
}

.cabinet-hotspot:hover {
  background: rgba(0, 210, 255, 0.12);
  box-shadow: inset 0 0 0 2px #00d2ff, 0 0 24px rgba(0, 210, 255, 0.25);
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
  text-shadow: 0 1px 4px rgba(0,0,0,.7);
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  user-select: none;
}
</style>
