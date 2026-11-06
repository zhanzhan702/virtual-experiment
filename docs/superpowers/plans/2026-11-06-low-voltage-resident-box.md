# 低压居民表箱外部场景（/LRB） Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 DOM + 百分比定位实现低压居民表箱外部场景，包含「架设围栏并悬挂标识牌」（步骤 3）与「三步式验电」（步骤 4），替换掉现有的 `/LSO` 占位页。

**Architecture:** 一个场景组件负责渲染与交互（背景图 + 左右工具栏 + 百分比定位的叠加元素 + 点击命中判定），一个视图组件负责编排（草稿、计时、统计、提交、步骤推进）。所有坐标常量集中在独立配置文件里，供使用者微调。

**Tech Stack:** Vue 3 (`<script setup>`) + Vue Router 4 + Element Plus。无测试框架，验证方式为构建 + 浏览器手动走查。

## Global Constraints

- 文件名规范：视图 `PascalCase` + `View` 后缀、低压以大写 `L` 开头；JS 配置文件 `kebab-case`
- 组件必须使用 `<script setup>`，导入一律用 `@/` 别名，禁止相对路径
- 图片统一从 `frontend/src/constants/images.js` 导出，组件内用 `Images.xxx`
- 提交信息格式 `<type>(<scope>): <中文描述>`
- 前端格式化：`cd frontend && npm run format`（注意它会顺带格式化全仓库，提交前用 `git status` 确认只暂存本任务的文件）
- 不允许 `git add -A`，逐文件暂存
- 无单元测试框架，每个任务的验证是 `npm run build` 通过 + 浏览器走查

---

## 文件结构

| 文件 | 职责 |
| --- | --- |
| `frontend/src/constants/resident-box-scene.js` | 全部场景坐标常量（使用者微调入口） |
| `frontend/src/components/LowVoltage/LResidentBoxScene.vue` | 场景渲染 + 交互 + 命中判定；暴露 `getState()` / `restore()` |
| `frontend/src/views/LowVoltage/LResidentBoxView.vue` | 页面编排：草稿、计时、统计、提交、步骤推进 |
| `frontend/src/router/index.js` | `/LSO` → `/LRB` |
| `frontend/src/views/LowVoltage/LToolSelectionView.vue` | 跳转目标改 `/LRB` |
| `frontend/src/views/LowVoltage/LSceneOverviewView.vue` | 删除 |
| `frontend/src/constants/images.js` | 注册 `residentBoxBg` |
| `backend/docs/sql/04_low_experiment_templates.sql` | 追加步骤 3、4 |
| `backend/docs/sql/09_low_resident_box_steps.sql` | 幂等增量脚本 |

---

### Task 1: 背景图注册与坐标配置

**Files:**
- Modify: `frontend/src/constants/images.js`（新增一条 import 与一处导出）
- Create: `frontend/src/constants/resident-box-scene.js`

**Interfaces:**
- Produces: `Images.residentBoxBg`
- Produces（配置常量，供 Task 3 的组件直接 import）：
  `LEFT_FENCE_STYLE`、`RIGHT_FENCE_STYLE`、`SIGN_DANGER_STYLE`、`SIGN_WORKING_STYLE`、
  `POWER_SOCKET_STYLE`、`DOOR_CHECK_ZONE`、`FENCE_HIT_ZONE`、`SIGN_HIT_ZONE`

- [ ] **Step 1: 在 images.js 注册背景图**

在 `import junctionBoxSwitch from '@/assets/images/cabinet/room/JunctionBoxSwitch.png'` 之后追加：

```js
// 低压居民表箱外部场景背景
import residentBoxBg from '@/assets/images/cabinet/7.jpg'
```

在默认导出对象里 `junctionBoxSwitch,` 之后追加：

```js
  residentBoxBg,
```

- [ ] **Step 2: 创建坐标配置文件**

