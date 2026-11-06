# 低压工器具选择页（/LTS） Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐低压实验的第 2 步「工器具选择」，并修掉低压工作票提交后不跳转的流程断点，使「工作票 → 工器具选择 → 场景总览」能被完整走通。

**Architecture:** 把高压那个完全配置驱动的 1469 行选择组件移到根级共享目录，高压/低压各用自己的配置对象驱动同一个组件。低压只新增一个配置文件和两个 View（工具选择 + 占位总览页）；高压页仅改 import 路径。

**Tech Stack:** Vue 3 (`<script setup>`) + Vue Router 4 + Element Plus + Vite。无测试框架，验证方式为构建 + 浏览器手动走查。

## Global Constraints

- 文件名规范：视图 `PascalCase` + `View` 后缀，低压场景以大写 `L` 开头；JS 工具/配置 `kebab-case`
- 组件必须使用 `<script setup>`，导入一律用 `@/` 别名，禁止相对路径
- 图片统一从 `frontend/src/constants/images.js` 导入导出，组件内用 `Images.xxx`，不得直接 import 图片文件
- 提交信息格式 `<type>(<scope>): <中文描述>`，scope 取 `frontend` / `experiment` / `config` / `component`
- 前端格式化命令：`cd frontend && npm run format`
- 不允许 `git add -A`，逐文件暂存
- 本仓库无单元测试框架，每个任务的验证步骤是 `npm run build` 通过 + 浏览器走查

---

## 文件结构

| 文件 | 职责 |
| --- | --- |
| `frontend/src/components/WizardInventorySelection.vue` | 四分类物品选择的全部交互（分页、选中、校验、错误提示），完全由 `categories` prop 驱动 |
| `frontend/src/components/VideoOverlay.vue` | 全屏视频遮罩，播毕 emit `ended` |
| `frontend/src/constants/low-voltage-tool-selection-config.js` | 低压四分类的目录与正确项 |
| `frontend/src/views/LowVoltage/LToolSelectionView.vue` | 低压工具选择页：草稿恢复、保存、计时、统计、提交、跳转 |
| `frontend/src/views/LowVoltage/LSceneOverviewView.vue` | `/LSO` 占位页 |
| `frontend/src/views/LowVoltage/LWorkTicketView.vue` | 已存在；本计划补提交后的视频与跳转 |
| `frontend/src/router/index.js` | 注册 `/LTS`、`/LSO` |
| `frontend/src/constants/images.js` | 注册三张新线材图 |
| `backend/docs/sql/04_low_experiment_templates.sql` | 权威脚本：追加第 2 步 |
| `backend/docs/sql/08_low_tool_selection_step.sql` | 增量脚本：给存量库补第 2 步（幂等） |

---

### Task 1: 素材归位与注册

**Files:**
- Rename: `frontend/src/assets/images/tool-selection/wire/4.png` → `WireGreenBlack.png`
- Rename: `frontend/src/assets/images/tool-selection/wire/30.png` → `WireRS485.png`
- Rename: `frontend/src/assets/images/cabinet/toolbar/3.png` → `WireGreenBlack.png`
- Modify: `frontend/src/constants/images.js:80-92`（选择页 wire 导入区）、`:128-142`（工具栏 bar 导入区）、`:238-250`（导出区）、`:280-292`（bar 导出区）

**Interfaces:**
- Produces: `Images.wireGreenBlack`、`Images.wireRS485`（选择页，供布局配置引用）；`Images.barWireGreenBlack`（工具栏，本轮只注册）

- [ ] **Step 1: 重命名三张数字命名的图片**

这三个文件目前是未跟踪状态，用 `mv` 而非 `git mv`：

```bash
cd /d/Code/virtual-experiment
mv frontend/src/assets/images/tool-selection/wire/4.png frontend/src/assets/images/tool-selection/wire/WireGreenBlack.png
mv frontend/src/assets/images/tool-selection/wire/30.png frontend/src/assets/images/tool-selection/wire/WireRS485.png
mv frontend/src/assets/images/cabinet/toolbar/3.png frontend/src/assets/images/cabinet/toolbar/WireGreenBlack.png
```

- [ ] **Step 2: 在选择页 wire 导入区追加两条 import**

在 `frontend/src/constants/images.js` 的 `import antenna from '@/assets/images/tool-selection/wire/Antenna.png'` 之后追加：

```js
// 低压专用线材（高压无对应素材）
import wireGreenBlack from '@/assets/images/tool-selection/wire/WireGreenBlack.png'
import wireRS485 from '@/assets/images/tool-selection/wire/WireRS485.png'
```

