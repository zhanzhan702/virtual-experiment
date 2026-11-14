<!--
  居民表箱内部操作画布（Leafer UI）：背景层 + 交互层。

  目前只搭骨架：背景图与具体操作内容待素材提供后补充。
  后续只需由父组件传入 `bg-url`，并在 createCanvas() 的交互层里添加热区/物品。
-->
<template>
  <div class="inner-canvas-wrap">
    <div class="canvas-stage" :class="{ 'no-bg': !bgUrl }">
      <!-- 背景图：素材提供后由父组件传入 bg-url -->
      <img
        v-if="bgUrl"
        ref="bgImgRef"
        :src="bgUrl"
        alt="居民表箱内部"
        class="bg-img"
        draggable="false"
        @load="onBgLoad"
      />
      <!-- Leafer 画布：与背景图同尺寸，覆盖在背景之上 -->
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
      <!-- 素材未就绪时的占位说明（位于画布之上，不拦截鼠标事件） -->
      <div v-if="!bgUrl" class="placeholder">
        <p class="title">居民表箱内部场景</p>
        <p class="hint">Leafer 画布已就绪，背景图与操作内容待补充</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Leafer, Image, Rect } from 'leafer-ui'

const props = defineProps({
  /** 当前步骤序号（步骤 5 起，后续步骤切换时复用同一画布） */
  stepOrder: { type: Number, default: 5 },
  experimentId: { type: String, default: '' },
  stepId: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  /** 背景图地址；为空时显示占位画布 */
  bgUrl: { type: String, default: '' }
})

// 事件留给后续交互内容使用（operation / error / complete）
defineEmits(['operation', 'error', 'complete'])

const bgImgRef = ref(null)
const leaferViewRef = ref(null)
const canvasStyle = ref({})

let leafer = null
let resizeTimer = null

/** 画布尺寸：有背景图时与图片实际渲染尺寸一致，否则按视口取占位尺寸 */
function measureCanvas() {
  if (props.bgUrl && bgImgRef.value) {
    const r = bgImgRef.value.getBoundingClientRect()
    if (r.width > 0 && r.height > 0) {
      return { w: Math.round(r.width), h: Math.round(r.height) }
    }
  }
  return {
    w: Math.round(window.innerWidth * 0.9),
    h: Math.round(window.innerHeight * 0.82)
  }
}

function destroyCanvas() {
  if (leafer) {
    leafer.destroy()
    leafer = null
  }
}

/**
 * 创建 Leafer 画布。
 *
 * 后续开发提示：
 * - 交互元素统一加到 hitLayer（Group），背景元素加到 bgLayer；
 * - 命中判定既可用 Leafer 的 pointer 事件，也可沿用低压外部场景的
 *   getBoundingClientRect 百分比方式（视素材形态选择）。
 */
function createCanvas() {
  const view = leaferViewRef.value
  if (!view) return

  destroyCanvas()
  const { w, h } = measureCanvas()
  canvasStyle.value = { width: `${w}px`, height: `${h}px` }

  leafer = new Leafer({ view, width: w, height: h })

  if (props.bgUrl) {
    leafer.add(new Image({ url: props.bgUrl, x: 0, y: 0, width: w, height: h }))
  } else {
    // 占位底色，保证画布可见且后续可直接叠加素材
    leafer.add(new Rect({ x: 0, y: 0, width: w, height: h, fill: '#ffffff' }))
  }
}

function onBgLoad() {
  createCanvas()
}

function onResize() {
  clearTimeout(resizeTimer)
  // 窗口尺寸变化时重建画布（与高压画布一致，避免元素坐标错位）
  resizeTimer = setTimeout(createCanvas, 150)
}

watch(
  () => props.bgUrl,
  async () => {
    await nextTick()
    // 图片已缓存（complete）时不会再触发 load，需要主动创建一次
    if (!props.bgUrl || bgImgRef.value?.complete) createCanvas()
  }
)

onMounted(async () => {
  await nextTick()
  if (!props.bgUrl || bgImgRef.value?.complete) createCanvas()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  clearTimeout(resizeTimer)
  window.removeEventListener('resize', onResize)
  destroyCanvas()
})

// ── 对外状态读写（供父组件存档/恢复；内容补充后扩展） ──
function getState() {
  return {}
}

function restore() {}

defineExpose({ getState, restore })
</script>

<style scoped>
.inner-canvas-wrap {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.canvas-stage {
  position: relative;
  display: inline-block;
}

/* 没有背景图时由容器撑出占位尺寸，画布按同一尺寸创建 */
.canvas-stage.no-bg {
  width: 90vw;
  height: 82vh;
}

.bg-img {
  max-width: 90vw;
  max-height: 82vh;
  display: block;
  user-select: none;
}

.leafer-view {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6em;
  z-index: 2;
  pointer-events: none;
  color: #123f68;
  text-align: center;
}

.placeholder .title {
  font-size: clamp(20px, 2vw, 32px);
  letter-spacing: 0.1em;
}

.placeholder .hint {
  font-size: clamp(13px, 1.1vw, 18px);
  color: #6b7c8d;
}
</style>