```js
/**
 * 低压居民表箱外部场景的坐标与尺寸。
 *
 * ★★★ 本文件是位置/大小的唯一调整入口 —— 改这里即可，不需要动组件 ★★★
 *
 * 所有数值都是「相对场景图」的百分比：
 *   left / top   元素左上角在场景图中的位置
 *   width        元素宽度占场景图宽度的比例
 *   height       固定写 'auto'，按图片原始比例缩放
 *
 * 场景容器（.scene-group）的尺寸由内部背景图决定，所以改窗口大小时整体等比缩放，
 * 这里的百分比不需要跟着改。
 */

// ── 围栏：左右两半各自独立落位 ─────────────────────────────
export const LEFT_FENCE_STYLE = { left: '-12%', top: '58%', width: '56%', height: 'auto' }
export const RIGHT_FENCE_STYLE = { left: '56%', top: '63%', width: '46%', height: 'auto' }

// ── 两块标识牌：分开挂，各自独立的位置与大小 ────────────────
export const SIGN_DANGER_STYLE = { left: '3%', top: '40%', width: '10%', height: 'auto' }
export const SIGN_WORKING_STYLE = { left: '82%', top: '40%', width: '10%', height: 'auto' }

// ── 排插：叠在背景左上角（与高压一致，不画进背景图） ────────
export const POWER_SOCKET_STYLE = { left: '2%', top: '12%', width: '7%', height: 'auto' }

// ── 三步验电第二步：表箱门把热区 ───────────────────────────
// 只做命中判定；验电进行到第二步时会显示半透明高亮框，方便调坐标
export const DOOR_CHECK_ZONE = { left: '52%', top: '30%', width: '8%', height: '12%' }

// ── 点击判定区域（不显示，只决定「点哪里算点中」） ──────────
export const FENCE_HIT_ZONE = { left: '5%', top: '55%', width: '90%', height: '35%' }
export const SIGN_HIT_ZONE = { left: '30%', top: '8%', width: '40%', height: '45%' }
```

- [ ] **Step 3: 验证构建**

Run: `cd frontend && npm run build`
Expected: `✓ built in ...`，无 `Could not resolve`

- [ ] **Step 4: 提交**

```bash
git add frontend/src/constants/images.js frontend/src/constants/resident-box-scene.js
git commit -m "feat(config): 新增居民表箱场景背景与坐标配置"
```

---

### Task 2: 数据库补步骤 3、4

**Files:**
- Modify: `backend/docs/sql/04_low_experiment_templates.sql`
- Create: `backend/docs/sql/09_low_resident_box_steps.sql`

**Interfaces:**
- Produces: `template_id = 0x...32` 下 `step_order` 为 3、4 的两条记录，
  `step_code` 分别为 `ERECT_FENCE`、`THREE_STEP_CHECK`

- [ ] **Step 1: 在权威脚本追加两步**

把文件末尾（当前是第 2 步那行）的

```sql
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00);
```

改为：

```sql
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 3, 'ERECT_FENCE', '架设围栏并悬挂标识牌', 120, 20.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 4, 'THREE_STEP_CHECK', '三步式验电', 90, 15.00);
```

同时把文件顶部 UUID 分配表那行更新为：

```sql
--   32 低压训练场景V1（LV_TRAIN_V1）：1 填写工作票 / 2 工器具选择 / 3 架设围栏并悬挂标识牌 / 4 三步式验电
```

- [ ] **Step 2: 创建幂等增量脚本**

```sql
-- V9 低压场景补第 3、4 步：架设围栏并悬挂标识牌、三步式验电
--
-- 用途：已按旧版 04 脚本建过库的环境，执行本脚本补齐。
-- 幂等：目标 step_order 已存在时不会重复插入，可重复执行。
--
-- 分值 120 秒/20 分、90 秒/15 分为暂定；模板总分变为 90 分，
-- 成绩按「原始分 ÷ 步骤分合计 × 100」换算，百分比口径不受影响。
--
-- 注意（Windows）：执行时需指定字符集，否则中文会报
--   ERROR 1366 (HY000) Incorrect string value
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 09_low_resident_box_steps.sql

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 3, 'ERECT_FENCE', '架设围栏并悬挂标识牌', 120, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = 0x00000000000000000000000000000032 AND step_order = 3
);

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 4, 'THREE_STEP_CHECK', '三步式验电', 90, 15.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = 0x00000000000000000000000000000032 AND step_order = 4
);
```

- [ ] **Step 3: 执行并核对**

