<template>
  <Teleport to="body">
    <div class="experiment-timer" title="实验总耗时">
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
  /* 略放大并整体放大字体，仍保持紧凑（减少留白） */
  width: clamp(120px, 13.5vw, 152px);
  height: clamp(34px, 5.2vh, 42px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 5px solid #ffa500;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  font-size: clamp(17px, 2vw, 22px);
  user-select: none;
}

.timer-text {
  /* 真正圆润的中文字体：汉仪中圆 B5，回退幼圆/苹方 */
  font-family: '汉仪中圆', '汉仪中圆B5', 'HYZhongYuanB5', 'YouYuan', '幼圆', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  font-weight: 600;
  color: #ff5722;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
  line-height: 1;
  padding: 0 1px;
}
</style>
