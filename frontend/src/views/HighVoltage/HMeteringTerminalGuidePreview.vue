<template>
  <div class="preview-page">
    <div class="preview-header">
      <h1>HMeteringTerminalGuide 组件预览</h1>
      <p>页面未接入画布</p>
    </div>
    <div class="preview-stage">
      <HMeteringTerminalGuide />
    </div>
  </div>
</template>

<script setup>
import HMeteringTerminalGuide from '@/components/HighVoltage/HMeteringTerminalGuide.vue'
</script>

<style scoped>
.preview-page {
  min-height: 100vh;
  background: #ffffff;
  color: #1a1a1a;
  padding: 24px;
  box-sizing: border-box;
}

.preview-header {
  margin-bottom: 24px;
  text-align: center;
}

.preview-header h1 {
  font-size: 22px;
  margin: 0 0 8px;
  font-weight: 600;
}

.preview-header p {
  font-size: 14px;
  color: #666666;
  margin: 0;
}

.preview-stage {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px 0;
}
</style>