```bash
export MYSQL_PWD='<数据库密码>'
MYSQL="/d/Program Files/MySQL/MySQL Server 8.0/bin/mysql"
"$MYSQL" --default-character-set=utf8mb4 -u root virtual_experiment < backend/docs/sql/09_low_resident_box_steps.sql
"$MYSQL" --default-character-set=utf8mb4 -u root virtual_experiment -e "
SELECT step_order, step_code, step_name, required_seconds, score
FROM experiment_steps WHERE template_id = 0x00000000000000000000000000000032 ORDER BY step_order;"
```

Expected:
```
1  FILL_TICKET      填写工作票              300  25.00
2  SELECT_TOOLS     工器具选择              180  30.00
3  ERECT_FENCE      架设围栏并悬挂标识牌     120  20.00
4  THREE_STEP_CHECK 三步式验电               90  15.00
```

再执行一次脚本确认幂等（仍只有 4 行）。

- [ ] **Step 4: 提交**

```bash
git add backend/docs/sql/04_low_experiment_templates.sql backend/docs/sql/09_low_resident_box_steps.sql
git commit -m "feat(experiment): 低压模板新增围栏与验电步骤"
```

---

### Task 3: 场景组件（步骤 3 放置 + 步骤 4 验电）

**Files:**
- Create: `frontend/src/components/LowVoltage/LResidentBoxScene.vue`

**Interfaces:**
- Consumes: `@/constants/resident-box-scene` 的 8 个样式/热区常量；`Images.residentBoxBg`、`Images.powerSocket`、`Images.barLeftFence`、`Images.barRightFence`、`Images.barSignStopHighVoltage`、`Images.barSignPersonWorking`、`Images.barVoltageTesterNormal`、`Images.barVoltageTesterWarning`
- Props: `stage: Number`（3 或 4）、`disabled: Boolean`
- Emits: `complete`（本阶段任务完成）、`operation`（发生一次有效操作）、`error`（操作失误，父组件据此累加错误次数）
- `defineExpose({ getState, restore })`

- [ ] **Step 1: 创建组件**

