# 低压居民表箱内部场景（步骤 5~7）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/LRB` 页面叠加 Leafer 画布，实现居民表箱内部的断电挂牌、挂表接线、上电封盖三个阶段，完成后跳转杆上变占位页 `/LPT`。

**Architecture:** 单画布 + 阶段驱动。新建 `LResidentBoxCabinetCanvas.vue` 持有全部 Leafer 元素，按 URL `stepOrder`（5/6/7）初始化图层与交互；外部场景 `LResidentBoxScene` 保持渲染但禁用，左右工具栏位于画布之上。阶段完成时父页面提交步骤并切换 `stepId/stepOrder`，画布不销毁只重建交互层。

**Tech Stack:** Vue 3 `<script setup>`、Leafer UI 2.x、Element Plus、Vue Router 4、Spring Boot + MyBatis-Plus（仅数据库脚本）。

参考设计：`docs/superpowers/specs/2026-11-21-low-resident-box-inner-design.md`

## Global Constraints

- 所有新增 Vue 组件使用 `<script setup>`，模板 → script → style 顺序
- 使用 `@/` 别名导入，禁止相对路径
- 中文注释；坐标常量集中在文件顶部 `★★★ ... ★★★` 区域
- 前端格式化：`cd frontend && npm run format`；提交前确保 `npm run build` 通过
- 禁止 `git add -A`，逐文件暂存；提交信息 `<type>(<scope>): 中文描述`
- 每个任务完成后在工作区测试通过再提交
- 1 号空开 off 暂用 `breaker1On` 图，切换状态但视觉不变
- 错序统一处理：`ElMessage.warning` + `emit('error')`，不回退
- 后端无需改动 Java 代码，只新增 SQL 脚本

---

### Task 1: 素材迁移、重命名与 images.js 注册

**Files:**
- Move/Rename: `frontend/src/assets/images/cabinet/room/居民表箱/` → `frontend/src/assets/images/cabinet/room/ResidentBox/`
- Rename: `frontend/src/assets/images/cabinet/toolbar/18.png` → `SinglePhaseMeter.png`
- Modify: `frontend/src/constants/images.js`

**Interfaces:**
- Produces: `Images.residentBoxInnerBg`、`Images.breaker1On`、`Images.breaker2On`、`Images.breaker2Off`、`Images.breaker3On`、`Images.breaker3Off`、`Images.singlePhaseMeterOpen`、`Images.barSinglePhaseMeter`
- Produces: `Images.residentBoxBg` 路径更新为 `cabinet/room/ResidentBox/ResidentBoxBackground.jpg`

- [ ] **Step 1: 迁移目录并重命名文件**

```bash
cd frontend/src/assets/images/cabinet/room
mv 居民表箱 ResidentBox
cd ResidentBox
mv 7.jpg ResidentBoxBackground.jpg
mv 8.png ResidentBoxInnerBackground.png
mv 1on.png Breaker1On.png
mv 2On.png Breaker2On.png
mv 2Off.png Breaker2Off.png
mv 3on.png Breaker3On.png
mv 3off.png Breaker3Off.png
mv 19.png SinglePhaseMeterOpen.png
cd ../../toolbar
mv 18.png SinglePhaseMeter.png
```

`11.png`、`12.png` 随目录移入 `ResidentBox/`，本期不接入，不改名。

- [ ] **Step 2: 更新 images.js 导入**

把 `residentBoxBg` 与 `residentBoxFence` 所在段（约 170 行）改为：

```js
// 低压居民表箱：外部背景 + 场景整体围栏 + 内部画布素材
import residentBoxBg from '@/assets/images/cabinet/room/ResidentBox/ResidentBoxBackground.jpg'
import residentBoxFence from '@/assets/images/cabinet/ResidentBoxFence.png'
// 表箱内部（Leafer 画布）
import residentBoxInnerBg from '@/assets/images/cabinet/room/ResidentBox/ResidentBoxInnerBackground.png'
import breaker1On from '@/assets/images/cabinet/room/ResidentBox/Breaker1On.png'
import breaker2On from '@/assets/images/cabinet/room/ResidentBox/Breaker2On.png'
import breaker2Off from '@/assets/images/cabinet/room/ResidentBox/Breaker2Off.png'
import breaker3On from '@/assets/images/cabinet/room/ResidentBox/Breaker3On.png'
import breaker3Off from '@/assets/images/cabinet/room/ResidentBox/Breaker3Off.png'
import singlePhaseMeterOpen from '@/assets/images/cabinet/room/ResidentBox/SinglePhaseMeterOpen.png'
```

工具栏段追加（放在 `barThreePhaseThreeWireMeter` 附近）：

```js
import barSinglePhaseMeter from '@/assets/images/cabinet/toolbar/SinglePhaseMeter.png'
```

- [ ] **Step 3: 更新 images.js 导出映射**

在 `residentBoxBg,` 之后追加：

```js
  residentBoxBg,
  residentBoxFence,
  residentBoxInnerBg,
  breaker1On,
  breaker2On,
  breaker2Off,
  breaker3On,
  breaker3Off,
  singlePhaseMeterOpen,
```

在 `barThreePhaseThreeWireMeter,` 之后追加：

```js
  barSinglePhaseMeter,
```

- [ ] **Step 4: 验证构建**

Run: `cd frontend && npm run build`
Expected: `✓ built in ...`，无 `Failed to resolve import`

- [ ] **Step 5: 提交**

```bash
git add frontend/src/assets/images/cabinet/room/ResidentBox frontend/src/assets/images/cabinet/toolbar/SinglePhaseMeter.png frontend/src/constants/images.js
git rm -q frontend/src/assets/images/cabinet/7.jpg frontend/src/assets/images/cabinet/toolbar/18.png frontend/src/assets/images/cabinet/room/居民表箱/7.jpg frontend/src/assets/images/cabinet/room/居民表箱/8.png frontend/src/assets/images/cabinet/room/居民表箱/1on.png frontend/src/assets/images/cabinet/room/居民表箱/2On.png frontend/src/assets/images/cabinet/room/居民表箱/2Off.png frontend/src/assets/images/cabinet/room/居民表箱/3on.png frontend/src/assets/images/cabinet/room/居民表箱/3off.png frontend/src/assets/images/cabinet/room/居民表箱/19.png
git commit -m "chore(frontend): 规范居民表箱素材目录与文件命名"
```

