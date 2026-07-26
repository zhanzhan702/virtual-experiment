<template>
  <div class="scene-page">
    <div class="scene-toolbar">
      <div class="scene-title">{{ sceneOverview.title }}</div>
      <div class="scene-tip">{{ sceneOverview.tip }}</div>
      <div class="scene-actions">
        <el-tag v-if="visited.incoming" type="success" effect="plain">进线柜已进入</el-tag>
        <el-tag v-else type="info" effect="plain">进线柜未进入</el-tag>
        <el-tag v-if="visited.metering" type="success" effect="plain">计量柜已进入</el-tag>
        <el-tag v-else type="info" effect="plain">计量柜未进入</el-tag>
        <el-button type="primary" :disabled="!bothVisited" @click="goNextExperimentStep">
          继续后续步骤
        </el-button>
      </div>
    </div>

    <SceneFrame
      :src="sceneOverview.background"
      alt="配电室总览"
      :aspect-ratio="sceneOverview.aspectRatio"
    >
      <SceneHotspotOverlay
        :hotspots="sceneOverview.hotspots"
        :debug="debugHotspot"
        @select="onHotspotSelect"
      />
    </SceneFrame>
  </div>
</template>

<script setup>
import { computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SceneFrame from '@/components/HighVoltage/SceneFrame.vue'
import SceneHotspotOverlay from '@/components/HighVoltage/SceneHotspotOverlay.vue'
import {
  sceneOverview,
  cabinetVisitedKey,
  HOTSPOT_DEBUG_DEFAULT
} from '@/constants/scene-hotspot-config'

const route = useRoute()
const router = useRouter()

const experimentId = computed(() => route.query.experimentId || '')
const debugHotspot = computed(
  () => route.query.debugHotspot === '1' || HOTSPOT_DEBUG_DEFAULT
)

const visited = reactive({
  incoming: false,
  metering: false
})

const bothVisited = computed(() => visited.incoming && visited.metering)

function loadVisited() {
  try {
    const raw = sessionStorage.getItem(cabinetVisitedKey)
    if (!raw) return
    const data = JSON.parse(raw)
    visited.incoming = !!data.incoming
    visited.metering = !!data.metering
  } catch (_) { /* ignore */ }
}

onMounted(loadVisited)

async function onHotspotSelect(zone) {
  if (!zone) return

  if (!zone.correct) {
    await ElMessageBox.alert('选择错误', '提示', {
      confirmButtonText: '确定',
      type: 'warning',
      center: true
    }).catch(() => {})
    return
  }

  if (!zone.targetRoute) return
  router.push({
    path: zone.targetRoute,
    query: {
      experimentId: experimentId.value,
      ...(debugHotspot.value ? { debugHotspot: '1' } : {})
    }
  })
}

function goNextExperimentStep() {
  if (!bothVisited.value) {
    ElMessage.warning('请先进入进线柜与计量柜')
    return
  }
  ElMessage.info('进线柜与计量柜已完成进入，后续步骤（设置围栏等）待接入')
}
</script>

<style scoped>
.scene-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  overflow: hidden;
  box-sizing: border-box;
}

.scene-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  padding: 10px 16px;
  background: rgba(15, 23, 42, 0.92);
  color: #e2e8f0;
  z-index: 3;
}

.scene-title {
  font-size: 18px;
  font-weight: 600;
}

.scene-tip {
  flex: 1;
  min-width: 200px;
  font-size: 13px;
  color: #94a3b8;
}

.scene-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
</style>
