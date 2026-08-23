<!-- 视频占位悬浮层：全屏暗色遮罩 + 视频播放，播放完毕 emit ended -->
<template>
  <Teleport to="body">
    <div v-if="visible" class="video-overlay">
      <video
        ref="videoRef"
        :src="src"
        autoplay
        playsinline
        muted
        @ended="$emit('ended')"
      ></video>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  src: { type: String, required: true }
})
const emit = defineEmits(['ended'])
const videoRef = ref(null)

// 重新显示时自动播放（autoplay 可能在部分浏览器被拦截，手动兜底）
watch(
  () => props.visible,
  v => {
    if (v) videoRef.value?.play?.().catch(() => {})
  }
)
</script>

<style scoped>
.video-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #000;
  overflow: hidden;
}

/* 全屏播放：视频铺满整个视口，contain 保证完整画面（超宽/超窄自动留黑边，无裁剪） */
.video-overlay video {
  width: 100vw;
  height: 100vh;
  object-fit: contain;
  background: #000;
}
</style>