> 若上述旧文件尚未被 git 跟踪，`git rm` 会报错；改用 `rm -f` 删除，`git add` 新目录即可。

---

### Task 2: 数据库脚本（步骤 5 改名 + 新增 6/7）

**Files:**
- Create: `backend/docs/sql/11_low_resident_box_inner_steps.sql`

**Interfaces:**
- Produces: 低压模板步骤 5 `POWER_OFF_SIGN`、6 `MOUNT_METER_WIRING`、7 `POWER_ON_SEAL`

- [ ] **Step 1: 编写幂等脚本**

```sql
-- V11 低压居民表箱内部：步骤 5 改为「断电挂牌」，新增步骤 6「挂表接线」、7「上电封盖」
--
-- 幂等：步骤 5 重复执行只更新字段；6/7 已存在时不再插入。
-- 注意（Windows）：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 11_low_resident_box_inner_steps.sql

UPDATE experiment_steps
SET step_code = 'POWER_OFF_SIGN',
    step_name = '断电挂牌',
    required_seconds = 120,
    score = 15.00
WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
  AND step_order = 5;

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       6, 'MOUNT_METER_WIRING', '挂表接线', 300, 30.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 6
);

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       7, 'POWER_ON_SEAL', '上电封盖', 180, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 7
);
```

- [ ] **Step 2: 执行并校验**

```bash
cd backend
MYSQL_PWD='fuzhanpeng602' mysql --default-character-set=utf8mb4 -u root virtual_experiment < docs/sql/11_low_resident_box_inner_steps.sql
MYSQL_PWD='fuzhanpeng602' mysql --default-character-set=utf8mb4 -u root virtual_experiment -e "SELECT step_order, step_code, step_name, required_seconds, score FROM experiment_steps WHERE template_id=(SELECT id FROM experiment_templates WHERE code='LV_TRAIN_V1') ORDER BY step_order;"
```

Expected: 输出 7 行，5/6/7 分别为 `POWER_OFF_SIGN 断电挂牌`、`MOUNT_METER_WIRING 挂表接线`、`POWER_ON_SEAL 上电封盖`。

- [ ] **Step 3: 提交**

```bash
git add backend/docs/sql/11_low_resident_box_inner_steps.sql
git commit -m "feat(experiment): 新增居民表箱内部三个实验步骤"
```

---

### Task 3: 路由与页面衔接（删除 /LBI，新增 /LPT）

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/ExperimentView.vue`
- Create: `frontend/src/views/LowVoltage/LPoleTransformerView.vue`
- Delete: `frontend/src/views/LowVoltage/LResidentBoxInnerView.vue`
- Delete: `frontend/src/components/LowVoltage/LResidentBoxInnerCanvas.vue`

**Interfaces:**
- Produces: 路由 `/LPT`（杆上变占位页）
- Produces: 低压步骤 5/6/7 的继续实验落点均为 `/LRB`

- [ ] **Step 1: 新建占位页**

```vue
<!-- 杆上变场景（步骤 8 起）：集中器安装，待后续开发 -->
<template>
  <main class="pole-transformer-view">
    <p class="title">杆上变场景 · 集中器安装</p>
    <p class="hint">该场景将在下一阶段开发</p>
    <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.pole-transformer-view {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.2em;
  background: #f5f7f9;
  color: #123f68;
}

.title {
  font-size: clamp(22px, 2.4vw, 36px);
  letter-spacing: 0.08em;
}

.hint {
  font-size: clamp(14px, 1.2vw, 20px);
  color: #6b7c8d;
}
</style>
```

- [ ] **Step 2: 修改 router**

删除这一行：

```js
  // LBI = Low Box Inner：居民表箱内部操作（Leafer 画布）
  { path: '/LBI', component: () => import('@/views/LowVoltage/LResidentBoxInnerView.vue') }
```

在 `/LRB` 之后追加：

```js
  // 低压居民表箱内部（步骤 5~7）在 /LRB 内叠加画布；LPT = 杆上变场景（步骤 8 起，占位）
  { path: '/LPT', component: () => import('@/views/LowVoltage/LPoleTransformerView.vue') }
```

- [ ] **Step 3: 修改 ExperimentView 低压映射**

```js
            : {
                1: '/LWT',
                2: '/LTS',
                3: '/LRB',
                4: '/LRB',
                5: '/LRB',
                6: '/LRB',
                7: '/LRB'
              }
```

- [ ] **Step 4: 删除旧页面文件**

```bash
git rm -q frontend/src/views/LowVoltage/LResidentBoxInnerView.vue
git rm -q frontend/src/components/LowVoltage/LResidentBoxInnerCanvas.vue
```

- [ ] **Step 5: 验证构建与路由**

Run: `cd frontend && npm run build`
Expected: 构建通过；`grep -rn "LResidentBoxInner" src` 无结果。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/router/index.js frontend/src/views/ExperimentView.vue frontend/src/views/LowVoltage/LPoleTransformerView.vue
git commit -m "feat(experiment): 低压内部场景并入 /LRB 并新增杆上变占位页"
```

---

### Task 4: 工具栏扩展与阶段 5+ 点击转发

**Files:**
- Modify: `frontend/src/components/LowVoltage/LResidentBoxScene.vue`
- Modify: `frontend/src/views/LowVoltage/LResidentBoxView.vue`

**Interfaces:**
- Produces: `@left-tool-select="idx => ..."`、`@right-tool-select="idx => ..."` 事件（stage >= 5 时触发）
- Produces: 右栏第一项为「单相智能电表」，`VT_IDX` 仍按名称查找
- Consumes: `Images.barSinglePhaseMeter`

- [ ] **Step 1: 右栏补充单相智能电表**

在 `LResidentBoxScene.vue` 的 `RIGHT_TOOLS` 定义前追加：

```js
const RIGHT_TOOLS = [
  ...pickCorrectItems('terminal'),
  ...pickCorrectItems('tool'),
  ...pickCorrectItems('wire')
]
```

改为：

```js
// 单相智能电表用于表箱内部（步骤 5+），主场景也常驻显示
const RIGHT_TOOLS = [
  { name: '单相智能电表', img: Images.barSinglePhaseMeter },
  ...pickCorrectItems('terminal'),
  ...pickCorrectItems('tool'),
  ...pickCorrectItems('wire')
]
```

- [ ] **Step 2: stage >= 5 时左栏点击只转发、不进入放置**

`selectTool(idx)` 开头追加：

