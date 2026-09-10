# 查看学生成绩 — 设计

日期：2026年09月10日
状态：待实现
依赖：Phase 0/1（JWT 鉴权、角色层级、后台布局）已完成

## 一、背景与范围

管理后台 5 个功能中的第 2 个。左侧组织架构树、右侧成绩内容。

**参考原站**（`/fangzhen/userUploadIndex.html`，菜单「查看学生上传管理」）得到三点可借鉴之处：

1. **树节点命名带父级上下文** —— 原站班级节点显示为「2025级电气工程及其自动化 1 班」而非「1班」。本项目 `organization` 表里班级名就叫「1班」，单独出现在汇总表里无法区分年级，因此**汇总表必须显示拼接名**（详见 4.2）
2. **状态标签** —— 原站每人一个「已上传/未上传」标签，本项目映射为「已完成/未完成」，青绿/灰两色
3. **左侧树 + 逐级下钻** 的信息组织方式

**一处刻意偏离原站**：原站学生视图是卡片网格，本设计改为**表格**。原因是本项目需同时展示高/低压两个维度的状态与分数，表格的信息密度明显更合适；仅借鉴其状态标签的视觉语言。

### 本期不做

- 批量改分、导出 Excel、成绩单打印
- 按时间范围筛选成绩
- 新增/编辑/删除组织节点（属「专业班级管理」）

## 二、数据表变更

### 2.1 新增三列

`user_experiments` 表增加人工改分字段，原始 `score` 保持不变以便回溯：

```sql
ALTER TABLE user_experiments
  ADD COLUMN manual_score DECIMAL(5,2) NULL COMMENT '人工改分（百分制 0-100）' AFTER score,
  ADD COLUMN scored_by    BINARY(16)   NULL COMMENT '改分人' AFTER manual_score,
  ADD COLUMN scored_at    DATETIME     NULL COMMENT '改分时间' AFTER scored_by;
```

⚠️ **注意两个分数的量纲不同**：

- `score` 是**原始加权分**（Σ 模板分值 × 得分率），量纲随模板步骤分合计变化，当前高压为 228 分制
- `manual_score` 存**百分制**（0-100）

**为什么人工分不存原始分**：模板步骤分值后期还会调整（已知将发生）。若 `manual_score` 存原始分，一旦模板分值变动，同一条记录的含义就发生漂移 —— 老师当初给的「85 分」会显示成别的值。存百分制则始终稳定，与展示、编辑的量纲一致。

改分接口传 `manualScore: null` 即撤销人工改分，回到系统分。

### 2.2 SQL 脚本

按项目脚本策略（`01`~`04` 为权威脚本，增量脚本后期删除）：

- 同步回写 `01_init_tables.sql` 的 `user_experiments` 建表语句
- 新建 `07_grade_manual_score.sql` 作为存量环境的增量迁移

## 三、分数的口径

### 3.1 现有计分方式

`ExperimentServiceImpl:321` 处逻辑：

```
加权总分 = Σ 步骤模板score × (用户步骤score / 100)
```

这是**原始分**，量纲等于模板全部步骤的模板分合计。当前高压 24 个步骤合计 **228**，低压目前仅 1 个步骤合计 **25**。

### 3.2 统一换算为百分制

**界面上所有分数一律显示百分制（0-100）**：

```
显示分 = 原始分 / 该模板步骤分合计 × 100
```

- 满分恒为 100，不受模板分值调整影响
- **步骤分合计不硬编码**，从 `experiment_steps` 按模板实时求和取得（`SELECT SUM(score) FROM experiment_steps WHERE template_id = ?`）。后期调整步骤分值后，历史分数会自动按新满分重新折算
- 保留 1 位小数

**改分同样按百分制输入**（0-100），与展示单位一致，老师不需要心算换算。

### 3.3 多次实验记录的取值

一个学生在同一类别下可能有多次实验记录（如 `student1` 有 3 条高压记录）。

**取值规则：先按 3.2 把每条已完成记录换算成百分制，再取最大值。**

- 只看 `status = 1` 的记录 —— 进行中的记录 `score` 为 NULL，不参与
- 取最大值而非最新一次 —— 已确认的教学口径
- 该类别下无已完成记录 → 状态为「未完成」，分数显示 `—`

**最终展示优先级**：`显示分 = manual_score ?? max(各已完成记录换算后的百分制分)`

## 四、后端设计

### 4.1 接口清单

全部标注 `@RequireRole(Role.TEACHER)`。可见范围与用户管理页一致：只能看到角色层级低于自己的用户（老师只看得到学生），组织树展示完整。

