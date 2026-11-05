# 低压工器具选择（/LTS） — 设计

日期：2026年11月05日
状态：待实现
依赖：低压工作票页（`LWorkTicketView.vue`）已完成

## 一、背景与范围

高压场景已有完整的 24 步流程（工作票 → 工器具选择 → 柜体局部操作），低压场景目前**只有第 1 步**：
`LV_TRAIN_V1` 模板里只定义了 `FILL_TICKET` 一条步骤，工作票页做完就断了。

`ExperimentView.vue` 的低压 `stepRouteMap` 已经预留了 `{ 1: '/LWT', 2: '/LTS' }`，
但路由表里没有 `/LTS`，数据库里也没有第 2 步，所以这条映射从未生效。

本期补齐第 2 步「工器具选择」，让低压流程能走到「工作票 → 工器具选择」两段。

### 本期做

- 低压工器具选择页 `/LTS`
- `LV_TRAIN_V1` 模板新增第 2 步 `SELECT_TOOLS`
- 低压工作票提交后自动播放视频并进入第 2 步（补上现有的流程断点）
- `/LSO` 占位页（第 3 步尚未开发，先把跳转目标建出来）

### 本期不做

- 低压场景总览（`/LSO`）的实际内容
- 低压柜体局部操作
- 低压专用的背景图、教学视频（暂时复用高压/通用素材）

## 二、与高压的差异

**四个分类里只有「线材」不同。** 个人防护、终端、工器具的目录与正确项与高压完全一致：

| 分类 | 目录 | 正确项 | 与高压 |
| --- | --- | --- | --- |
| 个人防护 | 12 项 | 工作服、蓝色安全帽、护目镜、纱手套、电力绝缘鞋 | **相同** |
| 终端 | 5 项 | 集中器 | **相同** |
| 工器具 | 8 项 | 十字螺丝刀、剥线钳、验电笔、铅封 | **相同** |
| 线材 | 19 项 | 13 项（见 4.1） | 不同 |

线材差异：低压用「4mm²绿黑线 + 0.5mm²遥控线 + 0.5mm²遥信线 + RS485 线」替换了高压的「6 芯 / 8 芯信号线」。

### 正确项来源

来自 `仿真操作手册.doc` 第 23-1 节「个人防护、终端、工器具、线材选择界面」的标准答案：

> 个人防护必选：工作服上装、下装、安全帽（蓝色）、纱手套、电力绝缘鞋。
> 终端必选：集中器。
> 工器具必选：十字螺丝刀、剥线钳、验电笔、铅封。
> 线材必选：2.5mm²黄色导线、4mm²黄色导线、4mm²黄黑色导线、2.5mm²绿色导线、4mm²绿色导线、
> 4mm²绿黑色导线、2.5mm²红色导线、4mm²红色导线、4mm²红黑色导线、2.5mm²黑色导线、
> 通信模块、SIM 卡、天线。

手册把「工作服」拆成上下装两件，但高压的分类里「工作服」是整件。
**决定沿用高压的 12 件目录**，把上下装视为已由「工作服」体现，因此个人防护的正确项与高压相同。

## 三、三项关键决定

### 3.1 抽取共享组件，而不是复制

`HWizardInventorySelection.vue` 有 1469 行，但确认是**完全配置驱动**的：
只通过 `props.categories` 与 `Images[tool.image]` 工作，没有任何写死的高压内容
（无「高压」文案、无路由、无视频）。

因此把它移到根级共享目录 `components/WizardInventorySelection.vue`，两侧共用。
高压页只改一行 import 路径，业务逻辑零改动。

复制一份会让 1469 行分叉，以后调整交互必须同步两处，得不偿失。

### 3.2 线材只提供有序列表，布局仍动态计算

物品网格由 `currentPageTools` + `totalPages` 分页渲染，`getRequiredCount` 取
`requiredCount ?? correctIds.length`。因此配置里**不做行分组**，只给一个按截图顺序排列的
扁平数组；每页显示几个、总共几页，仍由组件现算。

四项分类的 `requiredCount` 一律留空，显示数量由 `correctIds.length` 推导。

### 3.3 补上工作票的流程断点