```js
function selectTool(idx) {
  if (props.disabled) return
  // 步骤 5+：外部放置逻辑停用，左栏选择转发给父组件（画布用于悬挂标识牌）
  if (props.stage >= 5) {
    emit('left-tool-select', idx)
    return
  }
  emit('operation')
  ...
```

`defineEmits` 改为：

```js
const emit = defineEmits(['complete', 'operation', 'error', 'left-tool-select', 'right-tool-select'])
```

- [ ] **Step 3: stage >= 5 时右栏点击转发并高亮**

在 `onRightToolClick(idx)` 开头追加：

```js
function onRightToolClick(idx) {
  if (props.disabled) return
  if (props.stage >= 5) {
    activeRightIdx.value = idx
    emit('right-tool-select', idx, RIGHT_TOOLS[idx]?.name)
    return
  }
  ...
```

新增状态：

```js
const activeRightIdx = ref(null)
```

`ToolBarRight` 增加高亮数组：

```vue
    <ToolBarRight
      :items="RIGHT_TOOLS"
      :vt-idx="VT_IDX"
      :vt-active="vtActive"
      :vt-done="vtDone"
      :active-idxs="activeRightIdx === null ? [] : [activeRightIdx]"
      @click="onRightToolClick"
    />
```

- [ ] **Step 4: 父页面接收并转换成画布工具语义**

`LResidentBoxView.vue` 模板中的场景组件加监听：

```vue
    <LResidentBoxScene
      ref="sceneRef"
      :stage="stage"
      :disabled="!canInteract || stage >= 5"
      @complete="onStageComplete"
      @operation="operationCount++"
      @error="errorCount++"
      @left-tool-select="canvasLeftTool = $event"
      @right-tool-select="onCanvasRightTool"
    />
```

脚本新增：

```js
// 表箱内部画布当前选中的工具语义：sign / meter / seal / null
const canvasLeftTool = ref(null)
const canvasRightTool = ref(null)

function onCanvasRightTool(idx, name) {
  operationCount.value++
  if (name === '单相智能电表') canvasRightTool.value = 'meter'
  else if (name === '铅封') canvasRightTool.value = 'seal'
  else {
    canvasRightTool.value = null
    ElMessage.info('该工具将在后续步骤中使用')
  }
}
```

> 左栏索引 3 = 「禁止合闸」，画布内部判断 `props.leftTool === 3`。

- [ ] **Step 5: 验证**

Run: `cd frontend && npm run build`
Expected: 构建通过。浏览器进入 `/LRB` 步骤 3/4：右栏出现「单相智能电表」，其余交互不变。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxScene.vue frontend/src/views/LowVoltage/LResidentBoxView.vue
git commit -m "feat(scene): 低压工具栏补充单相智能电表并转发内部操作"
```

---

### Task 5: 画布骨架 + 阶段 5 断电

**Files:**
- Create: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`
- Modify: `frontend/src/views/LowVoltage/LResidentBoxView.vue`

**Interfaces:**
- Consumes: `Images.residentBoxInnerBg`、`Images.breaker1On`、`Images.breaker2On`、`Images.breaker2Off`、`Images.breaker3On`、`Images.breaker3Off`
- Props: `stage: Number`（5/6/7）、`disabled: Boolean`、`leftTool: Number|null`、`rightTool: String|null`
- Emits: `operation`、`error`、`complete`
- Produces: `getState()` / `restore(state)`（本任务先返回空骨架）
- Produces: `defineExpose({ getState, restore, markBoxDone })`

- [ ] **Step 1: 新建画布组件（骨架 + 空开）**