```
GET  /api/admin/org/tree
→ [{ id, name, type, children: [...] }]          5 级嵌套

GET  /api/admin/grades/classes?orgId=
→ [{ orgId, className, orgName, studentCount, highDone, lowDone, avgScore }]

GET  /api/admin/grades/students?orgId=&page=&size=&name=
→ { records: [{ userId, username, name, highStatus, highScore, lowStatus, lowScore }], total, page, size }

GET  /api/admin/grades/students/{userId}/experiments
→ { records: [{ experimentId, templateName, category, status,
                rawScore,        // 原始加权分（228 分制等），仅供参考
                percentScore,    // 换算后的百分制分，未完成时为 null
                manualScore,     // 人工改分（百分制），未改分时为 null
                startTime, endTime, totalDuration, scoredAt }] }

PUT  /api/admin/grades/experiments/{experimentId}/score
   body: { "manualScore": 200 }   // 传 null 撤销改分
→ { "message": "分数修改成功" }
```

### 4.2 两个设计要点

**`classes` 接口对任意节点都可用** —— 传年级返回其下所有班级，传班级返回它自己，传学院返回其下所有年级的所有班级。这样右侧汇总表的逻辑统一，不必按节点类型分支。

**`className` 为后端拼接的完整名** —— 形如 `2025级电气工程及其自动化1班`，由该班级节点向上回溯父级名称拼接而成（对应原站命名风格）。前端不自行拼接。另需返回 `orgName`（完整路径）供 tooltip 展示。

**只列出有学生的班级？** 不 —— **列出全部班级**，空班级显示 0 人。否则新建的班级在页面上不可见，而「专业班级管理」建完班级后正是要来这里核对。

### 4.3 改分的越权校验

改分前须校验：

1. 目标实验记录存在，否则 404
2. 该实验所属用户在当前操作者可见范围内（`MAX(role.level) < 我的 maxLevel`），否则 403

第 2 条与用户管理页的越权校验同一套逻辑，复用 `AdminUserService` 中的判定。

### 4.4 后端改动清单

| 文件 | 动作 |
| --- | --- |
| `controller/AdminGradeController.java` | 新增 |
| `service/AdminGradeService.java` + `impl/AdminGradeServiceImpl.java` | 新增 |
| `dto/admin/OrgTreeNodeVO.java` | 新增 |
| `dto/admin/ClassSummaryVO.java` | 新增 |
| `dto/admin/StudentGradeVO.java` | 新增 |
| `dto/admin/StudentExperimentVO.java` | 新增 |
| `dto/admin/UpdateScoreDTO.java` | 新增 |
| `mapper/OrganizationMapper.java` | 修改：查询全部节点（含 path 用于拼接名） |
| `mapper/UserExperimentsMapper.java` | 修改：按班级+类别聚合最高分 |
| `service/impl/AdminUserServiceImpl.java` | 复用其层级可见性判定；抽出可复用方法 |
| `docs/sql/01_init_tables.sql` | 修改：`user_experiments` 加三列 |
| `docs/sql/07_grade_manual_score.sql` | 新增（存量环境增量） |

## 五、前端设计

### 5.1 文件结构

```
frontend/src/
├── views/Admin/GradeView.vue                  左树右内容
├── components/Admin/OrgTreePanel.vue          组织树（专业班级管理页可复用）
├── components/Admin/ClassSummaryTable.vue     班级汇总表
├── components/Admin/StudentGradeTable.vue     学生成绩表
├── components/Admin/StudentGradeDialog.vue    完成历史 + 改分
└── api/admin-grade.js
```

路由新增 `/admin/grades`，并把顶栏「查看学生成绩」的 `disabled` 改为 `false`。

### 5.2 布局

```
┌──────────────┬────────────────────────────────────┐
│ 组织架构树    │  面包屑：闽江大学 / … / 2024级1班     │
│              │                                     │
│ ▾ 闽江大学    │  ┌─ 汇总（选中非班级节点时）──────┐  │
│  ▾ 物电学院   │  │ 班级  人数 高压完成 低压完成 平均分│  │
│   ▾ 电气     │  │ 2024级…1班 32  8/32  6/32  186.4 │  │
│    ▾ 2024级  │  └─────────────────────────────┘  │
│      1班 ◀   │  ┌─ 学生成绩（选中班级时）────────┐  │
│      2班     │  │ 姓名 用户名 高压 得分 低压 得分 │  │
│    ▾ 2025级  │  │ 学生A …  已完成 217.9 未完成 — │  │
└──────────────┴────────────────────────────────────┘
```