- [ ] **Step 3: 在工具栏 bar 导入区追加一条 import**

在 `import barSignalCable8Core from '@/assets/images/cabinet/toolbar/SignalCable8Core.png'` 之后追加：

```js
// 低压柜体工具栏版绿黑线（1:1）。本轮只注册，低压柜体场景开发时引用
import barWireGreenBlack from '@/assets/images/cabinet/toolbar/WireGreenBlack.png'
```

- [ ] **Step 4: 在默认导出对象中补上三个字段**

在 `export default {` 的 `antenna,` 之后追加两行：

```js
  wireGreenBlack,
  wireRS485,
```

在 `barSignalCable8Core,` 之后追加一行：

```js
  barWireGreenBlack,
```

- [ ] **Step 5: 验证构建通过**

Run: `cd frontend && npm run build`
Expected: `✓ built in ...`，无 `Could not resolve` 报错

- [ ] **Step 6: 提交**

```bash
git add frontend/src/assets/images/tool-selection/wire/WireGreenBlack.png \
        frontend/src/assets/images/tool-selection/wire/WireRS485.png \
        frontend/src/assets/images/cabinet/toolbar/WireGreenBlack.png \
        frontend/src/constants/images.js
git commit -m "chore(frontend): 新增低压线材图片并统一命名"
```

---

### Task 2: 抽取共享的选择组件与视频遮罩

**Files:**
- Move: `frontend/src/components/HighVoltage/HWizardInventorySelection.vue` → `frontend/src/components/WizardInventorySelection.vue`
- Move: `frontend/src/components/HighVoltage/HVideoOverlay.vue` → `frontend/src/components/VideoOverlay.vue`
- Modify: `frontend/src/views/HighVoltage/HToolSelectionView.vue:23`（模板标签）、`:33-34`（import）
- Modify: `frontend/src/views/HighVoltage/HWorkTicketView.vue:16`（模板标签）、`:29`（import）
- Modify: `frontend/src/views/HighVoltage/HCabinetLocalView.vue:68,71`（模板标签）、`:185`（import）

**Interfaces:**
- Produces: 组件 `WizardInventorySelection`，props `{ categories: Array }`，emits `finish | operation | submit-error`，`defineExpose({ selectedMap })`
- Produces: 组件 `VideoOverlay`，props `{ visible: Boolean, src: String }`，emits `ended`

- [ ] **Step 1: 移动文件（两个文件都是已跟踪的，用 git mv）**

```bash
cd /d/Code/virtual-experiment
git mv frontend/src/components/HighVoltage/HWizardInventorySelection.vue frontend/src/components/WizardInventorySelection.vue
git mv frontend/src/components/HighVoltage/HVideoOverlay.vue frontend/src/components/VideoOverlay.vue
```

- [ ] **Step 2: 更新 HToolSelectionView.vue 的 import**

把：

```js
import WizardInventorySelection from '@/components/HighVoltage/HWizardInventorySelection.vue'
import HVideoOverlay from '@/components/HighVoltage/HVideoOverlay.vue'
```

改为：

```js
import WizardInventorySelection from '@/components/WizardInventorySelection.vue'
import VideoOverlay from '@/components/VideoOverlay.vue'
```

- [ ] **Step 3: 更新 HToolSelectionView.vue 模板里的组件标签**

把 `<HVideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />`
改为 `<VideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />`

（`<WizardInventorySelection ...>` 原本就是这个标签名，无需改）

- [ ] **Step 4: 更新 HWorkTicketView.vue 的 import 与标签**

import 路径改为：

```js
import VideoOverlay from '@/components/VideoOverlay.vue'
```

模板里把 `<HVideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />`
改为 `<VideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />`

- [ ] **Step 4b: 更新 HCabinetLocalView.vue（/HCL 页面同样用到这个遮罩）**

`:185` 的 import 改为：

```js
import VideoOverlay from '@/components/VideoOverlay.vue'
```

模板里两处标签改名（第 68 行是单行，第 71 行是多行，只改开标签即可）：

```html
    <VideoOverlay :visible="showFenceVideo" :src="Videos.testVideo" @ended="onFenceVideoEnded" />
```

```html
    <VideoOverlay
      :key="videoSeq"
      :visible="showPowerVideo"
      :src="Videos.testVideo"
      @ended="onPowerVideoEnded"
    />
```

