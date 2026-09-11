# 专业班级管理 — 设计

日期：2026年09月10日
状态：待实现
依赖：Phase 0/1（JWT 鉴权、角色层级、后台布局）与「查看学生成绩」的 `OrgTreePanel` 组件已完成

## 一、背景与范围

管理后台 5 个功能中的第 3 个。左侧组织架构树、右侧节点编辑表单。

### 数据现状

```
organization 表 6 个节点：
  闽江大学 [university]                        ← 2 个用户（admin、superadmin）
    └ 物理与电子信息工程学院 [college]          ← 1 个用户（teacher1）
        └ 电气工程及其自动化 [major]
            └ 2024级 [grade]
                ├ 1班 [class]                 ← 2 个用户（student1、test2）
                └ 2班 [class]

path 形如 /闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/1班/
（首尾都带斜杠，因此前缀匹配无歧义：/a/b/ 不会误匹配 /a/bc/）
```

### 两个外键约束

| 外键 | 含义 |
| --- | --- |
| `organization.parent_id → organization.id` | 自引用，父子关系 |
| `users.org_id → organization.id` | 用户挂靠到某节点 |

这两个约束会**挡住删除**：删「2024级」因其下有班级而失败，删「1班」因有学生而失败。直接删会抛数据库异常，需要在应用层给出友好提示。

### 本期不做

- **移动节点到其他父级** —— 需要同时重算整棵子树的 path 与层级，且很容易造出「班级直接挂在学院下」这类破坏树结构的脏数据。原需求也只提了「新增编辑以及删除」
- 批量导入组织架构
- 节点的启用/停用状态

## 二、节点层级模型

层级由 `type` 表达，**子节点的 type 由父节点推导**，不由用户选择：

| 父节点 type | 允许的子节点 type |
| --- | --- |
| （无父节点） | `university` |
| `university` | `college` |
| `college` | `major` |
| `major` | `grade` |
| `grade` | `class` |
| `class` | **不允许**（叶子节点，再加子节点返回 400） |

这样从根往下只有一条确定路径，不存在「班级挂在学院下」的非法状态。

## 三、path 的维护（本设计最容易出错的部分）

`path` 不只是展示字段 —— **「查看学生成绩」页的按节点筛选完全依赖它**：

```sql
WHERE o.path LIKE CONCAT(#{pathPrefix}, '%')
```

因此 path 一旦与实际层级不符，成绩页会过滤出错误的学生。三条维护规则：

### 3.1 新增节点

```
子节点.path = 父节点.path + 子节点.name + "/"
```

### 3.2 重命名节点（需级联）

重命名会改变自身 path，**其所有子孙的 path 也必须同步更新**，否则子孙的 path 仍指向旧名字，成绩页按新名字筛选时会漏掉它们。

```
设 P_old = 节点原 path，P_new = 新 path
节点.path = P_new
对每个满足 path 以 P_old 开头的子孙 D：
    D.path = P_new + D.path.substring(P_old.length())
```

例：把「2024级」改名为「2025级」
```
P_old = /闽江大学/.../2024级/
P_new = /闽江大学/.../2025级/

1班  /闽江大学/.../2024级/1班/  →  /闽江大学/.../2025级/1班/
2班  /闽江大学/.../2024级/2班/  →  /闽江大学/.../2025级/2班/
```

整个操作在**一个事务**内完成，避免中途失败留下半截数据。

### 3.3 名称校验

**名称不能包含 `/`** —— 否则会破坏 path 的分段结构，且无法与分隔符区分。同时拒绝首尾空白、空字符串。

同级节点**不允许重名**（数据库没有这个约束，在应用层校验）：同年级下两个「1班」会让成绩页与用户管理页无法区分。跨级重名允许（不同年级下都可以有「1班」）。

## 四、后端设计

### 4.1 接口清单

```
GET    /api/admin/org/tree                组织树（成绩页也要用）   @RequireRole(TEACHER)
POST   /api/admin/org/nodes               新增 { parentId?, name }  @RequireRole(ADMIN)
PUT    /api/admin/org/nodes/{id}          重命名 { name }           @RequireRole(ADMIN)
DELETE /api/admin/org/nodes/{id}          删除                      @RequireRole(ADMIN)
POST   /api/admin/org/nodes/{id}/move     排序 { direction }        @RequireRole(ADMIN)
```

**组织树接口从 `AdminGradeController` 迁到 `AdminOrgController`**（URL 不变，前端无感知），让组织相关接口聚在一处。

但两个门槛不同：成绩页的老师需要读树，而改动组织架构影响全局、应当只允许管理员。处理方式是在类上标 `@RequireRole(Role.ADMIN)`，在 `getOrgTree` 方法上单独标 `@RequireRole(Role.TEACHER)`：

```java
@RestController
@RequestMapping("/api/admin/org")
@RequireRole(Role.ADMIN)          // 类级：默认管理员
public class AdminOrgController {

  @GetMapping("/tree")
  @RequireRole(Role.TEACHER)      // 方法级优先，放行老师
  public ResponseEntity<?> getOrgTree() { ... }
}
```

`AuthInterceptor` 已实现「方法级注解优先于类级」，无需改动。

树接口本次只**增加** `userCount` 字段（节点下挂靠的用户数），不改变其余结构，成绩页无需改动。该字段用于删除前提示「该班级下还有 2 名学生」。

### 4.2 删除守卫

删除前依次校验，任一不满足即返回 400 并给出具体原因：

| 情况 | 响应 |
| --- | --- |
| 节点不存在 | 404 `{"message":"组织节点不存在"}` |
| 有子节点 | 400 `该节点下还有 N 个子节点，请先删除子节点` |
| 有用户挂靠 | 400 `该节点下还有 N 名用户，请先调整归属` |

