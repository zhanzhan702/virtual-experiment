<template>
  <div class="side-rails">
    <!-- 左侧：本步新物品 -->
    <div class="rail left-rail" :style="panelBox(leftPanel)">
      <div class="rail-scroll">
        <div
          v-for="item in newItems"
          :key="item.id"
          class="rail-slot"
          :class="{ empty: item.empty }"
          :title="item.name"
        >
          <span class="rail-ico">{{ item.icon }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧：之前已选器械（可上下滚动） -->
    <div class="rail right-rail" :style="panelBox(rightPanel)">
      <div class="rail-scroll">
        <div v-if="selectedTools.length === 0" class="rail-empty">暂无</div>
        <div
          v-for="tool in selectedTools"
          :key="`${tool.categoryKey}-${tool.id}`"
          class="rail-slot"
          :title="tool.name"
        >
          <span class="rail-ico">{{ tool.icon }}</span>
        </div>
      </div>
      <div v-if="selectedTools.length > 4" class="rail-hint">▼</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  newItems: { type: Array, default: () => [] },
  selectedTools: { type: Array, default: () => [] },
  leftPanel: { type: Object, required: true },
  rightPanel: { type: Object, required: true }
})

function panelBox(panel) {
  return {
    top: panel.top,
    left: panel.left,
    width: panel.width,
    height: panel.height
  }
}
</script>

<style scoped>
.side-rails {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.rail {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
  padding: 6% 8%;
  border-radius: 10px;
  background: rgba(12, 74, 78, 0.96);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  pointer-events: auto;
  overflow: hidden;
}

.rail-scroll {
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.35) transparent;
}

.rail-scroll::-webkit-scrollbar {
  width: 4px;
}

.rail-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.35);
  border-radius: 4px;
}

.rail-slot {
  width: 86%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #2a7a7e, #0b3d40 70%);
  border: 2px solid rgba(180, 230, 235, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}

.rail-slot.empty {
  opacity: 0.45;
  border-style: dashed;
}

.rail-ico {
  font-size: clamp(14px, 2.2vw, 22px);
  line-height: 1;
  user-select: none;
}

.rail-empty {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  text-align: center;
  padding-top: 12px;
}

.rail-hint {
  flex-shrink: 0;
  color: #fff;
  font-size: 14px;
  line-height: 1;
  margin-top: 4px;
  opacity: 0.85;
  animation: bounce 1.2s ease-in-out infinite;
}

@keyframes bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(3px);
  }
}
</style>