- [ ] **Step 5: 确认没有残留引用**

Run: `grep -rn "HWizardInventorySelection\|HVideoOverlay" frontend/src`
Expected: 无输出

- [ ] **Step 6: 验证高压页面回归**

Run: `cd frontend && npm run build`
Expected: 构建成功

浏览器打开 `/HTS`（带 `?experimentId=...&stepId=...`），确认：
四个 Tab 正常渲染、物品卡片可点选、点「提交」时未选齐会弹出错误提示

`/HCL` 页面也引用了视频遮罩，同样需要确认能正常打开（画布渲染、工具栏出现）

- [ ] **Step 7: 提交**

```bash
git add frontend/src/components/WizardInventorySelection.vue \
        frontend/src/components/VideoOverlay.vue \
        frontend/src/views/HighVoltage/HToolSelectionView.vue \
        frontend/src/views/HighVoltage/HWorkTicketView.vue \
        frontend/src/views/HighVoltage/HCabinetLocalView.vue
git commit -m "refactor(component): 抽取工器具选择与视频遮罩为共享组件"
```

---

### Task 3: 低压工器具选择配置

**Files:**
- Create: `frontend/src/constants/low-voltage-tool-selection-config.js`
- Read: `frontend/src/constants/tool-selection-config.js`（复制个人防护/终端/工器具的 tools 数组）

**Interfaces:**
- Produces: `export const categories` —— 四元素数组，元素形如
  `{ key: string, title: string, icon: string, requiredCount: null, multi?: true, correctIds: number[], tools: Array<{ id: number, name: string, slot: string, icon: string, image: string }> }`

- [ ] **Step 1: 创建配置文件**

完整内容如下。个人防护、终端、工器具的 `tools` 数组与高压逐字相同（直接复制），
只把 `correctIds` 改成低压取值；`requiredCount` 一律留空，由 `correctIds.length` 动态推导。