```vue
<!-- 居民表箱内部 Leafer 画布：步骤 5 断电挂牌 / 6 挂表接线 / 7 上电封盖 -->
<template>
  <div class="canvas-wrap">
    <div class="canvas-stage">
      <img
        ref="bgImgRef"
        :src="Images.residentBoxInnerBg"
        alt="居民表箱内部"
        class="bg-img"
        draggable="false"
        @load="onBgLoad"
      />
      <div ref="leaferViewRef" class="leafer-view" :style="canvasStyle" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { Leafer, Group, Image, Path, Rect, PointerEvent } from 'leafer-ui'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'

// ★★★ 坐标常量（相对画布百分比，按需逐个调整） ★★★
// 空开：1/2 各 4 个横排，3 号 1 个，从下到上
const BREAKER_DEFS = [
  { id: '1-1', group: 1, x: 4, y: 70, w: 8 },
  { id: '1-2', group: 1, x: 13.5, y: 70, w: 8 },
  { id: '1-3', group: 1, x: 23, y: 70, w: 8 },
  { id: '1-4', group: 1, x: 32.5, y: 70, w: 8 },
  { id: '2-1', group: 2, x: 4, y: 48, w: 5.5 },
  { id: '2-2', group: 2, x: 10.5, y: 48, w: 5.5 },
  { id: '2-3', group: 2, x: 17, y: 48, w: 5.5 },
  { id: '2-4', group: 2, x: 23.5, y: 48, w: 5.5 },
  { id: '3', group: 3, x: 5, y: 22, w: 13 }
]
// 空开图片宽高比（宽/高），用于按宽度换算高度
const BREAKER_RATIO = { 1: 674 / 1333, 2: 367 / 1215, 3: 789 / 1239 }

const props = defineProps({
  stage: { type: Number, required: true },
  disabled: { type: Boolean, default: false },
  leftTool: { type: Number, default: null },
  rightTool: { type: String, default: null }
})
const emit = defineEmits(['operation', 'error', 'complete'])

const bgImgRef = ref(null)
const leaferViewRef = ref(null)
const canvasStyle = ref({})

let leafer = null
let bgLayer = null
let breakerLayer = null
let hitLayer = null
let resizeTimer = null

// 空开状态：id -> 'on' | 'off'
const breakerStates = reactive({})
// 阶段 5 子状态：0/1/2 = 正在断 1/2/3 号排，3 = 待挂牌，4 = 已挂牌
const stage5Step = ref(0)

function breakerImg(def, state) {
  if (def.group === 1) return Images.breaker1On
  if (def.group === 2) return state === 'off' ? Images.breaker2Off : Images.breaker2On
  return state === 'off' ? Images.breaker3Off : Images.breaker3On
}

function resetBreakers(initState) {
  BREAKER_DEFS.forEach(def => {
    breakerStates[def.id] = initState
  })
}

function createCanvas() {
  const img = bgImgRef.value
  if (!img) return
  const r = img.getBoundingClientRect()
  const w = Math.round(r.width)
  const h = Math.round(r.height)
  if (w <= 0 || h <= 0) return

  canvasStyle.value = { width: `${w}px`, height: `${h}px` }
  if (leafer) leafer.destroy()

  leafer = new Leafer({ view: leaferViewRef.value, width: w, height: h })
  bgLayer = new Group()
  breakerLayer = new Group()
  hitLayer = new Group()
  leafer.add(bgLayer)
  leafer.add(breakerLayer)
  leafer.add(hitLayer)
  bgLayer.add(new Image({ url: Images.residentBoxInnerBg, x: 0, y: 0, width: w, height: h }))

  buildBreakers(w, h)
  applyStageInit()
}

function buildBreakers(w, h) {
  breakerLayer.removeAll()
  BREAKER_DEFS.forEach(def => {
    const width = (w * def.w) / 100
    const node = new Image({
      url: breakerImg(def, breakerStates[def.id]),
      x: (w * def.x) / 100,
      y: (h * def.y) / 100,
      width,
      height: width / BREAKER_RATIO[def.group]
    })
    node.on(PointerEvent.CLICK, () => onBreakerClick(def.id))
    breakerLayer.add(node)
    def.node = node
  })
}

function refreshBreaker(def) {
  if (def.node) def.node.url = breakerImg(def, breakerStates[def.id])
}

/** 按阶段初始化空开状态：阶段 5 全 on；阶段 7 全 off（由阶段 5 断电后延续） */
function applyStageInit() {
  if (props.stage === 5 && stage5Step.value === 0) resetBreakers('on')
  if (props.stage === 7) resetBreakers('off')
  BREAKER_DEFS.forEach(refreshBreaker)
}

function onBreakerClick(id) {
  if (props.disabled || props.stage !== 5) return
  const def = BREAKER_DEFS.find(d => d.id === id)
  if (!def) return

  if (stage5Step.value < 3) {
    if (def.group !== stage5Step.value + 1) {
      ElMessage.warning('请按 1 → 2 → 3 的顺序断电')
      emit('error')
      return
    }
    if (breakerStates[id] === 'off') return
    breakerStates[id] = 'off'
    refreshBreaker(def)
    emit('operation')
    const groupDone = BREAKER_DEFS.filter(d => d.group === def.group).every(
      d => breakerStates[d.id] === 'off'
    )
    if (groupDone) {
      stage5Step.value++
      if (stage5Step.value === 3) ElMessage.info('断电完成，请悬挂「禁止合闸」标识牌')
    }
    return
  }

  // stage5Step === 3：挂牌逻辑（下一个任务实现）
}

function onBgLoad() {
  createCanvas()
}

function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (leafer) createCanvas()
  }, 200)
}

onMounted(() => {
  if (bgImgRef.value?.complete) createCanvas()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  clearTimeout(resizeTimer)
  window.removeEventListener('resize', onResize)
  if (leafer) {
    leafer.destroy()
    leafer = null
  }
})

watch(
  () => props.stage,
  () => {
    if (leafer) applyStageInit()
  }
)

function getState() {
  return { stage: props.stage, breakers: { ...breakerStates } }
}

function restore(state) {
  if (!state || !state.breakers) return
  Object.keys(state.breakers).forEach(id => {
    if (breakerStates[id] !== undefined) breakerStates[id] = state.breakers[id]
  })
  BREAKER_DEFS.forEach(refreshBreaker)
}

defineExpose({ getState, restore })
</script>

<style scoped>
.canvas-wrap {
  position: fixed;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-stage {
  position: relative;
  display: inline-block;
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
</style>
```

- [ ] **Step 2: 父页面挂载画布**

`LResidentBoxView.vue` 模板追加（放在场景组件之后）：

```vue
    <!-- 步骤 5~7：居民表箱内部 Leafer 画布叠加在外部场景之上 -->
    <LResidentBoxCabinetCanvas
      v-if="stage >= 5"
      ref="canvasRef"
      :stage="stage"
      :disabled="!canInteract"
      :left-tool="canvasLeftTool"
      :right-tool="canvasRightTool"
      @operation="operationCount++"
      @error="errorCount++"
      @complete="onStageComplete"
    />
```

脚本导入与 ref：

```js
import LResidentBoxCabinetCanvas from '@/components/LowVoltage/LResidentBoxCabinetCanvas.vue'

const canvasRef = ref(null)
```

- [ ] **Step 3: 验证**

Run: `cd frontend && npm run build`
Expected: 构建通过。

浏览器：API 提交步骤 1~4 后进入 `/LRB` 步骤 5，画布显示 8.png 与 9 个空开；按 1→2→3 点空开可切换，乱序点击出现提示并计错。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue frontend/src/views/LowVoltage/LResidentBoxView.vue
git commit -m "feat(scene): 居民表箱内部画布与断电交互"
```

---

### Task 6: 阶段 5 挂牌与提交推进

**Files:**
- Modify: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`
- Modify: `frontend/src/views/LowVoltage/LResidentBoxView.vue`

**Interfaces:**
- Consumes: `leftTool === 3`（左栏「禁止合闸」）
- Produces: 阶段 5 完成时 `emit('complete')`，父页面提交步骤并切到步骤 6

- [ ] **Step 1: 画布内实现挂牌**

在 `LResidentBoxCabinetCanvas.vue` 坐标区追加：

```js
// 标识牌：悬挂在 3 号空开上（相对画布 %）
const SIGN_STYLE = { x: 4, y: 18, w: 9 }
```

新增状态与渲染：

```js
const signPlaced = ref(false)
let signNode = null

function renderSign() {
  if (signNode) {
    signNode.remove()
    signNode = null
  }
  if (!signPlaced.value || !breakerLayer || !leafer) return
  const width = (leafer.width * SIGN_STYLE.w) / 100
  signNode = new Image({
    url: Images.barSafetyNotice,
    x: (leafer.width * SIGN_STYLE.x) / 100,
    y: (leafer.height * SIGN_STYLE.y) / 100,
    width,
    height: width
  })
  breakerLayer.add(signNode)
}
```

`createCanvas()` 里 `buildBreakers(w, h)` 之后追加 `renderSign(w, h)`。

把 `onBreakerClick` 中的挂牌分支替换为：