```vue
<template>
  <div
    class="scene-stage"
    :class="{ following: isFollowing }"
    @mousemove="moveCursor"
    @mouseup="vtProbing = false"
  >
    <!-- 场景容器：尺寸由内部背景图决定，所有叠加元素按百分比定位 -->
    <div ref="groupRef" class="scene-group" @click="onSceneClick">
      <img :src="Images.residentBoxBg" alt="居民表箱场景" class="scene-bg" draggable="false" />

      <!-- 排插：叠在背景左上角，与高压一致 -->
      <img
        :src="Images.powerSocket"
        alt="排插"
        class="overlay power-socket"
        :style="POWER_SOCKET_STYLE"
        draggable="false"
      />

      <!-- 已放置：围栏左右两半 -->
      <img
        v-if="placed[0]"
        :src="Images.barLeftFence"
        alt="围栏（左）"
        class="overlay"
        :style="LEFT_FENCE_STYLE"
        draggable="false"
      />
      <img
        v-if="placed[0]"
        :src="Images.barRightFence"
        alt="围栏（右）"
        class="overlay"
        :style="RIGHT_FENCE_STYLE"
        draggable="false"
      />

      <!-- 已放置：两块标识牌，各自独立位置 -->
      <img
        v-if="placed[1]"
        :src="Images.barSignStopHighVoltage"
        alt="有电危险标识牌"
        class="overlay"
        :style="SIGN_DANGER_STYLE"
        draggable="false"
      />
      <img
        v-if="placed[2]"
        :src="Images.barSignPersonWorking"
        alt="有人工作标识牌"
        class="overlay"
        :style="SIGN_WORKING_STYLE"
        draggable="false"
      />

      <!-- 三步验电第二步：门把热区高亮（同时是调坐标的可视化参照） -->
      <div v-if="vtActive && vtStep === 1" class="check-zone" :style="zoneStyle(DOOR_CHECK_ZONE)" />
    </div>

    <!-- 左侧工具栏：步骤 3 -->
    <aside v-if="stage === 3" class="toolbar toolbar-left">
      <button
        v-for="tool in LEFT_TOOLS"
        :key="tool.idx"
        type="button"
        class="tool-btn"
        :class="{ active: followingTool === tool.idx }"
        :aria-label="tool.name"
        @click.stop="selectTool(tool.idx)"
      >
        <img :src="tool.img" :alt="tool.name" draggable="false" />
      </button>
    </aside>

    <!-- 右侧工具栏：步骤 4 -->
    <aside v-if="stage === 4" class="toolbar toolbar-right">
      <button
        type="button"
        class="tool-btn"
        :class="{ active: vtActive }"
        aria-label="验电笔"
        @click.stop="toggleVoltageTester"
      >
        <img :src="Images.barVoltageTesterNormal" alt="验电笔" draggable="false" />
      </button>
    </aside>

    <!-- 鼠标跟随：围栏/标识牌中心对准光标；验电笔尖端对准光标 -->
    <img
      v-if="showFollower"
      class="follower"
      :src="followerImg"
      alt=""
      :style="followerStyle"
      draggable="false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import {
  LEFT_FENCE_STYLE,
  RIGHT_FENCE_STYLE,
  SIGN_DANGER_STYLE,
  SIGN_WORKING_STYLE,
  POWER_SOCKET_STYLE,
  DOOR_CHECK_ZONE,
  FENCE_HIT_ZONE,
  SIGN_HIT_ZONE
} from '@/constants/resident-box-scene'

const props = defineProps({
  /** 当前阶段：3 = 架设围栏并悬挂标识牌，4 = 三步式验电 */
  stage: { type: Number, required: true },
  /** 恢复失败等场景下禁止交互 */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['complete', 'operation', 'error'])

const groupRef = ref(null)

// ── 步骤 3：放置状态 ──────────────────────────────────────
// 顺序：0 围栏、1 有电危险牌、2 有人工作牌
const placed = reactive([false, false, false])
const followingTool = ref(null)

const LEFT_TOOLS = [
  { idx: 0, name: '围栏', img: Images.barLeftFence },
  { idx: 1, name: '有电危险标识牌', img: Images.barSignStopHighVoltage },
  { idx: 2, name: '有人工作标识牌', img: Images.barSignPersonWorking }
]

// ── 步骤 4：三步验电 ──────────────────────────────────────
// 0 待验电 → 1 已点排插 → 2 已点门把 → 3 完成
const vtActive = ref(false)
const vtStep = ref(0)
const vtDone = ref(false)
const vtProbing = ref(false)

const vtImg = computed(() =>
  vtProbing.value ? Images.barVoltageTesterWarning : Images.barVoltageTesterNormal
)

// ── 鼠标跟随 ──────────────────────────────────────────────
const isFollowing = computed(
  () => !props.disabled && (followingTool.value !== null || vtActive.value)
)
const showFollower = computed(() => isFollowing.value)
const cursor = reactive({ x: 0, y: 0 })
// 验电笔的尖端相对图片中心的偏移（正 = 右/下），让尖端对准光标
const VOLTAGE_TESTER_TIP = { x: -28, y: 28 }

const followerImg = computed(() => {
  if (vtActive.value) return vtImg.value
  return LEFT_TOOLS[followingTool.value]?.img || Images.barLeftFence
})

const followerStyle = computed(() => {
  const style = { left: cursor.x + 'px', top: cursor.y + 'px' }
  if (vtActive.value) {
    style['--follow-ox'] = -VOLTAGE_TESTER_TIP.x + 'px'
    style['--follow-oy'] = -VOLTAGE_TESTER_TIP.y + 'px'
  }
  return style
})

function moveCursor(e) {
  if (!isFollowing.value) return
  cursor.x = e.clientX
  cursor.y = e.clientY
}

// ── 命中判定 ──────────────────────────────────────────────

/** 百分比矩形是否命中光标（相对场景容器） */
function hitZone(e, zone) {
  const g = groupRef.value
  if (!g) return false
  const r = g.getBoundingClientRect()
  const x = r.left + (r.width * zone.left) / 100
  const y = r.top + (r.height * zone.top) / 100
  const w = (r.width * zone.width) / 100
  const h = (r.height * zone.height) / 100
  return e.clientX >= x && e.clientX <= x + w && e.clientY >= y && e.clientY <= y + h
}

function zoneStyle(zone) {
  return {
    left: zone.left + '%',
    top: zone.top + '%',
    width: zone.width + '%',
    height: zone.height + '%'
  }
}

function fail(message) {
  ElMessage.warning(message)
  emit('error')
}

// ── 步骤 3 交互 ───────────────────────────────────────────
function selectTool(idx) {
  if (props.disabled) return
  emit('operation')
  vtActive.value = false
  if (placed[idx]) {
    fail('该物品已放置')
    return
  }
  followingTool.value = followingTool.value === idx ? null : idx
}

function onSceneClick(e) {
  if (props.disabled) return
  if (props.stage === 3) handlePlace(e)
  else handleVoltageTest(e)
}

function handlePlace(e) {
  const idx = followingTool.value
  if (idx === null) {
    fail('请先从左侧工具栏选择工具')
    return
  }

  // 围栏与标识牌各自只认自己的命中区域
  const ok = idx === 0 ? hitZone(e, FENCE_HIT_ZONE) : hitZone(e, SIGN_HIT_ZONE)
  if (!ok) {
    fail('请选择正确的放置位置')
    return
  }

  placed[idx] = true
  followingTool.value = null
  emit('operation')
  if (placed.every(Boolean)) emit('complete')
}

// ── 步骤 4 交互 ───────────────────────────────────────────
function toggleVoltageTester() {
  if (props.disabled) return
  emit('operation')
  if (placed.some(v => !v)) {
    fail('请先完成围栏与标识牌放置')
    return
  }
  if (vtDone.value) {
    fail('验电已完成')
    return
  }
  if (vtActive.value) {
    vtActive.value = false
    vtStep.value = 0
    vtProbing.value = false
    return
  }
  vtActive.value = true
  vtStep.value = 0
}

function handleVoltageTest(e) {
  if (!vtActive.value || vtDone.value) return

  const onSocket = hitZone(e, POWER_SOCKET_STYLE)
  const onDoor = hitZone(e, DOOR_CHECK_ZONE)

  // 顺序错：归零并计一次错误（与高压一致）
  if (vtStep.value === 0 && onSocket) {
    vtStep.value = 1
  } else if (vtStep.value === 1 && onDoor) {
    vtStep.value = 2
  } else if (vtStep.value === 2 && onSocket) {
    vtStep.value = 3
    vtDone.value = true
    vtActive.value = false
    emit('operation')
    emit('complete')
  } else {
    vtStep.value = 0
    fail('验电顺序不正确，请按「排插 → 门把 → 排插」操作')
  }
}

// ── 对外状态读写 ──────────────────────────────────────────
function getState() {
  return {
    placed: [...placed],
    vtStep: vtStep.value,
    vtDone: vtDone.value
  }
}

function restore(state) {
  if (!state) return
  if (Array.isArray(state.placed) && state.placed.length === 3) {
    placed.splice(0, 3, ...state.placed.map(Boolean))
  }
  if (typeof state.vtStep === 'number') vtStep.value = state.vtStep
  if (typeof state.vtDone === 'boolean') vtDone.value = state.vtDone
}

defineExpose({ getState, restore })
</script>

<style scoped>
.scene-stage {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.scene-stage.following {
  cursor: crosshair;
}

/* 容器尺寸由背景图决定，叠加元素的百分比因此始终对齐图片 */
.scene-group {
  position: relative;
  display: inline-block;
  line-height: 0;
}

.scene-bg {
  max-width: 90vw;
  max-height: 85vh;
  display: block;
  user-select: none;
}

.overlay {
  position: absolute;
  height: auto;
  user-select: none;
  pointer-events: none;
}

/* 排插在验电时需要被点到，单独放开指针事件 */
.power-socket {
  pointer-events: auto;
}

/* 门把热区高亮：既是操作提示，也是调 DOOR_CHECK_ZONE 时的可视化参照 */
.check-zone {
  position: absolute;
  border: 2px dashed #00d2ff;
  background: rgba(0, 210, 255, 0.12);
  box-shadow: 0 0 18px rgba(0, 210, 255, 0.35);
  pointer-events: none;
}

/* 工具栏 */
.toolbar {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 10px;
  background: rgba(0, 141, 133, 0.9);
  border-radius: 16px;
  z-index: 20;
}

.toolbar-left {
  left: 2vw;
}

.toolbar-right {
  right: 2vw;
}

.tool-btn {
  width: 64px;
  height: 64px;
  padding: 6px;
  border: 2px solid transparent;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition:
    border-color 0.2s,
    transform 0.2s;
}

.tool-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.tool-btn:hover {
  transform: scale(1.05);
}

.tool-btn.active {
  border-color: #ffd400;
  box-shadow: 0 0 12px rgba(255, 212, 0, 0.8);
}

/* 鼠标跟随图：fixed 定位，中心（或验电笔尖端）对准光标 */
.follower {
  position: fixed;
  width: 72px;
  height: auto;
  pointer-events: none;
  z-index: 50;
  transform: translate(-50%, -50%) translate(var(--follow-ox, 0), var(--follow-oy, 0));
  user-select: none;
}
</style>
```