```js
/**
 * 低压工器具选择配置。
 *
 * 四个分类里只有「线材」与高压不同：低压用 4mm²绿黑线、0.5mm²遥控线、0.5mm²遥信线、
 * RS485 线替换了高压的 6 芯 / 8 芯信号线。个人防护、终端、工器具的目录与正确项
 * 与高压完全一致（correctIds 也相同）。
 *
 * requiredCount 一律留空：组件用 `requiredCount ?? correctIds.length` 推导应选数量，
 * 这样只需维护 correctIds 一处，不会出现「应选 5 个但只标了 4 个正确项」的矛盾。
 *
 * 正确项来源：仿真操作手册「个人防护、终端、工器具、线材选择界面」一节的标准答案。
 */

const ppe = [
  { id: 1, name: '工作服', slot: 'body', icon: '👔', image: 'workClothes' },
  { id: 2, name: 'T恤和短裤', slot: 'body', icon: '👕', image: 'tShirtAndShorts' },
  { id: 3, name: '工作服上装和牛仔裤', slot: 'body', icon: '👖', image: 'workClothesTopAndJeans' },
  { id: 4, name: '红色安全帽', slot: 'head', icon: '⛑️', image: 'redSafetyHelmet' },
  { id: 5, name: '蓝色安全帽', slot: 'head', icon: '🪖', image: 'blueSafetyHelmet' },
  { id: 6, name: '摩托车帽', slot: 'head', icon: '🪖', image: 'motorcycleHelmet' },
  { id: 7, name: '护目镜', slot: 'eye', icon: '🥽', image: 'safetyGoggles' },
  { id: 8, name: '橡胶手套', slot: 'hand', icon: '🧤', image: 'rubberGloves' },
  { id: 9, name: '纱手套', slot: 'hand', icon: '🧤', image: 'cottonGloves' },
  { id: 10, name: '电力绝缘鞋', slot: 'foot', icon: '🥾', image: 'electricalInsulatingShoes' },
  { id: 11, name: '凉鞋', slot: 'foot', icon: '🩴', image: 'sandal' },
  { id: 12, name: '皮鞋', slot: 'foot', icon: '👞', image: 'leatherShoes' }
]

const terminal = [
  { id: 21, name: '三相三线专变终端', slot: 'device', icon: '📟', image: 'threePhaseThreeWireTerminal' },
  { id: 22, name: '三相四线专变终端', slot: 'device', icon: '📟', image: 'threePhaseFourWireTerminal' },
  { id: 23, name: '230M终端', slot: 'device', icon: '📡', image: 'terminal230M' },
  { id: 24, name: '集中器', slot: 'device', icon: '🖥️', image: 'concentrator' },
  { id: 25, name: '采集器', slot: 'device', icon: '📷', image: 'collector' }
]

const tool = [
  { id: 31, name: '一字螺丝刀', slot: 'tool', icon: '🪛', image: 'flatheadScrewdriver' },
  { id: 32, name: '十字螺丝刀', slot: 'tool', icon: '🪛', image: 'crossScrewdriver' },
  { id: 33, name: '活动扳手', slot: 'tool', icon: '🔧', image: 'adjustableWrench' },
  { id: 34, name: '尖嘴钳', slot: 'tool', icon: '🔧', image: 'needleNosePliers' },
  { id: 35, name: '老虎钳', slot: 'tool', icon: '🔧', image: 'slipJointPliers' },
  { id: 36, name: '剥线钳', slot: 'tool', icon: '🔧', image: 'wireStripper' },
  { id: 37, name: '验电笔', slot: 'tool', icon: '🖊️', image: 'voltageTester' },
  { id: 38, name: '铅封', slot: 'tool', icon: '🔒', image: 'seal' }
]

// 顺序按原网页截图排列；4.0mm² 无独立素材，复用 2.5mm² 图（与高压惯例一致）
const wire = [
  { id: 51, name: '2.5MM²黄色导线', slot: 'wire', icon: '🟡', image: 'wireYellow' },
  { id: 52, name: '2.5MM²黄黑色导线', slot: 'wire', icon: '🟡⚫', image: 'wireYellowBlack' },
  { id: 53, name: '2.5MM²绿色导线', slot: 'wire', icon: '🟢', image: 'wireGreen' },
  { id: 54, name: '2.5MM²红色导线', slot: 'wire', icon: '🔴', image: 'wireRed' },
  { id: 55, name: '2.5MM²红黑色导线', slot: 'wire', icon: '🔴⚫', image: 'wireRedBlack' },
  { id: 56, name: '2.5MM²黑色导线', slot: 'wire', icon: '⚫', image: 'wire25mmBlack' },

  { id: 57, name: '4.0MM²黄色导线', slot: 'wire', icon: '🟡', image: 'wireYellow' },
  { id: 58, name: '4.0MM²黄黑色导线', slot: 'wire', icon: '🟡⚫', image: 'wireYellowBlack' },
  { id: 59, name: '4.0MM²绿色导线', slot: 'wire', icon: '🟢', image: 'wireGreen' },
  { id: 60, name: '4.0MM²红色导线', slot: 'wire', icon: '🔴', image: 'wireRed' },
  { id: 61, name: '4.0MM²红黑色导线', slot: 'wire', icon: '🔴⚫', image: 'wireRedBlack' },
  { id: 62, name: '扎带', slot: 'wire', icon: '🏷️', image: 'cableTieLabel' },

  { id: 63, name: '4.0MM²绿黑色导线', slot: 'wire', icon: '🟢⚫', image: 'wireGreenBlack' },
  { id: 64, name: '0.5MM²遥控线', slot: 'wire', icon: '🔌', image: 'wire2Core' },
  { id: 65, name: '0.5MM²遥信线', slot: 'wire', icon: '🔌', image: 'wire2Core' },
  { id: 66, name: 'RS485线', slot: 'wire', icon: '🔌', image: 'wireRS485' },
  { id: 67, name: '通信模块', slot: 'wire', icon: '📶', image: 'communicationModule' },
  { id: 68, name: 'SIM卡', slot: 'wire', icon: '💳', image: 'simCard' },
  { id: 69, name: '天线', slot: 'wire', icon: '📡', image: 'antenna' }
]

export const categories = [
  {
    key: 'ppe',
    title: '个人防护',
    icon: 'UserFilled',
    requiredCount: null,
    correctIds: [1, 5, 7, 9, 10],
    tools: ppe
  },
  {
    key: 'terminal',
    title: '终端',
    icon: 'Monitor',
    requiredCount: null,
    correctIds: [24],
    tools: terminal
  },
  {
    key: 'tool',
    title: '工器具',
    icon: 'SetUp',
    requiredCount: null,
    correctIds: [32, 36, 37, 38],
    tools: tool
  },
  {
    key: 'wire',
    title: '线材',
    icon: 'Connection',
    requiredCount: null,
    multi: true,
    // 正确 13 项：黄/绿/红/黑各规格 10 根 + 通信模块 + SIM卡 + 天线
    // 错误 6 项：2.5mm²黄黑、2.5mm²红黑、扎带、遥控线、遥信线、RS485线
    correctIds: [51, 53, 54, 56, 57, 58, 59, 60, 61, 63, 67, 68, 69],
    tools: wire
  }
]
```

