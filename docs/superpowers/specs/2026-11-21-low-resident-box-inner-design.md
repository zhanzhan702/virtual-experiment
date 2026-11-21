# 低压居民表箱内部场景（步骤 5~7） — 设计

日期：2026年11月21日
状态：待实现
依赖：低压居民表箱外部场景（`/LRB` 步骤 3/4）已完成

## 一、背景与范围

按《低压仿真操作手册》1.6 节，三步验电完成后打开柜门进入居民表箱内部。
手册列出四种表型，本期只做第一种：

> 居民表箱内部场景 1 —— 单相载波智能电表（开关内置型）

手册流程：

| 手册章节 | 内容 |
| --- | --- |
| 1.6.1 | 断电（三排空开按 1、2、3 顺序拉闸） |
| 1.6.2 | 悬挂「禁止合闸 有人工作」安全标识牌（3 号空开） |
| 1.6.3 | 挂 4 块单相智能电表 |
| 1.6.4 | 接电压、电流进出线（每表 4 根，从右往左） |
| 1.6.5 | 上电（三排空开按 3、2、1 顺序推闸） |
| 1.6.6 | 电表合盖 + 每表加 2 处铅封 |
| 1.6.7 | 回主场景，表箱门把加铅封，进入下一场景 |

### 本期做

- 手册 1.6.1 ~ 1.6.7 全部交互
- 步骤粒度为 3 个数据库步骤：断电挂牌 / 挂表接线 / 上电封盖
- 表箱内部 Leafer 画布叠加在 `/LRB` 外部场景之上
- 收尾跳转到杆上变场景占位页 `/LPT`

### 本期不做

- 手册 1.6.8 起的杆上变场景（集中器、通信模块、SIM、天线、接线盒、调试上线、清理现场）
- 其他三种表型（开关外置型 / RS485 通信）
- 1 号空开的 off 状态图（素材未完成，暂共用 on 图）

## 二、关键决定

### 2.1 并入 `/LRB`，不新建页面

Leafer 画布直接覆盖在居民表箱外部场景上，外部内容（背景、人物、排插、门把）
保持渲染、不卸载，只是被画布盖住。左右工具栏位于画布之上，始终可点。

原第 5 步占位的 `/LBI` 路由、`LResidentBoxInnerView.vue`、
`LResidentBoxInnerCanvas.vue` 全部删除。

### 2.2 步骤拆成 3 个数据库步骤

| 步骤 | step_code | 名称 | 包含手册章节 |
| --- | --- | --- | --- |
| 5 | `POWER_OFF_SIGN` | 断电挂牌 | 1.6.1 + 1.6.2 |
| 6 | `MOUNT_METER_WIRING` | 挂表接线 | 1.6.3 + 1.6.4 |
| 7 | `POWER_ON_SEAL` | 上电封盖 | 1.6.5 + 1.6.6 + 1.6.7 |

三个步骤都在 `/LRB` 内完成，由 URL `stepOrder` 与 `stepId` 驱动；
页面阶段判定优先读 `route.query.stepOrder`（与高压 `HCabinetLocalView` 一致）。

### 2.3 单画布 + 阶段驱动

与高压 `HMeteringRoomCanvas` 架构一致：一个 Leafer 画布组件
`LResidentBoxCabinetCanvas.vue` 持有全部元素，按 `stepOrder` 初始化对应图层与交互。
阶段切换时画布不销毁，只按新阶段重建交互层。

## 三、页面结构

```
/LRB 页面
├─ LResidentBoxScene（外部场景，一直渲染）
│    stage >= 5 时传 :disabled="true"，排插/门把/围栏不再响应
├─ LResidentBoxCabinetCanvas（stage 5~7 挂载）
│    .canvas-wrap { position: fixed; inset: 0; z-index: 5;
│                    display: flex; align-items: center; justify-content: center; }
│    内部 .canvas-stage 尺寸 = 8.png 实际渲染尺寸，最大 90vw × 82vh，等比
├─ ToolBarLeft / ToolBarRight（fixed，z-index: 50，位于画布之上）
├─ ExperimentTimer / 保存进度 / 查看工作任务
└─ PromptModal（工作任务、完成提示）
```

## 四、素材

### 4.1 目录与重命名

`assets/images/cabinet/room/居民表箱/` 改为 `cabinet/room/ResidentBox/`，
文件统一 PascalCase：

| 原文件 | 新文件 | images.js 导出 | 用途 |
| --- | --- | --- | --- |
| `7.jpg`（原在 `cabinet/`） | `ResidentBoxBackground.jpg` | `residentBoxBg` | 外部场景背景（引用路径同步更新） |
| `8.png` | `ResidentBoxInnerBackground.png` | `residentBoxInnerBg` | 画布背景（1352×1024） |
| `1on.png` | `Breaker1On.png` | `breaker1On` | 1 号空开（on/off 共用） |
| `2On.png` | `Breaker2On.png` | `breaker2On` | 2 号空开 on |
| `2Off.png` | `Breaker2Off.png` | `breaker2Off` | 2 号空开 off |
| `3on.png` | `Breaker3On.png` | `breaker3On` | 3 号空开 on |
| `3off.png` | `Breaker3Off.png` | `breaker3Off` | 3 号空开 off |
| `19.png` | `SinglePhaseMeterOpen.png` | `singlePhaseMeterOpen` | 场景内开盖电表 |