- [ ] **Step 2: 验证构建**

Run: `cd frontend && npm run build`
Expected: 构建成功（此时组件尚未被任何路由引用，但会被编译检查）

- [ ] **Step 3: 提交**

```bash
git add frontend/src/components/LowVoltage/LResidentBoxScene.vue
git commit -m "feat(component): 新增低压居民表箱场景组件"
```

---

### Task 4: 页面视图与路由替换

**Files:**
- Create: `frontend/src/views/LowVoltage/LResidentBoxView.vue`
- Modify: `frontend/src/router/index.js`（`/LSO` 改为 `/LRB`）
- Modify: `frontend/src/views/LowVoltage/LToolSelectionView.vue`（跳转目标）
- Delete: `frontend/src/views/LowVoltage/LSceneOverviewView.vue`

**Interfaces:**
- Consumes: `LResidentBoxScene`（Task 3）、`@/api/experiment` 的 `getStepDraft` / `saveDraft` / `submitStep`
- Produces: 路由 `/LRB`，query `{ experimentId, stepId }`

- [ ] **Step 1: 创建视图**

```vue
<template>
  <main class="resident-box-view">
    <LResidentBoxScene
      ref="sceneRef"
      :stage="stage"
      :disabled="!canInteract"
      @complete="onStageComplete"
      @operation="operationCount++"
      @error="errorCount++"
    />

    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="durationSeconds" />

    <div v-if="canInteract" class="save-bar-fixed" :class="{ saving }" @click="saveProgress" />

    <div v-if="hasMetadata" class="work-task-btn" @click="showTask = true" title="查看工作任务" />

    <!-- 缺元数据 / 恢复失败：遮罩挡住场景，避免空状态下误操作覆盖存档 -->
    <div v-if="!hasMetadata || restoreFailed" class="blocker" />

    <div v-if="!hasMetadata" class="load-panel" role="status">
      <p>缺少实验信息，请从实验选择页进入低压场景。</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </div>

    <div v-else-if="restoreFailed" class="load-panel" role="status">
      <p>场景进度恢复失败，重试成功后可继续，已有存档不会被覆盖。</p>
      <el-button type="primary" @click="handleRestore">重新加载</el-button>
    </div>

    <div v-if="finished" class="completion-banner" role="status">✓ 表箱外部操作已完成</div>

    <PromptModal :visible="showTask" @close="showTask = false">
      <article class="task-notice">
        <h2>低压场景工作任务</h2>
        <p>本次作业：集中器安装与调试、单相电能表安装。</p>
        <p>请先架设围栏并悬挂标识牌，再对居民表箱做三步式验电。</p>
      </article>
    </PromptModal>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LResidentBoxScene from '@/components/LowVoltage/LResidentBoxScene.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import PromptModal from '@/components/PromptModal.vue'
import { getStepDraft, saveDraft, submitStep } from '@/api/experiment'

const route = useRoute()
const router = useRouter()

const experimentId = typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
const stepId = typeof route.query.stepId === 'string' ? route.query.stepId : ''
const hasMetadata = !!experimentId && !!stepId

const sceneRef = ref(null)
const showTask = ref(false)
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)
const finished = ref(false)
const durationSeconds = ref(0)
const operationCount = ref(0)
const errorCount = ref(0)

let timer = null

/** 步骤映射表来自实验启动时写入 localStorage 的 steps */
const steps = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('experimentSteps_' + experimentId) || '[]')
  } catch {
    return []
  }
})

/** 当前步骤属于第几阶段：3 = 围栏挂牌，4 = 三步验电 */
const stage = computed(() => steps.value.find(s => s.stepId === stepId)?.stepOrder ?? 3)

const canInteract = computed(
  () => hasMetadata && !restoreFailed.value && !saving.value && !submitting.value && !finished.value
)

async function handleRestore() {
  if (!hasMetadata) return
  restoreFailed.value = false
  try {
    const draft = await getStepDraft(experimentId, stepId)
    if (draft && Object.keys(draft).length > 0) sceneRef.value?.restore(draft)
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  }
}

function buildPayload(status) {
  return {
    experimentId,
    stepId,
    status,
    durationSeconds: durationSeconds.value,
    operationCount: operationCount.value,
    errorCount: errorCount.value,
    ...(status === 1 ? { score: Math.max(0, 100 - errorCount.value * 10) } : {}),
    resultData: JSON.stringify({ stage: stage.value, ...(sceneRef.value?.getState() || {}) })
  }
}

async function saveProgress() {
  if (!canInteract.value) return
  saving.value = true
  try {
    await saveDraft(buildPayload(0))
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

/** 本阶段完成：提交当前步骤，步骤 3 完成后推进到步骤 4 */
async function onStageComplete() {
  if (!canInteract.value) return
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  submitting.value = true
  try {
    await submitStep(buildPayload(1))

    if (stage.value === 4) {
      finished.value = true
      ElMessage.success('表箱外部操作完成')
      return
    }

    const next = steps.value.find(s => s.stepOrder === stage.value + 1)
    if (!next) {
      ElMessage.warning('未找到下一步骤，请确认数据库已执行第 4 步脚本')
      return
    }
    ElMessage.success('围栏与标识牌已完成')
    // 同一组件内换 stepId，场景原地进入下一阶段
    await router.replace({ query: { experimentId, stepId: next.stepId } })
    await handleRestore()
    timer = setInterval(() => durationSeconds.value++, 1000)
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  handleRestore()
  timer = setInterval(() => durationSeconds.value++, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.resident-box-view {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.blocker {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(0, 0, 0, 0.35);
}

.load-panel {
  position: fixed;
  z-index: 90;
  top: 38%;
  left: 50%;
  transform: translateX(-50%);
  background: #fffdf5;
  border: 3px solid #078c83;
  border-radius: 12px;
  padding: 1.5em;
  max-width: 85vw;
  text-align: center;
  color: #123f68;
  box-shadow: 0 5px 30px #0003;
}

.load-panel p {
  margin-bottom: 1em;
}

.completion-banner {
  position: fixed;
  top: 2vh;
  left: 50%;
  transform: translateX(-50%);
  color: #08766a;
  background: #f0fff8;
  border: 2px solid #078c83;
  border-radius: 20px;
  padding: 0.4em 1em;
  z-index: 90;
  font-size: clamp(14px, 1.4vw, 22px);
}

.task-notice {
  width: min(650px, 82vw);
  max-height: 80vh;
  overflow-y: auto;
  box-sizing: border-box;
  border: 6px solid #008d85;
  border-radius: 24px;
  background: #fffdf5;
  color: #123f68;
  padding: 5% 7% 20%;
  font-size: clamp(15px, 1.4vw, 21px);
  line-height: 1.8;
}

.task-notice h2 {
  text-align: center;
  margin-bottom: 0.6em;
  font-size: 1.5em;
}

.task-notice p {
  margin-bottom: 0.8em;
}
</style>
```

