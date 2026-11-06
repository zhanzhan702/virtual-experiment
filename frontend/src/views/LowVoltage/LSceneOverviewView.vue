<template>
  <main class="scene-overview">
    <section class="panel">
      <h1>低压场景总览</h1>
      <p class="hint">该页面正在建设中，后续将在这里展示低压场景与操作步骤。</p>
      <p class="meta">实验 ID：{{ experimentId || '—' }}</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </section>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const experimentId = computed(() =>
  typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
)
</script>

<style scoped>
.scene-overview {
  width: 100vw;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--img-hwt-bg) center / cover no-repeat;
}

.panel {
  background: #fffdf5;
  border: 6px solid #008d85;
  border-radius: 24px;
  padding: 3em 4em;
  text-align: center;
  color: #123f68;
  max-width: 80vw;
}

.panel h1 {
  margin-bottom: 0.6em;
  font-size: 1.6em;
}

.hint {
  margin-bottom: 1em;
  line-height: 1.8;
}

.meta {
  margin-bottom: 1.5em;
  color: #5b7c96;
  font-size: 0.9em;
}
</style>