`assets/images/cabinet/toolbar/18.png` 改为 `toolbar/SinglePhaseMeter.png`，
导出名 `barSinglePhaseMeter`（工具栏 1:1 版）。

`11.png`、`12.png` 属于杆上变场景，随目录移入 `ResidentBox/`，本期不接入。

### 4.2 素材说明

- 导线不需要图片：起点是背景图上的线头，终点是电表端子孔，接好后用 Leafer `Path` 画线
- 标识牌沿用 `Images.barSafetyNotice`，铅封沿用 `Images.barSeal`
- 1 号空开 off 暂用 `breaker1On`，素材完成后只需改 `images.js` 一处

## 五、画布图层与元素

### 5.1 图层（从底到顶）

| 层 | 内容 |
| --- | --- |
| bgLayer | `residentBoxInnerBg` |
| breakerLayer | 9 个空开，初始全部 on |
| signLayer | 悬挂的禁止合闸标识牌 |
| meterLayer | 4 块电表（开盖/合盖两态切换） |
| wireLayer | 已接好的导线 |
| sealLayer | 电表铅封 |
| hitLayer | 全部热区 |

鼠标跟随图用 DOM 层（`.follower`），不进入画布，与高压一致。

### 5.2 空开布局

- 1 号：4 个横排，使用 `breaker1On`
- 2 号：4 个横排，使用 `breaker2On` / `breaker2Off`
- 3 号：1 个，使用 `breaker3On` / `breaker3Off`
- 从下到上依次：1 排、2 排、3 号，分布在画布左侧
- 每个空开是一个可点击图片，点击切换状态

坐标常量集中放在文件顶部的 `★★★ 坐标（相对画布 %，按需调整） ★★★` 区，
包括：9 个空开位置与宽度、标识牌位置、4 个表位、16 个线头热区、每表 4 个端子孔
（相对电表）、每表 2 个铅封位（相对电表）。

### 5.3 电表

- 4 个表位按「左上 → 右上 → 左下 → 右下」编号 1~4
- 挂表后显示 `singlePhaseMeterOpen`
- 合盖后切换为 `barSinglePhaseMeter`（1:1 工具栏图作为合盖形态）

### 5.4 导线

- 每表 4 个线头热区，16 个位置**各自独立常量**，相对画布单独调整
- 每表 4 个端子孔热区**相对电表**定位，随电表位置移动，编号从右往左 1~4
  （19.png 端子排左右各 4 个孔，编号与具体取哪 4 个以调好的热区位置为准）
- 接线完成后用 `Path` 从左下（线头）到右上（孔）画直线

线色分布（编号从左到右、从上到下）：

| 电表 | 线色（从右往左：1→4 根） |
| --- | --- |
| 1（左上） | 黑黑黄黄 |
| 2（右上） | 黑黑黄黄 |
| 3（左下） | 黑黑绿绿 |
| 4（右下） | 黑黑红红 |

即：表 1/2 为「黄黄黑黑」，表 3 为「绿绿黑黑」，表 4 为「红红黑黑」。
线色只用于视觉提示，判定只依赖线头/孔的固定对应关系。

## 六、交互与判定

### 6.1 阶段 5 · 断电挂牌

状态：`breakers = [on × 9]`、`signPlaced = false`

1. **断电**：目标顺序为「1 排 4 个全 off → 2 排 4 个全 off → 3 号 1 个 off」，
   同排内部任意顺序。
   - 点击当前排的空开：切换为 off
   - 点击非当前排的空开：提示「请按 1→2→3 顺序断电」+ 计错，状态不变
   - 点击已 off 的空开：无操作
2. **挂牌**：9 个全 off 后，左栏选中「禁止合闸」→ 点击 3 号空开 →
   标识牌出现在 3 号空开上，`signPlaced = true`
   - 点击其他位置：提示 + 计错
3. `signPlaced = true` 后提交步骤 5，进入步骤 6

### 6.2 阶段 6 · 挂表接线

状态：`meters = [未挂 × 4]`、`wires = [未接 × 16]`

1. **挂表**：右栏选「单相智能电表」→ 点击表位 → 该表位显示开盖电表
   - 4 块表顺序不限
   - 点击已挂表位：无操作
2. **接线**（每表内部严格从右往左，表与表之间顺序不限）：
   - 点击线头：必须是该表当前待接的那一根，否则提示 + 计错，不选中
   - 选中线后点击端子孔：必须是对应固定孔（右起第 n 根 → 孔右起第 n 个），
     否则提示 + 计错，取消选中
   - 正确则记录连接并画线