高压 `HWorkTicketView` 提交成功后：播放视频 1 → 从 `localStorage` 的
`experimentSteps_<id>` 里找 `stepOrder === 2` 的 `stepId` → 跳 `/HTS`。

低压 `LWorkTicketView` 目前提交后只置 `completed = true`，**不跳转**，学生会卡在第 1 步。
本期照抄高压那一段，目标路径改为 `/LTS`。

找不到第 2 步时（例如数据库还没执行增量脚本）**不跳转**，给出提示并停留在当前页 ——
否则会用空 `stepId` 跳过去，工具选择页既无法存档也无法提交。

## 四、前端设计

### 4.1 线材配置（19 项，正确 13 项）

按截图的顺序排列：

| 序号 | 名称 | id | 正确 |
| --- | --- | --- | --- |
| 1 | 2.5mm²黄色导线 | 51 | ✓ |
| 2 | 2.5mm²黄黑色导线 | 52 | |
| 3 | 2.5mm²绿色导线 | 53 | ✓ |
| 4 | 2.5mm²红色导线 | 54 | ✓ |
| 5 | 2.5mm²红黑色导线 | 55 | |
| 6 | 2.5mm²黑色导线 | 56 | ✓ |
| 7 | 4.0mm²黄色导线 | 57 | ✓ |
| 8 | 4.0mm²黄黑色导线 | 58 | ✓ |
| 9 | 4.0mm²绿色导线 | 59 | ✓ |
| 10 | 4.0mm²红色导线 | 60 | ✓ |
| 11 | 4.0mm²红黑色导线 | 61 | ✓ |
| 12 | 扎带 | 62 | |
| 13 | 4.0mm²绿黑色导线 | 63 | ✓ |
| 14 | 0.5mm²遥控线 | 64 | |
| 15 | 0.5mm²遥信线 | 65 | |
| 16 | RS485 线 | 66 | |
| 17 | 通信模块 | 67 | ✓ |
| 18 | SIM 卡 | 68 | ✓ |
| 19 | 天线 | 69 | ✓ |

`correctIds = [51, 53, 54, 56, 57, 58, 59, 60, 61, 63, 67, 68, 69]`

个人防护、终端、工器具直接复用 `constants/tool-selection-config.js` 的 `tools` 数组与 id，
只把 `correctIds` 改为低压的取值。

### 4.2 文件结构

**新增**

| 文件 | 说明 |
| --- | --- |
| `components/WizardInventorySelection.vue` | 从 `components/HighVoltage/HWizardInventorySelection.vue` 移动，内容不变 |
| `components/VideoOverlay.vue` | 从 `components/HighVoltage/HVideoOverlay.vue` 移动，内容不变 |
| `constants/low-voltage-tool-selection-config.js` | 低压四分类配置 |
| `views/LowVoltage/LToolSelectionView.vue` | 低压工器具选择页 |
| `views/LowVoltage/LSceneOverviewView.vue` | `/LSO` 占位页 |

**修改**

| 文件 | 改动 |
| --- | --- |
| `views/HighVoltage/HToolSelectionView.vue` | 两个 import 路径 |
| `views/HighVoltage/HWorkTicketView.vue` | `HVideoOverlay` import 路径 |
| `views/LowVoltage/LWorkTicketView.vue` | 增加视频 1 与跳转 `/LTS` |
| `router/index.js` | 注册 `/LTS`、`/LSO` |
| `constants/images.js` | 新增 `wireGreenBlack`、`wireRS485`（选择页）与 `barWireGreenBlack`（工具栏，本轮只注册不使用） |
| `backend/docs/sql/04_low_experiment_templates.sql` | 追加第 2 步（权威脚本） |

**删除**

| 文件 | 说明 |
| --- | --- |
| `components/HighVoltage/HWizardInventorySelection.vue` | 内容已移到共享目录 |
| `components/HighVoltage/HVideoOverlay.vue` | 同上 |

### 4.3 视图行为

`LToolSelectionView.vue` 照抄 `HToolSelectionView.vue` 的编排，差异只有三处：