- [ ] **Step 2: 路由替换**

在 `frontend/src/router/index.js` 里把

```js
  { path: '/LSO', component: () => import('@/views/LowVoltage/LSceneOverviewView.vue') }
```

改为

```js
  { path: '/LRB', component: () => import('@/views/LowVoltage/LResidentBoxView.vue') }
```

- [ ] **Step 3: 改工器具选择页的跳转目标**

在 `frontend/src/views/LowVoltage/LToolSelectionView.vue` 里把

```js
  router.push({ path: '/LSO', query: { experimentId } })
```

改为

```js
  router.push({ path: '/LRB', query: { experimentId, stepId: nextStepId.value } })
```

同时在 `onVideoEnded` 之前补一个查找第 3 步的引用（放在 `showVideo` 状态附近）：

```js
const nextStepId = ref('')
```

并在 `handleToolSelectionSubmit` 提交成功的分支里（`showVideo.value = true` 之前）补：

```js
    // 找到第 3 步的 stepId，视频播完后跳过去
    const allSteps = JSON.parse(localStorage.getItem('experimentSteps_' + experimentId) || '[]')
    nextStepId.value = allSteps.find(s => s.stepOrder === 3)?.stepId || ''
```

- [ ] **Step 4: 删除占位页**

```bash
git rm frontend/src/views/LowVoltage/LSceneOverviewView.vue
```