3. 16 根全部接好后提交步骤 6，进入步骤 7

### 6.3 阶段 7 · 上电封盖

状态：`breakers = [off × 9]`、`covers = [false × 4]`、`seals = [false × 8]`

1. **上电**：目标顺序「3 号 → 2 排 4 个 → 1 排 4 个」，同排内部任意。
   - 点击非当前排的空开：提示 + 计错，状态不变
   - 3 号排第一个空开推上时销毁标识牌（`signPlaced = false`）；
     只销毁一次，之后空开再切换也不会重新生成
2. **合盖**：逐一点击 4 块表的盖子，图片切换为合盖形态；顺序不限
3. **加铅封**：右栏选「铅封」→ 点击每块表的 2 个铅封位，共 8 处；顺序不限
   - 已加铅封的位置再点击：无操作
   - 未选铅封工具点击：提示先选工具
4. 8 处铅封完成后，画布自动卸载，回到主场景（此时不提交）
5. 主场景：右栏选「铅封」→ 点击表箱门把 → 提交步骤 7 →
   提示「低压实验完成」→ `router.push('/LPT')`

### 6.4 错序处理

所有顺序错误统一为：`ElMessage.warning` 提示 + `emit('error')` 计错一次，
**不回退**已完成的正确操作（与高压验电的「归零」不同）。

### 6.5 有效操作

每个成功的状态变化 `emit('operation')`（切换空开、挂表、接线、合盖、加铅封），
错误操作只 `emit('error')`。

## 七、数据与存档

- 画布 `getState()` 按阶段返回：
  - 阶段 5：`{ stage, breakers, signPlaced }`
  - 阶段 6：`{ stage, meters, wires }`
  - 阶段 7：`{ stage, breakers, covers, seals, boxInnerDone }`
- `boxInnerDone` 表示 8 处电表铅封已完成、画布已卸载进入门把阶段；
  恢复阶段 7 时若为 `true`，直接显示主场景（挂载门把铅封交互），不再显示画布
- `restore(state)` 在画布创建完成后按草稿重建图层与状态
- 父页面沿用现有草稿机制：`saveDraft` / `submitStep` / `getStepDraft`
- 草稿按实验 ID 存储（不按步骤区分），阶段以 URL `stepOrder` 为准

## 八、数据库

新增幂等脚本 `backend/docs/sql/11_low_resident_box_inner_steps.sql`：

1. 更新步骤 5：`BOX_INNER_OPERATION` → `POWER_OFF_SIGN` /「断电挂牌」
2. 插入步骤 6：`MOUNT_METER_WIRING` /「挂表接线」
3. 插入步骤 7：`POWER_ON_SEAL` /「上电封盖」

时长与分值（暂定，可调整）：

| 步骤 | required_seconds | score |
| --- | --- | --- |
| 5 | 120 | 15.00 |
| 6 | 300 | 30.00 |
| 7 | 180 | 20.00 |

脚本使用与现有脚本一致的子查询定位模板 ID，并保证重复执行不重复插入。

## 九、文件清单

### 新增

- `frontend/src/components/LowVoltage/LResidentBoxCabinetCanvas.vue`
- `frontend/src/views/LowVoltage/LPoleTransformerView.vue`（`/LPT` 占位页）
- `backend/docs/sql/11_low_resident_box_inner_steps.sql`
- 素材：`cabinet/room/ResidentBox/` 下 6 个文件、`cabinet/toolbar/SinglePhaseMeter.png`

### 修改

- `frontend/src/views/LowVoltage/LResidentBoxView.vue`
- `frontend/src/components/LowVoltage/LResidentBoxScene.vue`
- `frontend/src/views/ExperimentView.vue`
- `frontend/src/router/index.js`
- `frontend/src/constants/images.js`

### 删除

- `frontend/src/views/LowVoltage/LResidentBoxInnerView.vue`
- `frontend/src/components/LowVoltage/LResidentBoxInnerCanvas.vue`
- `/LBI` 路由

## 十、异常与边界

- 窗口 resize：重建 Leafer 画布并按当前状态重绘（参考高压画布）
- 草稿为空或格式异常：忽略，按初始状态渲染，不阻塞
- 阶段切换后旧状态清空，不残留
- 快速重复点击：状态机幂等，不重复计分/计数
- 步骤 7 提交后锁死，不重复提交
- 画布期间外部场景禁用；画布卸载后恢复

## 十一、验证方式

1. `npm run build`、Prettier 检查通过
2. API 提交步骤 1~4，进入步骤 5，完整操作三个阶段直到跳转 `/LPT`
3. 每个阶段保存进度后刷新，检查状态恢复
4. 逐项验证错序提示与计错（断电乱序、接线错孔、上电乱序）
5. 验证标识牌在 3 号空开推上后销毁且不再生成