- [ ] **Step 2: 验证配置可被解析且四项数量正确**

Run: `cd frontend && npm run build`
Expected: 构建成功

再用 Node 快速核对四个分类的数量与正确项数量：

```bash
cd frontend && node --input-type=module -e "
import('./src/constants/low-voltage-tool-selection-config.js').then(m => {
  for (const c of m.categories) {
    console.log(c.key, 'items=' + c.tools.length, 'correct=' + c.correctIds.length)
  }
})"
```

Expected 输出：
```
ppe items=12 correct=5
terminal items=5 correct=1
tool items=8 correct=4
wire items=19 correct=13
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/constants/low-voltage-tool-selection-config.js
git commit -m "feat(config): 新增低压工器具选择分类配置"
```

---

### Task 4: 数据库补低压第 2 步

**Files:**
- Modify: `backend/docs/sql/04_low_experiment_templates.sql:13-15`
- Create: `backend/docs/sql/08_low_tool_selection_step.sql`

**Interfaces:**
- Produces: `experiment_steps` 中 `template_id = 0x...32` 且 `step_order = 2` 的一行，`step_code = 'SELECT_TOOLS'`

- [ ] **Step 1: 在权威脚本末尾追加第 2 步**

把 `backend/docs/sql/04_low_experiment_templates.sql` 结尾那两行

```sql
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 1, 'FILL_TICKET', '填写工作票', 300, 25.00);
```

改为（注意把分号换成逗号，并补上第 2 步）：

```sql
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 1, 'FILL_TICKET', '填写工作票', 300, 25.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00);
```

同时在文件顶部注释的 uuid 分配表里补充一行说明：

```sql
--   32 低压训练场景V1（LV_TRAIN_V1）：步骤 1 填写工作票 / 步骤 2 工器具选择
```

- [ ] **Step 2: 创建幂等增量脚本**

```sql
-- V8 低压场景补第 2 步：工器具选择
--
-- 用途：已按旧版 04 脚本建过库的环境，执行本脚本补齐第 2 步。
-- 幂等：已存在 step_order = 2 时不会重复插入，可重复执行。
--
-- 分值 180 秒 / 30 分为暂定，与高压第 2 步同值；模板总分变为 55 分，
-- 成绩按「原始分 ÷ 步骤分合计 × 100」换算，百分比口径不受影响。

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()),
       0x00000000000000000000000000000032,
       2,
       'SELECT_TOOLS',
       '工器具选择',
       180,
       30.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM experiment_steps
  WHERE template_id = 0x00000000000000000000000000000032
    AND step_order = 2
);
```

- [ ] **Step 3: 在数据库上执行并核对**

Run: `mysql -u root -p"$MYSQL_PASSWORD" virtual_experiment < backend/docs/sql/08_low_tool_selection_step.sql`

再查一次确认只有一行且顺序正确：

```bash
mysql -u root -p"$MYSQL_PASSWORD" virtual_experiment -e "
SELECT step_order, step_code, step_name, required_seconds, score
FROM experiment_steps WHERE template_id = 0x00000000000000000000000000000032 ORDER BY step_order;"
```

Expected:
```
1  FILL_TICKET   填写工作票   300  25.00
2  SELECT_TOOLS  工器具选择   180  30.00
```

- [ ] **Step 4: 提交**

```bash
git add backend/docs/sql/04_low_experiment_templates.sql backend/docs/sql/08_low_tool_selection_step.sql
git commit -m "feat(experiment): 低压模板新增工器具选择步骤"
```

---

### Task 5: 低压工器具选择页与路由

**Files:**
- Create: `frontend/src/views/LowVoltage/LToolSelectionView.vue`
- Modify: `frontend/src/router/index.js:45`

**Interfaces:**
- Consumes: `WizardInventorySelection`（Task 2 产出）、`VideoOverlay`（Task 2）、`categories`（Task 3）
- Consumes: `getStepDraft(experimentId, stepId)`、`saveDraft(payload)`、`submitStep(payload)` from `@/api/experiment`
- Produces: 路由 `/LTS`，query `{ experimentId, stepId }`

- [ ] **Step 1: 创建视图**

