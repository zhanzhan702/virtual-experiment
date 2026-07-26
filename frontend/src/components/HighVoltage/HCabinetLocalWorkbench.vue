<template>
  <div class="scene-page">
    <div class="scene-toolbar">
      <el-button @click="backToOverview">返回配电室</el-button>
      <div class="scene-title">{{ title }}</div>
      <div class="scene-tip">{{ tip }}</div>
    </div>

    <SceneFrame :src="scene.background" :alt="title" :aspect-ratio="scene.aspectRatio">
      <SceneHotspotOverlay
        :hotspots="scene.hotspots"
        :debug="debugHotspot"
        @select="onHotspotSelect"
      />
      <CabinetSideRails
        :new-items="localStepNewItems"
        :selected-tools="selectedTools"
        :left-panel="scene.leftPanel"
        :right-panel="scene.rightPanel"
      />
    </SceneFrame>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SceneFrame from '@/components/HighVoltage/SceneFrame.vue'
import SceneHotspotOverlay from '@/components/HighVoltage/SceneHotspotOverlay.vue'
import CabinetSideRails from '@/components/HighVoltage/CabinetSideRails.vue'
import {
  cabinetLocalScene,
  localStepNewItems,
  cabinetVisitedKey,
  HOTSPOT_DEBUG_DEFAULT
} from '@/constants/scene-hotspot-config'
import { loadSelectedTools } from '@/utils/selectedTools'

const props = defineProps({
  title: { type: String, required: true },
  tip: { type: String, default: '左侧为本步新物品，右侧为已选工器具（可滚动）' },
  /** 从总览进入时默认对应的柜体 visitKey */
  defaultVisitKey: { type: String, default: '' }
})

const route = useRoute()
const router = useRouter()

const scene = cabinetLocalScene
const selectedTools = ref([])

const experimentId = computed(() => route.query.experimentId || '')
const debugHotspot = computed(
  () => route.query.debugHotspot === '1' || HOTSPOT_DEBUG_DEFAULT
)

onMounted(() => {
  selectedTools.value = loadSelectedTools()
})

function markVisited(visitKey) {
  if (!visitKey) return
  let data = { incoming: false, metering: false }
  try {
    data = { ...data, ...JSON.parse(sessionStorage.getItem(cabinetVisitedKey) || '{}') }
  } catch (_) { /* ignore */ }
  data[visitKey] = true
  sessionStorage.setItem(cabinetVisitedKey, JSON.stringify(data))
}

function backToOverview() {
  router.push({
    path: '/HScene',
    query: {
      experimentId: experimentId.value,
      ...(debugHotspot.value ? { debugHotspot: '1' } : {})
    }
  })
}

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

  const visitKey = zone.visitKey || props.defaultVisitKey
  markVisited(visitKey)
  ElMessage.success(`已确认选择「${zone.label}」`)
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
</style>