**两个外键是最后一道防线**：即便应用层校验有疏漏，数据库也会拒绝删除，不会产生孤儿数据。但应用层校验能给出可读的提示，而不是一串外键约束错误。

### 4.3 排序交换

上移时与该节点的**前一个兄弟**交换 `sort` 值，下移时与后一个兄弟交换。已是第一个/最后一个时返回 400（前端会把对应按钮置灰，但服务端仍要校验）。

交换 `sort` 而非重新编号，改动最小；`sort` 值重复时（理论上不应出现）按 `sort, name` 排序仍是确定的。

### 4.4 后端改动清单

| 文件 | 动作 |
| --- | --- |
| `controller/AdminOrgController.java` | 新增（含从 GradeController 迁来的 `/tree`） |
| `service/AdminOrgService.java` + `impl/AdminOrgServiceImpl.java` | 新增（含建树逻辑，从 GradeService 迁来） |
| `dto/admin/OrgNodeDTO.java` | 新增（新增/重命名的请求体） |
| `dto/admin/MoveNodeDTO.java` | 新增（direction） |
| `dto/admin/OrgTreeNodeVO.java` | 修改：增加 `userCount` |
| `mapper/OrganizationMapper.java` | 修改：新增用户数聚合、兄弟节点查询 |
| `service/AdminGradeService.java` + `impl/` | 修改：移除建树逻辑（迁往 AdminOrgService） |
| `controller/AdminGradeController.java` | 修改：移除 `/org/tree` |

## 五、前端设计

### 5.1 文件结构

```
frontend/src/
├── views/Admin/OrgManageView.vue          左树右表单
├── components/Admin/OrgNodeForm.vue       节点编辑表单
└── api/admin-org.js
```

复用「查看学生成绩」的 `components/Admin/OrgTreePanel.vue`，但需要它支持**刷新**（增删改后树要重载）与**选中态恢复**。

路由新增 `/admin/org`，顶栏「专业班级管理」的 `disabled` 改为 `false`。

### 5.2 布局

```
┌──────────────┬────────────────────────────────┐
│ 组织架构树    │  父级：电气工程及其自动化（只读） │
│              │  名称：[2024级              ]   │
│ ▾ 闽江大学    │  类型：年级（自动推导，只读）    │
│  ▾ 物电学院   │  排序：[↑ 上移]  [↓ 下移]       │
│   ▾ 电气     │                                │
│    ▾ 2024级 ◀│  [保存修改]  [删除节点]         │
│      1班     │  ──────────────────────────    │
│      2班     │  [+ 新增子节点]                 │
└──────────────┴────────────────────────────────┘
```

### 5.3 表单行为

- **父级**：只读展示。未选中节点时显示「新增学校」，此时父级为空
- **类型**：由父级推导后只读展示（`grade` 显示为「年级」），用户不能改
- **排序**：两个按钮，在首位/末位时对应按钮置灰
- **保存**：仅名称可编辑。名称未变时保存按钮置灰
- **删除**：二次确认弹窗，文案含节点名。若有子节点或用户，后端返回 400，前端用 `ElMessage.error` 展示后端给的具体原因
- **新增子节点**：`class` 类型节点隐藏此按钮（叶子节点不能加子节点）；新增后自动刷新树并选中新节点

### 5.4 树的刷新与选中态

增删改后需要重载树。`OrgTreePanel` 当前只在 `onMounted` 加载一次，本次为其增加：

- `refresh()`：重新拉取树数据
- 重载后按 `nodeKey` 恢复选中节点；被删除的节点则清空右侧表单

## 六、验证方式

1. 树节点显示用户数，与 `SELECT org_id, COUNT(*) FROM users GROUP BY org_id` 一致
2. 新增年级 → 出现在对应专业下，`type` 自动为 `grade`，`path` 继承父级
3. 在班级节点下新增子节点 → 被拒绝（叶子节点）
4. 重命名「2024级」为「2025级」→ 树中更新，**且两个班级的 `path` 同步更新**（查库确认）
5. 重命名后进入「查看学生成绩」，按该年级筛选仍能正确列出学生（**回归验证 path 级联未出错**）
6. 重命名导致同级重名 → 被拒绝
7. 名称含 `/` → 被拒绝
8. 删除有子节点的「2024级」→ 400 提示「还有 2 个子节点」
9. 删除有用户的「1班」→ 400 提示「还有 2 名用户」
10. 删除空的「2班」→ 成功，树中消失
11. 上移/下移 → 顺序变化；在首位点「上移」→ 400
12. 教师访问 `/api/admin/org/nodes` → 403（本接口门槛是管理员）
13. 增删改后树的选中态正确，被删节点的右侧表单清空

## 七、待确认风险

| 风险 | 说明 | 处置 |
| --- | --- | --- |
| **path 级联遗漏** | 重命名后子孙 path 未更新，成绩页按新名字筛选会漏人 | 3.2 给出算法；验证项 4、5 专门核对 |
| 名称含分隔符 | 名称里的 `/` 会破坏 path 分段 | 3.3 校验；验证项 7 核对 |
| 同级重名 | 数据库无唯一约束，两个「1班」无法区分 | 应用层校验；验证项 6 核对 |
| 外键报错外泄 | 绕过应用层校验时数据库抛外键异常，前端显示一串英文 | 应用层先校验给出中文提示；外键作为兜底防线 |
| 并发编辑 | 两人同时改同一节点，后提交者覆盖前者 | 本期不做乐观锁。组织架构调整是低频操作，且影响面可人工发现 |