```vue
<template>
  <div class="tool-selection-view">
    <WizardInventorySelection
      ref="wizardRef"
      :categories="categories"
      @finish="handleToolSelectionSubmit"
      @operation="handleOperation"
      @submit-error="handleSubmitError"
    />

    <ExperimentTimer :experiment-id="experimentId" :current-step-seconds="stats.duration_seconds" />
    <div
      v-if="canWrite"
      class="save-bar-fixed"
      :class="{ saving }"
      @click="saveProgress"
      title="保存进度"
    />

    <div v-if="hasMetadata" class="work-task-btn" @click="showTask = true" title="查看工作任务" />

    <!-- 缺少实验信息 -->
    <div v-if="!hasMetadata" class="load-panel" role="status">
      <p>缺少实验信息，请从实验选择页进入低压场景。</p>
      <el-button type="primary" @click="router.push('/experiment')">返回实验选择</el-button>
    </div>

    <!-- 恢复失败：显式提示，避免存档丢失变成无从排查的问题 -->
    <div v-else-if="restoreFailed" class="load-panel" role="status">
      <p>工器具选择进度恢复失败，重试成功后可继续，已有存档不会被覆盖。</p>
      <el-button type="primary" @click="handleRestore">重新加载</el-button>
    </div>

    <PromptModal :visible="showTask" @close="showTask = false">
      <article class="task-notice">
        <h2>低压场景工器具选择</h2>
        <p>本次作业：集中器安装与调试、单相电能表安装。</p>
        <p>请在「个人防护 / 终端 / 工器具 / 线材」四个分类中选齐所需物品。</p>
        <p>线材需区分规格与线型，多选或漏选都无法进入下一步。</p>
      </article>
    </PromptModal>

    <VideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PromptModal from '@/components/PromptModal.vue'
import ExperimentTimer from '@/components/ExperimentTimer.vue'
import WizardInventorySelection from '@/components/WizardInventorySelection.vue'
import VideoOverlay from '@/components/VideoOverlay.vue'
import { categories } from '@/constants/low-voltage-tool-selection-config'
import Videos from '@/constants/videos'
import { submitStep, saveDraft, getStepDraft } from '@/api/experiment'
import { formatLocalTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()

const wizardRef = ref(null)
const showTask = ref(false)
const showVideo = ref(false)

const experimentId = typeof route.query.experimentId === 'string' ? route.query.experimentId : ''
const stepId = typeof route.query.stepId === 'string' ? route.query.stepId : ''
const hasMetadata = !!experimentId && !!stepId

const startedAt = ref(formatLocalTime(new Date()))
const saving = ref(false)
const submitting = ref(false)
const restoreFailed = ref(false)

const canWrite = computed(
  () => hasMetadata && !restoreFailed.value && !saving.value && !submitting.value
)

const stats = reactive({ duration_seconds: 0, operation_count: 0, error_count: 0 })
let timer = null

/** 恢复草稿：失败时置 restoreFailed，页面给出重试入口而不是静默吞掉 */
async function handleRestore() {
  if (!hasMetadata) return
  restoreFailed.value = false
  try {
    const draft = await getStepDraft(experimentId, stepId)
    if (draft && Object.keys(draft).length > 0 && wizardRef.value) {
      Object.assign(wizardRef.value.selectedMap, draft)
    }
  } catch (err) {
    restoreFailed.value = true
    ElMessage.error('恢复失败：' + (err.response?.data?.message || err.message))
  }
}

/** 保存进度（全量选择数据） */
async function saveProgress() {
  if (!canWrite.value) return
  saving.value = true
  try {
    const fullData = wizardRef.value
      ? JSON.parse(JSON.stringify(wizardRef.value.selectedMap))
      : {}
    await saveDraft({
      experimentId,
      stepId,
      status: 0,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      resultData: JSON.stringify(fullData),
      startedAt: startedAt.value
    })
    ElMessage.success('进度已保存')
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

const handleOperation = () => {
  stats.operation_count++
}

const handleSubmitError = errorPageCount => {
  stats.error_count += errorPageCount
}

async function handleToolSelectionSubmit(selectedMap) {
  // 提交时冻结计时器，避免等待跳转期间 stats 继续增长导致显示与提交值不一致
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  const score = Math.max(0, 100.0 - stats.error_count * 10)
  submitting.value = true
  try {
    await submitStep({
      experimentId,
      stepId,
      status: 1,
      durationSeconds: stats.duration_seconds,
      operationCount: stats.operation_count,
      errorCount: stats.error_count,
      score,
      resultData: JSON.stringify(selectedMap),
      startedAt: startedAt.value
    })
    ElMessage.success('工器具选择已完成，即将播放教学视频…')
    showVideo.value = true
  } catch (err) {
    ElMessage.error('提交失败：' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

/** 视频播放完毕 → 进入低压场景总览（占位页） */
function onVideoEnded() {
  showVideo.value = false
  router.push({ path: '/LSO', query: { experimentId } })
}

onMounted(() => {
  handleRestore()
  timer = setInterval(() => {
    stats.duration_seconds++
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.tool-selection-view {
  position: relative;
  padding: 28px;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: linear-gradient(180deg, rgba(240, 245, 255, 0.82) 0%, rgba(245, 247, 250, 0.82) 100%);
}

.tool-selection-view::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: var(--img-tool-selection-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
}

/* 内容元素盖在背景之上；固定定位按钮（保存进度/查看工作任务）除外，保持 main.css 的 fixed 定位 */
.tool-selection-view > :not(.save-bar-fixed):not(.work-task-btn) {
  position: relative;
  z-index: 1;
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

- [ ] **Step 2: 注册 /LTS 路由**

在 `frontend/src/router/index.js` 的 `/LWT` 一行之后追加：

```js
  { path: '/LTS', component: () => import('@/views/LowVoltage/LToolSelectionView.vue') },
