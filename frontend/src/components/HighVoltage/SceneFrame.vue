<template>
  <div class="scene-stage">
    <div class="scene-frame" :style="{ aspectRatio }">
      <img class="scene-bg" :src="src" :alt="alt" draggable="false" />
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  aspectRatio: { type: String, default: '16 / 9' }
})
</script>

<style scoped>
.scene-stage {
  position: relative;
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #020617;
}

.scene-frame {
  position: relative;
  width: 100%;
  max-height: 100%;
}

.scene-bg {
  display: block;
  width: 100%;
  height: auto;
  user-select: none;
  pointer-events: none;
}
</style>