- [ ] **Step 5: 验证**

Run: `cd frontend && npm run build`
Expected: 构建成功；`grep -rn "LSceneOverviewView\|'/LSO'" frontend/src` 无输出

- [ ] **Step 6: 浏览器走查**

启动前后端，用学生账号启动低压实验，从 `/LTS` 提交后进入 `/LRB`，确认：
- 背景图与左侧三个工具按钮显示
- 选围栏点地面 → 左右两半围栏出现
- 选两块牌分别点悬挂区域 → 各自挂到自己的位置
- 三样放完 → URL 的 `stepId` 换成步骤 4，工具栏换成右侧验电笔

- [ ] **Step 7: 提交**

```bash
git add frontend/src/views/LowVoltage/LResidentBoxView.vue \
        frontend/src/router/index.js \
        frontend/src/views/LowVoltage/LToolSelectionView.vue
git commit -m "feat(experiment): 新增低压居民表箱场景页面并替换总览占位页"
```

---

### Task 5: 格式化与端到端验证

**Files:** 无新增

- [ ] **Step 1: 格式化**

Run: `cd frontend && npm run format`
然后 `git status --short`，**只提交本功能涉及的文件**；被 prettier 顺带改动的无关文件
（例如 `HMiddleArea.vue`、`HMeteringRoomCanvas.vue`）用 `git checkout --` 还原。