```

- [ ] **Step 3: 验证构建与页面渲染**

Run: `cd frontend && npm run build`
Expected: 构建成功

浏览器打开 `/LTS?experimentId=<任一实验ID>&stepId=<第2步ID>`，确认：
- 四个 Tab 分别显示 `0/5`、`0/1`、`0/4`、`0/13`
- 切到「线材」Tab 共 19 个物品，分 3 页（每页 9 个）
- 点物品可选中/取消，底部「已选物品」栏跟随变化

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/LowVoltage/LToolSelectionView.vue frontend/src/router/index.js
git commit -m "feat(experiment): 新增低压工器具选择页面"
```

---

### Task 6: /LSO 占位页与路由

**Files:**
- Create: `frontend/src/views/LowVoltage/LSceneOverviewView.vue`
- Modify: `frontend/src/router/index.js`（在 `/LTS` 之后）

**Interfaces:**
- Produces: 路由 `/LSO`，query `{ experimentId }`

- [ ] **Step 1: 创建占位页**

```vue
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
```

- [ ] **Step 2: 注册 /LSO 路由**

在 `frontend/src/router/index.js` 的 `/LTS` 一行之后追加：

```js
  { path: '/LSO', component: () => import('@/views/LowVoltage/LSceneOverviewView.vue') },
```

- [ ] **Step 3: 验证**

Run: `cd frontend && npm run build`
Expected: 构建成功

浏览器打开 `/LSO?experimentId=abc`，确认页面显示「低压场景总览」「实验 ID：abc」，
点「返回实验选择」能跳到 `/experiment`

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/LowVoltage/LSceneOverviewView.vue frontend/src/router/index.js
git commit -m "feat(experiment): 新增低压场景总览占位页"
```

---

### Task 7: 低压工作票提交后进入第 2 步

**Files:**
- Modify: `frontend/src/views/LowVoltage/LWorkTicketView.vue`（template 与 script 两处）

**Interfaces:**
- Consumes: `VideoOverlay`（Task 2）、路由 `/LTS`（Task 5）、`experiment_steps` 第 2 步（Task 4）

- [ ] **Step 1: 在模板末尾加视频遮罩**

在 `</main>` 之前、`<PromptModal ...>...</PromptModal>` 之后追加：

```html
    <!-- 视频1：工作票提交完（播放完毕自动进入工器具选择） -->
    <VideoOverlay :visible="showVideo" :src="Videos.testVideo" @ended="onVideoEnded" />
```

- [ ] **Step 2: 补 import**

在 `import PromptModal from '@/components/PromptModal.vue'` 之后追加两行：

```js
import VideoOverlay from '@/components/VideoOverlay.vue'
import Videos from '@/constants/videos'
```

- [ ] **Step 3: 增加两个状态**

在 `const showTask = ref(false)` 之后追加：

```js
// 视频1：工作票提交完播放（播毕进入工器具选择）
const showVideo = ref(false)
const nextStepId = ref('')
```

- [ ] **Step 4: 改提交成功分支**

把 `handleTicketSubmit` 里的

```js
    await submitStep(createPayload(result.data, true))
    persistedStats = { ...result.data.stats }
    completed.value = true
    ElMessage.success('两张工作票已完成')
