<template>
  <Teleport to="body">
    <div class="experiment-timer" title="实验总耗时">
      <span class="timer-icon">⏱</span>
      <span class="timer-text">{{ formatted }}</span>
    </div>
  </Teleport>
</template>

<script setup>
import { toRef } from 'vue'
import { useExperimentTimer } from '@/composables/useExperimentTimer'

const props = defineProps({
  experimentId: { type: String, default: '' },
  // 当前步骤实时秒数（来自各步骤页已有的 stats.duration_seconds）
  currentStepSeconds: { type: Number, default: 0 }
})

const experimentIdRef = toRef(props, 'experimentId')
const currentStepSecondsRef = toRef(props, 'currentStepSeconds')

const { formatted } = useExperimentTimer(experimentIdRef, currentStepSecondsRef)
</script>

<style scoped>
/* 右上角固定定位，宽高仿照 save-bar-fixed / work-task-btn */
.experiment-timer {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: clamp(120px, 14vw, 160px);
  height: clamp(32px, 5vh, 40px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 2px solid #3395e0;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  font-size: clamp(14px, 1.6vw, 18px);
  font-weight: 600;
  color: #303133;
  user-select: none;
}

.timer-icon {
  font-size: clamp(16px, 1.8vw, 20px);
  line-height: 1;
}

.timer-text {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
  line-height: 1;
}
</style>