- [ ] **Step 2: 构建**

Run: `cd frontend && npm run build`
Expected: `✓ built in ...`

- [ ] **Step 3: 按 spec 第八节逐条走查**

| # | 验证项 |
| --- | --- |
| 1 | `/LTS` 提交后进入 `/LRB`，URL 带步骤 3 的 `stepId` |
| 2 | 「继续实验」→ `nextStepOrder = 3` → 进入 `/LRB` |
| 3 | 选围栏点地面 → 左右两半围栏同时出现 |
| 4 | 未选工具点场景 → 提示 + 错误次数 +1 |
| 5 | 两块牌分别挂到各自位置 |
| 6 | 三样放完 → 自动提交步骤 3 → `stepId` 换成步骤 4 |
| 7 | 点验电笔 → 点排插 → 点门把（高亮框可见）→ 再点排插 → 验电完成 |
| 8 | 跳过门把直接点排插 → 归零 + 提示 |
| 9 | 中途保存 → 刷新 → 恢复到相同阶段与进度 |
| 10 | 完成步骤 4 → 提交成功，显示「表箱外部操作已完成」 |
| 11 | 数据库里该实验步骤 3、4 的 `status = 1`，分数按公式落库 |
| 12 | 窗口缩放时场景与热区同步等比变化，不错位 |
| 13 | 只改 `resident-box-scene.js` 的坐标常量，刷新即生效 |

- [ ] **Step 4: 确认工作区干净**

Run: `git status --short`
Expected: 无输出

---

## Self-Review 记录

- **Spec 覆盖**：背景与坐标配置（Task 1）、数据库两步（Task 2）、场景组件与两阶段交互（Task 3）、
  页面编排与路由替换（Task 4）、格式化与 13 条验证（Task 5）—— spec 各节均有对应任务
- **占位符**：无 TBD；所有代码步骤都给了可直接粘贴的完整内容
- **类型一致性**：`getState()` 返回 `{ placed: boolean[3], vtStep: number, vtDone: boolean }`，
  与 `LResidentBoxView.buildPayload` 里的 `{ stage, ...getState() }` 及 `restore(state)` 的读取字段一一对应；
  配置常量名与 Task 3 组件里的 import 完全一致