```js
  // stage5Step === 3：必须先用左栏选中「禁止合闸」，再点 3 号空开
  if (props.leftTool !== 3) {
    ElMessage.warning('请先从左侧选择「禁止合闸」标识牌')
    emit('error')
    return
  }
  if (id !== '3') {
    ElMessage.warning('请将标识牌悬挂在 3 号空开上')
    emit('error')
    return
  }
  if (signPlaced.value) return
  signPlaced.value = true
  renderSign()
  emit('operation')
  stage5Step.value = 4
  emit('complete')
```

- [ ] **Step 2: 父页面提交并推进阶段**

`LResidentBoxView.vue` 的阶段推进逻辑需要区分「步骤 3」与「步骤 5」：

```js
/** 本阶段完成：提交当前步骤，推进到下一步 */
async function onStageComplete() {
  if (!canInteract.value) return
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  submitting.value = true
  try {
    await submitStep(buildPayload(1))

    // 三步验电完成：先弹提示窗，关窗后再进入表箱内部（步骤 5）
    if (stage.value === 4) {
      electrifyDone.value = true
      showElectrifyNotice.value = true
      ElMessage.success('验电操作完成！')
      return
    }

    const next = steps.value.find(s => s.stepOrder === stage.value + 1)
    if (!next) {
      ElMessage.warning('未找到下一步骤，请确认数据库已执行第 5~7 步脚本')
      return
    }
    // 步骤 3 → 4 仍在本页；步骤 5/6 → 6/7 由画布驱动
    await router.replace({
      query: { experimentId: experimentId.value, stepId: next.stepId, stepOrder: next.stepOrder }
    })
    await handleRestore()
    timer = setInterval(() => durationSeconds.value++, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}
```

`buildPayload` 中的 `sceneRef?.getState()` 改为按阶段取状态：

```js
function currentState() {
  if (stage.value >= 5) return canvasRef.value?.getState() || {}
  return sceneRef.value?.getState() || {}
}

function buildPayload(status) {
  return {
    experimentId: experimentId.value,
    stepId: stepId.value,
    status,
    durationSeconds: durationSeconds.value,
    operationCount: operationCount.value,
    errorCount: errorCount.value,
    ...(status === 1 ? { score: Math.max(0, 100 - errorCount.value * 10) } : {}),
    resultData: JSON.stringify({ stage: stage.value, ...currentState() })
  }
}
```

`handleRestore` 的分发：

```js
    const draft = await getStepDraft(experimentId.value, stepId.value)
    if (draft && Object.keys(draft).length > 0) {
      if (stage.value >= 5) canvasRef.value?.restore(draft)
      else sceneRef.value?.restore(draft)
    }
```

- [ ] **Step 3: 验证**

浏览器：步骤 5 断电完成后，左栏点「禁止合闸」→ 点 3 号空开 → 标识牌出现 → 自动提交并进入步骤 6（URL `stepOrder=6`）。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue frontend/src/views/LowVoltage/LResidentBoxView.vue
git commit -m "feat(scene): 居民表箱内部断电挂牌阶段"
```

---

### Task 7: 阶段 6 挂表

**Files:**
- Modify: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`

**Interfaces:**
- Consumes: `rightTool === 'meter'`、`Images.singlePhaseMeterOpen`、`Images.barSinglePhaseMeter`
- Produces: `meters`（4 个表位状态）

- [ ] **Step 1: 表位常量与图层**

坐标区追加：

```js
// 4 个表位的左上角与宽度（相对画布 %），按左上→右上→左下→右下编号 1~4
const METER_SLOTS = [
  { x: 56, y: 12, w: 18 },
  { x: 78, y: 12, w: 18 },
  { x: 56, y: 56, w: 18 },
  { x: 78, y: 56, w: 18 }
]
```

新增图层变量与状态：

```js
let meterLayer = null
const meterPlaced = reactive([false, false, false, false])
```

`createCanvas()` 中在 `breakerLayer` 之后加 `meterLayer = new Group(); leafer.add(meterLayer)`，并在 `buildBreakers` 后调用 `buildMeterSlots(w, h)` / `renderMeters(w, h)`。

```js
let slotRects = []

function buildMeterSlots(w, h) {
  hitLayer.removeAll()
  slotRects = []
  METER_SLOTS.forEach((slot, i) => {
    const rect = new Rect({
      x: (w * slot.x) / 100,
      y: (h * slot.y) / 100,
      width: (w * slot.w) / 100,
      height: (h * slot.w) / 100 / 0.7,
      fill: 'rgba(0,210,255,0.08)'
    })
    rect.on(PointerEvent.CLICK, () => onSlotClick(i))
    hitLayer.add(rect)
    slotRects.push(rect)
  })
}

function onSlotClick(i) {
  if (props.disabled || props.stage !== 6) return
  if (props.rightTool !== 'meter') {
    ElMessage.warning('请先从右侧选择「单相智能电表」')
    emit('error')
    return
  }
  if (meterPlaced[i]) return
  meterPlaced[i] = true
  renderMeters()
  emit('operation')
}
```

`renderMeters`：

```js
let meterNodes = []

function renderMeters() {
  if (!meterLayer || !leafer) return
  meterNodes.forEach(n => n.remove())
  meterNodes = []
  const w = leafer.width
  const h = leafer.height
  METER_SLOTS.forEach((slot, i) => {
    if (!meterPlaced[i]) return
    const width = (w * slot.w) / 100
    const node = new Image({
      url: Images.singlePhaseMeterOpen,
      x: (w * slot.x) / 100,
      y: (h * slot.y) / 100,
      width,
      height: width / 0.75
    })
    meterLayer.add(node)
    meterNodes.push(node)
  })
}
```

> 表图宽高比按素材实际值调整（`19.png` 1011×1290 ≈ 0.784，可先用 0.75 占位）。

- [ ] **Step 2: 验证**

浏览器：步骤 6 右栏选「单相智能电表」→ 点 4 个表位 → 依次出现开盖电表；未选工具点表位有提示。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue
git commit -m "feat(scene): 表箱内部挂表交互"
```

---

### Task 8: 阶段 6 接线

**Files:**
- Modify: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`

**Interfaces:**
- Consumes: `wires`（16 个连接状态）
- Produces: 16 根线全部接好后 `emit('complete')`

- [ ] **Step 1: 线头与孔常量（各自独立）**

坐标区追加：

