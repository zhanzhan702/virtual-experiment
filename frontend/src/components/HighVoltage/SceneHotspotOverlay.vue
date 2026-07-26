<template>
  <div class="scene-hotspot-overlay" :style="overlayStyle">
    <button
      v-for="zone in hotspots"
      :key="zone.id"
      type="button"
      class="hotspot"
      :class="{ debug: showDebug }"
      :style="zoneStyle(zone)"
      :aria-label="zone.label"
      :title="zone.label"
      @click="$emit('select', zone)"
    >
      <span v-if="showDebug" class="hotspot-label">{{ zone.label }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  hotspots: {
    type: Array,
    default: () => []
  },
  /** 是否半透明显示热区（标定坐标用） */
  debug: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select'])

const showDebug = computed(() => props.debug)

const overlayStyle = computed(() => ({
  position: 'absolute',
  inset: '0',
  zIndex: 2
}))

function zoneStyle(zone) {
  return {
    top: zone.top,
    left: zone.left,
    width: zone.width,
    height: zone.height
  }
}
</script>

<style scoped>
.scene-hotspot-overlay {
  pointer-events: none;
}

.hotspot {
  position: absolute;
  margin: 0;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  pointer-events: auto;
  box-sizing: border-box;
}

.hotspot:hover {
  background: rgba(64, 158, 255, 0.18);
  box-shadow: inset 0 0 0 2px rgba(64, 158, 255, 0.55);
}

.hotspot:focus-visible {
  outline: 2px solid rgba(64, 158, 255, 0.9);
  outline-offset: 2px;
}

.hotspot.debug {
  background: rgba(64, 158, 255, 0.28);
  border: 1px dashed rgba(64, 158, 255, 0.95);
}

.hotspot.debug:hover {
  background: rgba(64, 158, 255, 0.42);
}

.hotspot-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.55);
  user-select: none;
}
</style>
