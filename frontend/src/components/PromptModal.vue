<!-- 通用提示弹窗：灰色透明背景 + 内容插槽 + 覆盖确认按钮 -->
<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" :style="overlayVars" @click.self="$emit('close')">
      <div class="modal-body">
        <!-- 内容区域（确认按钮覆盖在上方） -->
        <div class="modal-content">
          <slot />
          <div
            class="confirm-btn"
            :class="{ green }"
            :style="confirmBtnStyle"
            @click.stop="$emit('close')"
          />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** 确认按钮距底部百分比，默认 12% */
  buttonBottom: { type: String, default: '12%' },
  /** 是否使用绿色确认键（默认黄色） */
  green: { type: Boolean, default: false },
  /** 内容图片最大宽度（父组件传入以调小，默认 80vw；仅注入宽、高 auto，不压缩尺寸） */
  width: { type: String, default: '80vw' },
  /** 内容图片最大高度（默认 none=不约束，若只需高约束则传 height、width 传 'none'） */
  height: { type: String, default: 'none' },
  /** 确认键宽度（相对内容区百分比，默认 15%；只传宽、高 auto 按按钮图比例，不压缩） */
  btnWidth: { type: String, default: '15%' },
  /** 确认键高度（默认 auto=按宽等比；如需强制高约束再传值） */
  btnHeight: { type: String, default: 'auto' }
})
defineEmits(['close'])

// 内容尺寸以 CSS 变量注入 :slotted(img)（保持弹窗由图片贴合、确认键相对图片定位）
const overlayVars = computed(() => ({
  '--pm-img-w': props.width,
  '--pm-img-h': props.height
}))
// 确认键尺寸/位置由父组件控制
const confirmBtnStyle = computed(() => ({
  bottom: props.buttonBottom,
  width: props.btnWidth,
  height: props.btnHeight
}))
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
  max-width: var(--pm-img-w, 80vw);
  max-height: var(--pm-img-h, none);
  height: auto; /* 宽与高只约束一个，另一维 auto 保持比例、不压缩 */
  border-radius: 8px;
}

/* 确认按钮 — 覆盖在内容偏下位置，与场景选择按钮风格一致的悬停动画
   高度由 aspect-ratio 按宽等比（按钮图 ≈ 2.32:1），只传宽、高 auto 不压缩 */
.confirm-btn {
  position: absolute;
  bottom: 12%;
  left: 50%;
  transform: translateX(-50%);
  width: 15%;
  aspect-ratio: 2.32;
  cursor: pointer;
  background-image: var(--img-confirm-btn);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.2s;
  z-index: 10;
}

.confirm-btn:hover {
  background-image: var(--img-confirm-btn-hover);
  transform: translateX(-50%) scale(1.05);
}

.confirm-btn:active {
  transform: translateX(-50%) scale(0.95);
}

/* 绿色确认键变体（默认黄色，`green` 时切换） */
.confirm-btn.green {
  background-image: var(--img-confirm-btn-green);
}

.confirm-btn.green:hover {
  background-image: var(--img-confirm-btn-green-hover);
}
</style>