```js
// 16 个线头热区：下标 = 表序号 * 4 + 线序（0 = 最右）
const WIRE_START_POS = [
  { x: 60, y: 45 }, { x: 64, y: 45 }, { x: 68, y: 45 }, { x: 72, y: 45 },
  { x: 82, y: 45 }, { x: 86, y: 45 }, { x: 90, y: 45 }, { x: 94, y: 45 },
  { x: 60, y: 89 }, { x: 64, y: 89 }, { x: 68, y: 89 }, { x: 72, y: 89 },
  { x: 82, y: 89 }, { x: 86, y: 89 }, { x: 90, y: 89 }, { x: 94, y: 89 }
]
// 每表 4 个端子孔：相对电表左上角的比率（0~1），从右往左编号
const HOLE_RATIO = [
  { x: 0.80, y: 0.94 }, { x: 0.70, y: 0.94 }, { x: 0.60, y: 0.94 }, { x: 0.50, y: 0.94 }
]
// 线色（只做视觉）：表 1/2 黄黄黑黑，表 3 绿绿黑黑，表 4 红红黑黑（从右往左）
const WIRE_COLORS = [
  ['#111', '#111', '#f5c518', '#f5c518'],
  ['#111', '#111', '#f5c518', '#f5c518'],
  ['#111', '#111', '#2e9d4f', '#2e9d4f'],
  ['#111', '#111', '#d23f3f', '#d23f3f']
]
```

- [ ] **Step 2: 接线状态机**

```js
let wireLayer = null
// 已接线：`${meterIndex}-${lineIndex}` -> true
const wires = reactive({})
// 当前选中的线：{ meter, line } | null
const selectedWire = ref(null)

function wireDone(meter, line) {
  return !!wires[`${meter}-${line}`]
}

function meterNextLine(meter) {
  for (let line = 0; line < 4; line++) {
    if (!wireDone(meter, line)) return line
  }
  return -1
}

function onWireStartClick(meter, line) {
  if (props.disabled || props.stage !== 6) return
  if (!meterPlaced[meter]) return
  const next = meterNextLine(meter)
  if (next === -1) return
  if (line !== next) {
    ElMessage.warning('请按从右往左的顺序接线')
    emit('error')
    return
  }
  selectedWire.value = { meter, line }
  emit('operation')
}

function onHoleClick(meter, hole) {
  if (props.disabled || props.stage !== 6) return
  const sel = selectedWire.value
  if (!sel) {
    ElMessage.warning('请先点击要连接的导线')
    emit('error')
    return
  }
  if (sel.meter !== meter) {
    ElMessage.warning('请连接当前电表的导线')
    emit('error')
    return
  }
  if (hole !== sel.line) {
    ElMessage.warning('请连接到对应的端子孔')
    emit('error')
    selectedWire.value = null
    return
  }
  wires[`${meter}-${sel.line}`] = true
  selectedWire.value = null
  drawWire(meter, sel.line)
  emit('operation')
  if (Object.keys(wires).length === 16) emit('complete')
}
```

- [ ] **Step 3: 热区与导线绘制**

```js
function buildWireHotspots(w, h) {
  // 线头热区
  WIRE_START_POS.forEach((pos, idx) => {
    const meter = Math.floor(idx / 4)
    const line = idx % 4
    const r = new Rect({
      x: (w * pos.x) / 100,
      y: (h * pos.y) / 100,
      width: (w * 1.5) / 100,
      height: (w * 1.5) / 100,
      fill: 'rgba(255,255,255,0.001)'
    })
    r.on(PointerEvent.CLICK, () => onWireStartClick(meter, line))
    hitLayer.add(r)
  })
  // 端子孔热区（相对电表）
  METER_SLOTS.forEach((slot, meter) => {
    if (!meterPlaced[meter]) return
    const meterW = (w * slot.w) / 100
    const meterH = meterW / 0.75
    HOLE_RATIO.forEach((ratio, line) => {
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * ratio.x,
        y: (h * slot.y) / 100 + meterH * ratio.y,
        width: (w * 1.4) / 100,
        height: (w * 1.4) / 100,
        fill: 'rgba(0,210,255,0.10)'
      })
      r.on(PointerEvent.CLICK, () => onHoleClick(meter, line))
      hitLayer.add(r)
    })
  })
}

function holeAbsPos(meter, line) {
  const slot = METER_SLOTS[meter]
  const w = leafer.width
  const h = leafer.height
  const meterW = (w * slot.w) / 100
  const meterH = meterW / 0.75
  const ratio = HOLE_RATIO[line]
  return {
    x: (w * slot.x) / 100 + meterW * ratio.x,
    y: (h * slot.y) / 100 + meterH * ratio.y
  }
}

function startAbsPos(meter, line) {
  const pos = WIRE_START_POS[meter * 4 + line]
  return { x: (leafer.width * pos.x) / 100, y: (leafer.height * pos.y) / 100 }
}

function drawWire(meter, line) {
  if (!wireLayer) return
  const from = startAbsPos(meter, line)
  const to = holeAbsPos(meter, line)
  const color = WIRE_COLORS[meter][line]
  wireLayer.add(
    new Path({
      x: 0,
      y: 0,
      path: `M ${from.x} ${from.y} L ${to.x} ${to.y}`,
      stroke: color,
      strokeWidth: Math.max(2, leafer.width * 0.003),
      strokeCap: 'round'
    })
  )
}
```

`createCanvas()` 中创建 `wireLayer`（在 meterLayer 之后、hitLayer 之前），并在挂表后重建热区：`onSlotClick` 成功后调用 `buildWireHotspots(leafer.width, leafer.height)`。`renderMeters()` 完成后也调用一次。

`restore` 时对每条已接的线调用 `drawWire` 并重建热区。

- [ ] **Step 4: 验证**

浏览器：挂完 4 表后，按从右往左点线头 → 点对应孔，线出现；乱序/错孔有提示且计错；16 根完成自动提交进入步骤 7。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue
git commit -m "feat(scene): 表箱内部接线交互"
```

---

### Task 9: 阶段 7 上电、合盖、表体铅封

**Files:**
- Modify: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`

**Interfaces:**
- Consumes: `rightTool === 'seal'`
- Produces: 8 处铅封完成后 `emit('box-done')`，画布由父页面卸载

- [ ] **Step 1: 上电顺序 + 标识牌销毁**

新增状态：