左栏固定宽度（约 260px），右栏自适应。树默认展开到年级层。

**面包屑**：与原站一致，让用户始终知道自己选的是哪个节点 —— 尤其因为班级名「1班」本身不带上下文。

### 5.3 汇总表（选中非班级节点）

列：班级 / 人数 / 高压完成 / 低压完成 / 平均分。

- 「高压完成」显示 `8/32` 形式
- 「平均分」定义为：**取该班级全部学生的高压显示分与低压显示分，合并后求算术平均**，百分制。无任何有效分时显示 `—`
- 点击行即下钻到该班级的学生成绩表

由于分数已统一为百分制，界面上满分恒为 100，不再需要「228 分制」这类提示。

### 5.4 学生成绩表（选中班级）

列：姓名 / 用户名 / 高压状态 / 高压得分 / 低压状态 / 低压得分 / 操作。

- 状态用 `el-tag`：已完成=青绿，未完成=灰
- 得分显示 `manual_score ?? score`，**人工改分过的分数带一个角标或不同颜色**，便于一眼看出哪些是人工调整的
- 顶部有姓名搜索框，分页复用用户管理页的模式（含页码越界回退）
- 操作列「查看详情」打开完成历史弹窗

### 5.5 完成历史弹窗

`el-table` 列出该生全部实验记录：实验名称 / 类型 / 状态 / 原始分 / 人工分 / 用时 / 开始时间 / 操作。

每行「改分」按钮 → `el-input-number`（范围 `0 ~ 100`，百分制，与展示单位一致）+ 确定。改分后刷新列表。已人工改分的行提供「撤销改分」。

服务端同样校验范围 `0 ~ 100`，超出返回 400。前端校验只是即时反馈，**服务端才是权威**。

**改分是敏感操作**，提交前 `ElMessageBox.confirm` 二次确认，文案含学生姓名与实验名。

## 六、验证方式

**数据准备**：`student1` 已有 3 条高压记录（含 1 条进行中、2 条已完成，分数 208.2 / 217.9），可直接验证取最高分与「进行中不参与」两条规则。

1. 组织树返回 5 级嵌套，层级与 `organization` 表一致
2. 选中年级 → 汇总表列出其下所有班级；选中学院 → 列出其下所有年级的所有班级；空班级显示 0 人
3. 班级汇总的「高压完成」「低压完成」计数与直接查库一致
4. 选中班级 → 学生表列出全班学生，含无任何实验记录的（显示未完成）
5. **换算正确**：`student1` 两次已完成高压记录原始分 208.2 / 217.9，满分 228 → 换算为 **91.3 / 95.6**，学生行显示 **95.6**（取最高）；进行中那条不参与
6. **模板分值调整后自动重折算**：手工把某步骤分改大，学生显示分随之下降（分母变大），无需改代码
7. 改分为 85 → 显示 85 且标记为人工改分；查库 `score` 仍为 217.9 未被覆盖，`manual_score` 为 85
8. 撤销改分 → 回到 95.6
8b. 改分传 120（超过百分制上限）→ 被拒，返回 400
9. 教师改非可见用户的分数 → 403
10. 学生访问 `/api/admin/grades/classes` → 403
11. 分页、姓名搜索、页码越界回退
12. 刷新页面保持在当前选中节点

## 七、待确认风险

| 风险 | 说明 | 处置 |
| --- | --- | --- |
| 班级名为短名 | 库中班级叫「1班」，脱离上下文不可辨识 | 后端拼接 `className`；界面加面包屑；验证项 2 核对 |
| 平均分口径 | 高压低压显示分混在一起平均，可能不符合教学预期 | 已按「所有有效显示分平均」实现；若要分开统计可后续调整 |
| 模板分值调整影响历史分 | 步骤分值一变，历史分数的换算结果随之变化 | 这是百分制折算的固有行为，且符合「满分统一为 100」的预期；验证项 6 专门核对 |
| 原始分与百分制混用 | 后端同时存在 `score`（原始）与 `manual_score`（百分制）两种量纲 | 接口返回字段名明确区分（`rawScore` / `percentScore` / `manualScore`）；界面只出现百分制 |
| 低压模板步骤尚未齐全 | `04_low_experiment_templates.sql` 目前只有 1 个步骤 | 折算后低压满分同样是 100，不再有量纲上的突兀；后续补齐步骤后自动跟随 |
| 改分不可逆的观感 | 虽保留原始 `score`，但界面上看不出 | 人工改分带视觉标记，提供「撤销改分」 |