| | 高压 | 低压 |
| --- | --- | --- |
| 配置 | `tool-selection-config.js` | `low-voltage-tool-selection-config.js` |
| 工作任务弹窗 | 图片 `Images.highWorkBg` | 文字版，沿用 `LWorkTicketView` 的 `.task-notice` 样式；**文案需改写**为工器具选择任务（如「请根据本项作业选择所需的个人防护、终端、工器具与线材」），不能直接照搬工作票页那份「两张票均正确后统一提交」的文案 |
| 跳转目标 | `/HSO` | `/LSO` |

其余（草稿恢复、保存进度、计时、操作次数、错误页数、提交 payload 的分数计算
`100 - 错误页数 × 10`）完全一致。

存档恢复失败时沿用 `LWorkTicketView` 的**显式失败面板**（提示 + 重新加载按钮），
而不是高压工具选择页的静默 `catch (_) {}` —— 静默失败会让「存档丢了」变成一个无从排查的问题，
而存档可靠性正是待办事项里要求测试的内容。

### 4.4 占位页

`LSceneOverviewView.vue` 只做三件事：显示「低压场景总览（建设中）」、带上
`experimentId` / `stepId` 的提示、提供「返回实验选择」按钮。目的是让第 2 步的
跳转目标真实存在，另开一期再实现内容。

## 五、素材

现有三张以数字命名的未跟踪素材，按用途与尺寸归位（尺寸验证过：选择页为横向，工具栏为 1:1）：

| 原文件 | 尺寸 | 重命名为 | 用途 |
| --- | --- | --- | --- |
| `tool-selection/wire/4.png` | 4724×3543 | `tool-selection/wire/WireGreenBlack.png` | 选择页 · 4mm²绿黑线 |
| `tool-selection/wire/30.png` | 1048×656 | `tool-selection/wire/WireRS485.png` | 选择页 · RS485 线 |
| `cabinet/toolbar/3.png` | 3543×3543 | `cabinet/toolbar/WireGreenBlack.png` | 工具栏 · 绿黑线 |

其余素材处理：

| 用途 | 处理 |
| --- | --- |
| 扎带 | 复用 `wire/CableTieLabel.png` |
| 其余线材/防护/终端/工器具 | 复用现有素材 |
| 页面背景 | 复用 `tool-selection/ToolSelectionBackground.jpg` |
| 教学视频 | 复用 `videos/test.mp4` |

**工具栏版本的绿黑线本轮只注册、不使用**：它服务于柜体局部操作的工具栏，而低压柜体场景尚未开发。
在 `images.js` 里先加 `barWireGreenBlack` 导出，等做低压柜体时直接引用，避免这张图以数字文件名长期滞留在仓库里。

## 六、数据库

`LV_TRAIN_V1`（`0x...32`）新增第 2 步：

```sql
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00);
```

- 同时写入权威脚本 `04_low_experiment_templates.sql`
- 另写增量脚本 `08_low_tool_selection_step.sql` 供存量库执行
- 时长与分值暂定 180 秒 / 30 分（与高压第 2 步同值）。模板总分从 25 变成 55，
  由于成绩按「原始分 ÷ 步骤分合计 × 100」换算，百分比口径不受影响，后续可调

## 七、验证方式

1. 低压实验启动 → 第 1 步完成后自动播放视频 → 进入 `/LTS`
2. 从实验选择页「继续实验」→ `nextStepOrder = 2` → 进入 `/LTS`（而非 `/LWT`）
3. 四个 Tab 数量：个人防护 12、终端 5、工器具 8、线材 19
4. 只选线材的正确 13 项 → 通过；额外勾选 RS485 线 / 扎带 / 遥控线任一 → 提示错误
5. 个人防护少选一项 → 不通过；选齐 5 项 → 通过
6. 保存进度 → 刷新 → 选择被完整恢复
7. 存档被破坏时展示失败面板，点「重新加载」可重试
8. 提交成功 → 播放视频 → 跳转 `/LSO`
9. 该步骤分数按 `100 - 错误页数 × 10` 计算并落库
10. 高压 `/HTS` 页面在组件移动后功能不变（回归）

## 八、待确认风险

- **素材复用**：背景、扎带、教学视频都借用现有资源，视觉上可能与低压原版不完全一致
- **第 2 步分值**为暂定值，若后续有正式的分值表需同步调整（会改变历史成绩的百分制换算）
- **`/LSO` 是占位页**，学生走到这里会看到「建设中」，属于本期范围内的已知状态