```js
const stage7Step = ref(0) // 0 断 3 号排, 1 断 2 排, 2 断 1 排, 3 完成
const covers = reactive([false, false, false, false])
const seals = reactive([false, false, false, false, false, false, false, false])
```

`onBreakerClick` 在 `props.stage === 7` 时走独立分支：

```js
function onBreakerClick(id) {
  if (props.disabled) return
  if (props.stage === 7) return onBreakerPowerOn(id)
  if (props.stage !== 5) return
  // ...阶段 5 逻辑保持不变
}

function onBreakerPowerOn(id) {
  const def = BREAKER_DEFS.find(d => d.id === id)
  if (!def) return
  const expected = [3, 2, 1][stage7Step.value]
  if (stage7Step.value < 3 && def.group !== expected) {
    ElMessage.warning('请按 3 → 2 → 1 的顺序上电')
    emit('error')
    return
  }
  if (breakerStates[id] === 'on') return
  breakerStates[id] = 'on'
  refreshBreaker(def)
  emit('operation')
  // 第一个 3 号排空开推上时销毁标识牌，只销毁一次
  if (def.group === 3 && signPlaced.value) {
    signPlaced.value = false
    if (signNode) {
      signNode.remove()
      signNode = null
    }
  }
  const groupDone = BREAKER_DEFS.filter(d => d.group === def.group).every(
    d => breakerStates[d.id] === 'on'
  )
  if (groupDone) stage7Step.value++
}
```

- [ ] **Step 2: 合盖**

表盖上叠加一层热区（合盖前）：

```js
// 表盖热区相对电表（宽 0~1，高 0~1）
const COVER_RATIO = { x: 0.1, y: 0.75, w: 0.8, h: 0.2 }
```

在 `buildWireHotspots` 末尾追加表盖热区（仅 `props.stage === 7 && !covers[meter]`）：

```js
  if (props.stage === 7) {
    METER_SLOTS.forEach((slot, meter) => {
      if (!meterPlaced[meter] || covers[meter]) return
      const meterW = (w * slot.w) / 100
      const meterH = meterW / 0.75
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * COVER_RATIO.x,
        y: (h * slot.y) / 100 + meterH * COVER_RATIO.y,
        width: meterW * COVER_RATIO.w,
        height: meterH * COVER_RATIO.h,
        fill: 'rgba(0,210,255,0.10)'
      })
      r.on(PointerEvent.CLICK, () => onCoverClick(meter))
      hitLayer.add(r)
    })
  }
```

```js
function onCoverClick(meter) {
  if (props.disabled || props.stage !== 7) return
  if (covers[meter]) return
  covers[meter] = true
  renderMeters() // 合盖后切换为 barSinglePhaseMeter
  buildWireHotspots(leafer.width, leafer.height)
  emit('operation')
}
```

`renderMeters` 中电表图按状态选择：

```js
      url: covers[i] ? Images.barSinglePhaseMeter : Images.singlePhaseMeterOpen,
```

（`covers` 在阶段 6 全为 false，不影响挂表。）

- [ ] **Step 3: 每表两处铅封**

坐标区追加：

```js
// 每表 2 处铅封相对电表（宽/高比率）
const SEAL_RATIOS = [
  { x: 0.22, y: 0.13 },
  { x: 0.78, y: 0.13 }
]
```

```js
function buildSealHotspots(w, h) {
  if (props.stage !== 7) return
  METER_SLOTS.forEach((slot, meter) => {
    if (!meterPlaced[meter] || !covers[meter]) return
    const meterW = (w * slot.w) / 100
    const meterH = meterW / 0.75
    SEAL_RATIOS.forEach((ratio, k) => {
      const idx = meter * 2 + k
      if (seals[idx]) return
      const r = new Rect({
        x: (w * slot.x) / 100 + meterW * ratio.x,
        y: (h * slot.y) / 100 + meterH * ratio.y,
        width: (w * 2) / 100,
        height: (w * 2) / 100,
        fill: 'rgba(0,210,255,0.12)'
      })
      r.on(PointerEvent.CLICK, () => onSealClick(meter, k))
      hitLayer.add(r)
    })
  })
}

function onSealClick(meter, k) {
  if (props.disabled || props.stage !== 7) return
  if (props.rightTool !== 'seal') {
    ElMessage.warning('请先从右侧选择「铅封」')
    emit('error')
    return
  }
  const idx = meter * 2 + k
  if (seals[idx]) return
  seals[idx] = true
  drawSeal(meter, k)
  emit('operation')
  if (seals.every(Boolean)) emit('box-done')
}

function drawSeal(meter, k) {
  const slot = METER_SLOTS[meter]
  const meterW = (leafer.width * slot.w) / 100
  const meterH = meterW / 0.75
  const ratio = SEAL_RATIOS[k]
  const size = leafer.width * 0.025
  const node = new Image({
    url: Images.barSeal,
    x: (leafer.width * slot.x) / 100 + meterW * ratio.x,
    y: (leafer.height * slot.y) / 100 + meterH * ratio.y,
    width: size,
    height: size
  })
  if (!sealLayer) {
    sealLayer = new Group()
    leafer.add(sealLayer)
  }
  sealLayer.add(node)
}
```

`createCanvas()` 中依次创建 `meterLayer`、`wireLayer`、`sealLayer`（zIndex 递增）和 `hitLayer`；在 `renderMeters()` 后调用 `buildWireHotspots`、`buildSealHotspots`。

- [ ] **Step 4: 验证**

浏览器：步骤 7 按 3→2→1 推闸（推到 3 号时标识牌消失且不再出现）→ 点 4 个表盖合上 → 选铅封 → 点 8 处铅封 → 画布自动消失回到主场景。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue
git commit -m "feat(scene): 表箱内部上电合盖与铅封"
```

---

### Task 10: 门把铅封、收尾跳转与存档恢复

**Files:**
- Modify: `frontend/src/views/LowVoltage/LResidentBoxView.vue`
- Modify: `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`

**Interfaces:**
- Consumes: 画布 `@box-done`
- Produces: 门把铅封完成后提交步骤 7 并 `router.push('/LPT')`
- Produces: `boxInnerDone` 存档标志，刷新后直接进入门把阶段

- [ ] **Step 1: 父页面处理画布完成与门把铅封**

```js
// 8 处表体铅封完成：卸载画布回到主场景，等待门把铅封
const boxInnerDone = ref(false)