```

改为：

```js
    await submitStep(createPayload(result.data, true))
    persistedStats = { ...result.data.stats }
    completed.value = true

    // 查找第 2 步。找不到时停留并提示，而不是带着空 stepId 跳过去 ——
    // 工具选择页拿不到有效 stepId 就既存不了档也提交不了
    const steps = JSON.parse(localStorage.getItem('experimentSteps_' + experimentId) || '[]')
    const nextStep = steps.find(s => s.stepOrder === 2)
    if (!nextStep) {
      ElMessage.warning('未找到下一步骤，请确认数据库已执行低压第 2 步脚本')
      return
    }
    nextStepId.value = nextStep.stepId
    ElMessage.success('两张工作票已完成，即将播放教学视频…')
    showVideo.value = true
```

- [ ] **Step 5: 增加视频播毕回调**

在 `onMounted(handleRestore)` 之前追加：

```js
/** 视频1 播放完毕 → 进入工器具选择（步骤2） */
function onVideoEnded() {
  showVideo.value = false
  router.push({
    path: '/LTS',
    query: { experimentId, stepId: nextStepId.value }
  })
}
```

- [ ] **Step 6: 验证构建**

Run: `cd frontend && npm run build`
Expected: 构建成功

- [ ] **Step 7: 端到端走查**

1. 以学生身份登录，进入 `/experiment`，启动低压训练场景
2. 在第 1 步填完两张工作票并提交 → 播放视频 → 自动进入 `/LTS`，地址栏 `stepId` 非空
3. 回到 `/experiment`，点「继续实验」→ 应直接进入 `/LTS`（`nextStepOrder = 2`）
4. 在 `/LTS` 选齐四项 → 提交 → 播放视频 → 跳到 `/LSO`
5. 到「查看学生成绩」确认该实验的两步都有记录

- [ ] **Step 8: 提交**

```bash
git add frontend/src/views/LowVoltage/LWorkTicketView.vue
git commit -m "feat(experiment): 低压工作票提交后进入工器具选择"
```

---

### Task 8: 格式化与收尾验证

**Files:** 无新增，只格式化与全量走查

- [ ] **Step 1: 格式化前端**

Run: `cd frontend && npm run format`
Expected: 只输出被改写的文件，无报错

- [ ] **Step 2: 全量构建**

Run: `cd frontend && npm run build`
Expected: `✓ built in ...`

- [ ] **Step 3: 按 spec 第七节逐条走查**

| # | 验证项 |
| --- | --- |
| 1 | 低压实验启动 → 第 1 步完成自动播视频 → 进入 `/LTS` |
| 2 | 「继续实验」→ `nextStepOrder = 2` → 进入 `/LTS` |
| 3 | 四个 Tab 数量 12 / 5 / 8 / 19 |
| 4 | 只选线材正确 13 项 → 通过；多选 RS485/扎带/遥控线 → 提示错误 |
| 5 | 个人防护少选一项 → 不通过；选齐 5 项 → 通过 |
| 6 | 保存进度 → 刷新 → 选择被完整恢复 |
| 7 | 存档异常时展示失败面板，可点「重新加载」重试 |
| 8 | 提交成功 → 播视频 → 跳转 `/LSO` |
| 9 | 该步骤分数按 `100 - 错误页数 × 10` 落库 |
| 10 | 高压 `/HTS`、`/HCL` 页面功能不变（回归；`/HCL` 依赖被移动的视频遮罩，Task 2 只做了构建验证，页面级回归放到这里） |

- [ ] **Step 4: 确认工作区干净**

Run: `git status --short`
Expected: 无输出

---

## Self-Review 记录

- **Spec 覆盖**：素材命名（Task 1）、共享组件（Task 2）、线材配置与四项目录（Task 3）、
  数据库第 2 步（Task 4）、`/LTS` 页面（Task 5）、`/LSO` 占位（Task 6）、工作票跳转（Task 7）、
  格式化与 10 条验证（Task 8）—— spec 各节均有对应任务
- **占位符**：无 TBD / TODO；所有代码步骤都给出了可直接粘贴的完整内容
- **类型一致性**：`categories[].correctIds` 为 `number[]`、`tools[].id` 为 `number`，
  与 `WizardInventorySelection` 内部 `selected.includes(toolId)` 的用法一致；
  图片字段名（`wireGreenBlack`、`wireRS485`）与 Task 1 在 `images.js` 注册的导出一致
