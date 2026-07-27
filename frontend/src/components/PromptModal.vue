<!-- 通用提示弹窗：灰色透明背景 + 内容插槽 + 覆盖确认按钮 -->
<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-body">
        <!-- 内容区域（确认按钮覆盖在上方） -->
        <div class="modal-content">
          <slot />
          <div class="confirm-btn" :style="{ bottom: buttonBottom }" @click.stop="$emit('close')" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  /** 确认按钮距底部百分比，默认 12% */
  buttonBottom: { type: String, default: '12%' }
})
defineEmits(['close'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
}

.modal-body {
  max-width: 90vw;
  max-height: 90vh;
}

.modal-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content :slotted(img) {
  display: block;
  max-width: 80vw;
  max-height: 70vh;
  border-radius: 8px;
}

/* 确认按钮 — 覆盖在内容偏下位置，与场景选择按钮风格一致的悬停动画 */
.confirm-btn {
  position: absolute;
  bottom: 12%;
  left: 50%;
  transform: translateX(-50%);
  width: 140px;
  height: 50px;
  cursor: pointer;
  background-image: url('@/assets/images/ConfirmButton.png');
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
  z-index: 10;
}

.confirm-btn:hover {
  background-image: url('@/assets/images/ConfirmButtonHover.png');
  transform: translateX(-50%) scale(1.05);
}

.confirm-btn:active {
  transform: translateX(-50%) scale(0.95);
}
</style>