function onBoxInnerDone() {
  boxInnerDone.value = true
  ElMessage.success('表箱内部操作完成，请给表箱门把加铅封')
}
```

模板中画布改为：

```vue
    <LResidentBoxCabinetCanvas
      v-if="stage >= 5 && !boxInnerDone"
      ...
      @box-done="onBoxInnerDone"
    />
```

主场景门把热区：`LResidentBoxScene` 已有 `DOOR_CHECK_ZONE`，但外部场景在 stage≥5 时被 `disabled`。需要新增一个门把专用事件：在 `LResidentBoxScene` 的 `onSceneClick` 中，若 `props.stage === 7 && props.boxInnerReady` 且命中门把 → `emit('door-sealed')`。

`LResidentBoxScene.vue` 增加：

```js
const props = defineProps({
  stage: { type: Number, required: true },
  disabled: { type: Boolean, default: false },
  /** 步骤 7 表箱内部完成，只允许门把铅封 */
  doorReady: { type: Boolean, default: false }
})
const emit = defineEmits([
  'complete', 'operation', 'error', 'left-tool-select', 'right-tool-select', 'door-sealed'
])
```

`onSceneClick` 开头：

```js
function onSceneClick(e) {
  if (props.doorReady) {
    if (hitZone(e, DOOR_CHECK_ZONE)) {
      emit('door-sealed')
    }
    return
  }
  if (props.disabled) return
  ...
}
```

父页面模板：

```vue
    <LResidentBoxScene
      ...
      :disabled="!canInteract || (stage >= 5 && !boxInnerDone)"
      :door-ready="stage === 7 && boxInnerDone"
      @door-sealed="onDoorSealed"
      ...
    />
```

```js
const doorPending = ref(false)

async function onDoorSealed() {
  if (stage.value !== 7 || !boxInnerDone.value || doorPending.value) return
  // 必须先选铅封工具
  if (canvasRightTool.value !== 'seal') {
    ElMessage.warning('请先从右侧选择「铅封」')
    errorCount.value++
    return
  }
  doorPending.value = true
  doorSealed.value = true
  ElMessage.success('加铅封完成')
  await onFinalStepComplete()
}
```

- [ ] **Step 2: 提交步骤 7 并跳转**

```js
const doorSealed = ref(false)

async function onFinalStepComplete() {
  submitting.value = true
  try {
    await submitStep(buildPayload(1))
    ElMessage.success('低压实验完成')
    setTimeout(() => router.push('/LPT'), 800)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
    doorPending.value = false
  } finally {
    submitting.value = false
  }
}
```

`buildPayload` 的 `currentState` 在阶段 7 且 `boxInnerDone` 时追加标志：

```js
function currentState() {
  if (stage.value >= 5) {
    return { ...(canvasRef.value?.getState() || {}), boxInnerDone: boxInnerDone.value }
  }
  return sceneRef.value?.getState() || {}
}
```

> 画布已卸载时 `canvasRef.value` 可能为 null，`boxInnerDone` 仍会写入。

- [ ] **Step 3: 恢复逻辑**

`handleRestore` 在阶段 7 读草稿后：

```js
    const draft = await getStepDraft(experimentId.value, stepId.value)
    if (draft && Object.keys(draft).length > 0) {
      if (draft.boxInnerDone) boxInnerDone.value = true
      if (stage.value >= 5 && !boxInnerDone.value) canvasRef.value?.restore(draft)
      else if (stage.value < 5) sceneRef.value?.restore(draft)
    }
```

画布 `getState` 补充：

```js
function getState() {
  return {
    stage: props.stage,
    breakers: { ...breakerStates },
    signPlaced: signPlaced.value,
    meters: [...meterPlaced],
    wires: { ...wires },
    covers: [...covers],
    seals: [...seals]
  }
}
```

`restore` 依次恢复并重绘：

```js
function restore(state) {
  if (!state) return
  if (state.breakers) {
    Object.keys(state.breakers).forEach(id => {
      if (breakerStates[id] !== undefined) breakerStates[id] = state.breakers[id]
    })
  }
  if (typeof state.signPlaced === 'boolean') signPlaced.value = state.signPlaced
  if (Array.isArray(state.meters)) state.meters.forEach((v, i) => (meterPlaced[i] = !!v))
  if (state.wires) {
    Object.keys(state.wires).forEach(k => (wires[k] = !!state.wires[k]))
  }
  if (Array.isArray(state.covers)) state.covers.forEach((v, i) => (covers[i] = !!v))
  if (Array.isArray(state.seals)) state.seals.forEach((v, i) => (seals[i] = !!v))
  if (!leafer) return
  BREAKER_DEFS.forEach(refreshBreaker)
  renderSign()
  renderMeters()
  buildWireHotspots(leafer.width, leafer.height)
  buildSealHotspots(leafer.width, leafer.height)
  Object.keys(wires).forEach(k => {
    const [meter, line] = k.split('-').map(Number)
    if (wires[k]) drawWire(meter, line)
  })
  seals.forEach((v, idx) => {
    if (v) drawSeal(Math.floor(idx / 2), idx % 2)
  })
}
```

- [ ] **Step 4: 验证**

1. 三步验电后进入步骤 5，完整走到门把铅封 → 提示 → 跳 `/LPT`
2. 阶段 5/6 中途点“保存进度”→ 刷新 → 状态恢复
3. 8 处表体铅封完成后刷新 → 直接显示主场景门把阶段，不再出现画布
4. 完整流程结束后实验状态为已完成（`unfinished` 列表不再包含）

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/LowVoltage/LResidentBoxView.vue frontend/src/components/LowVoltage/LResidentBoxScene.vue frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue
git commit -m "feat(scene): 表箱门把铅封与内部场景收尾"
```

---

## 自审记录

- 设计 4.1 中 7.jpg 迁移已在 Task 1 覆盖
- 设计 6.3 的 `boxInnerDone` 已在 Task 10 覆盖（提交 payload + 恢复跳过画布）
- 设计 7 的存档字段与 Task 10 的 `getState/restore` 一致
- 设计 6.1/6.3 的顺序判定分别落在 Task 5 / Task 9
- 设计 6.2 的接线固定对应落在 Task 8
- 设计 5.4 的 16 个线头独立常量落在 Task 8
- 设计 4.2 的 1 号 off 共用 on 图落在 Task 5 的 `breakerImg`
- 无 TBD/TODO；所有任务均给出可执行代码或命令
