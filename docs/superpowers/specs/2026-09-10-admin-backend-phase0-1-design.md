# 管理员/教师后台 — Phase 0 基础设施 + Phase 1 用户管理

日期：2026年09月10日
状态：待实现

## 一、背景与范围

管理员/教师后台共规划 5 个功能：用户管理、查看学生成绩、专业班级管理、统计数据分析、个人消息管理（修改密码）。这 5 个功能各自独立且体量不小，拆成多个子项目逐个走「设计 → 计划 → 实现」。

本次只覆盖前两阶段：

- **Phase 0**：后端鉴权基础设施 + 分页插件 + 前端路由守卫 + 后台布局骨架
- **Phase 1**：用户管理页

### 为什么 Phase 0 必须先做

现状盘点：

| 项 | 现状 |
| --- | --- |
| `views/AdminView.vue` | 仅一行「管理后台」占位 |
| 路由守卫 | 无 `beforeEach`，无 `meta` 权限标记 |
| 后端鉴权 | **完全没有**。前端已发 `Authorization: Bearer`，但后端无拦截器、无安全过滤器 |
| 分页 | 未配置 MyBatis-Plus `PaginationInnerInterceptor` |
| 角色 | 仅 `admin` / `teacher` / `student`，无「超级管理员」 |
| 组织架构 | `organization` 表已是 5 级：`university → college → major → grade → class` |
| 统计埋点 | 无访问日志表 |

后端鉴权缺失是硬阻塞：用户管理要求「管理员看到老师+学生、老师只看到学生」，这是**按请求者身份决定数据范围**的逻辑。没有服务端身份识别，纯前端过滤等于没有安全边界 —— 任何人构造一个 HTTP 请求就能拿到全量用户。

### 本期明确不做

- 学生成绩页、专业班级管理、统计数据分析、个人消息管理 —— 后续各自独立设计
- 新增/编辑/删除用户（`users` 表写操作）—— 本期只读 + 改密
- 角色分配、组织架构树编辑
- 访问日志表与浏览量统计建表（属统计页，届时再设计）
- token 主动失效机制（`token_version`）
- 自助改密入口（顶栏下拉本期只放「退出登录」）

## 二、角色层级模型

`roles` 表新增 `level INT`，数值越大权限越高。可见范围统一判定为 `level < 我的 level`。

| code | name | level |
| --- | --- | --- |
| `super_admin` | 超级管理员 | 40 |
| `admin` | 管理员 | 30 |
| `teacher` | 教师 | 20 |
| `student` | 学生 | 10 |

用数据行而非代码常量表达层级，后期增减角色或微调老师权限只改数据、不改代码。

用户可能持有多个角色，判定其可见性取**最高** level：`HAVING MAX(r.level) < :myMaxLevel`。这样「既是教师又是管理员」的用户对普通管理员不可见、对超管可见，符合层级直觉。

## 三、数据表变更

### 3.1 SQL 脚本策略

本项目数据库可整体清空重建，因此脚本分工如下：

| 脚本 | 定位 | 维护要求 |
| --- | --- | --- |
| `01` ~ `04` | **权威建库脚本**。从零建库的唯一依据 | **始终同步到最新**，任何结构变更都要回写 |
| `05`、`06` | 增量变更记录，仅供「不重建库」的存量环境使用 | 后期整体删除，只保留 `01` ~ `04` |

推论出一条硬约束：**增量脚本必须是完全冗余的** —— 它记录的每一条变更，都必须在 `01` ~ `04` 中有对应实现。否则删掉增量脚本就会丢失变更。本次据此设计，`06` 中的结构变更全部回写进 `01` 与 `02`。

本次仅涉及 `01`（表结构）与 `02`（初始数据）；`03`、`04` 是实验模板数据，与本次无关，不动。

### 3.2 回写 `01` ~ `04`（权威路径）

**`01_init_tables.sql`** —— `roles` 表增加层级列：

```sql
-- 角色表
CREATE TABLE roles (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    level INT NOT NULL DEFAULT 0 COMMENT '角色层级，数值越大权限越高'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

只加一列，`code` 的 `UNIQUE` 保持原样不额外加索引。

**`02_create_data.sql`** —— 重写，见下方 3.2.1 的完整内容。关键改动有两点：角色 INSERT 带上 `level`；弃用会话变量。

#### 3.2.1 弃用会话变量（顺带修复既有隐患）

现有脚本用 `SET @univ_id = (SELECT id FROM organization WHERE ...)` 承接上级 ID，注释写的是「避免同表子查询报错」。这个写法有真实危险：

**会话变量是连接级的。** 若执行方逐条新开连接（部分 GUI 工具的「执行文件」、`allowMultiQueries` 语句拆分器、某些迁移工具都是这样），`@univ_id` 会取到 `NULL`。而 `organization.parent_id` 与 `users.org_id` **都允许 NULL** —— 于是脚本**全程不报错地**写出断裂的组织架构树和没有班级的用户。

`03` / `04` 有同样写法（`SET @tpl_id`），但 `experiment_steps.template_id` 是 `NOT NULL`，那边会立刻报错，反而是安全的。`02` 是唯一会静默损坏的脚本。

**方案：所有被跨行引用的种子数据改用固定 UUID 字面量。**

```sql
-- 固定 UUID 分配表（后续脚本直接引用这些字面量）
--   组织架构：01 闽江大学 / 02 物理与电子信息工程学院 / 03 电气工程及其自动化
--             04 2024级   / 05 1班 / 06 2班
--   角色：    11 学生 / 12 教师 / 13 管理员 / 14 超级管理员
--   用户：    21 admin / 22 teacher1 / 23 student1 / 24 superadmin
--   模板：    31 高压训练场景V1 / 32 低压训练场景V1
```

固定字面量同时满足两种执行方式，结果完全一致，且不再依赖任何会话状态。未被引用的叶子行（`experiment_steps` 等）继续用 `UUID_TO_BIN(UUID())` 生成即可。

**重写后的 `02_create_data.sql`：**

```sql
-- V2 初始化数据
-- 注意：本脚本不使用会话变量（SET @var）。
-- 会话变量是连接级的，逐条新开连接执行时会丢失并静默写入 NULL
-- （organization.parent_id 与 users.org_id 均可空，不会报错）。
-- 固定 UUID 字面量保证「手动逐句执行」与「整文件一次性执行」结果一致。

-- 1. 组织架构
ALTER TABLE organization
ADD CONSTRAINT chk_organization_type
CHECK (type IN ('university', 'college', 'major', 'grade', 'class'));

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000001, '闽江大学', 'university', NULL, '/闽江大学/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000002, '物理与电子信息工程学院', 'college',
 0x00000000000000000000000000000001,
 '/闽江大学/物理与电子信息工程学院/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000003, '电气工程及其自动化', 'major',
 0x00000000000000000000000000000002,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000004, '2024级', 'grade',
 0x00000000000000000000000000000003,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000005, '1班', 'class',
 0x00000000000000000000000000000004,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/1班/', 1),
(0x00000000000000000000000000000006, '2班', 'class',
 0x00000000000000000000000000000004,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/2班/', 2);

-- 2. 角色（先插入，供 user_roles 引用）
INSERT INTO roles (id, code, name, level) VALUES
(0x00000000000000000000000000000011, 'student',     '学生',       10),
(0x00000000000000000000000000000012, 'teacher',     '教师',       20),
(0x00000000000000000000000000000013, 'admin',       '管理员',     30),
(0x00000000000000000000000000000014, 'super_admin', '超级管理员', 40);

-- 3. 用户（密码为 BCrypt 哈希值）
-- superadmin 的哈希与 admin 相同，即初始密码一致
INSERT INTO users (id, username, password, name, org_id) VALUES
(0x00000000000000000000000000000021, 'admin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme', '管理员',
 0x00000000000000000000000000000001),
(0x00000000000000000000000000000022, 'teacher1',
 '$2a$10$mKchFrQcW7J/adF5kluUH.98xHoppnW85/OEsJx96GfLMP1SD/TDi', '教师A',
 0x00000000000000000000000000000002),
(0x00000000000000000000000000000023, 'student1',
 '$2a$10$Z9LfnXd9GhoS3JGW8sB7pOa6fu/KnoBTYAK.9MK52kDCUsbnDUST6', '学生A',
 0x00000000000000000000000000000005),
(0x00000000000000000000000000000024, 'superadmin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme', '超级管理员',
 0x00000000000000000000000000000001);

-- 4. 用户角色关联
INSERT INTO user_roles (user_id, role_id) VALUES
(0x00000000000000000000000000000021, 0x00000000000000000000000000000013),
(0x00000000000000000000000000000022, 0x00000000000000000000000000000012),
(0x00000000000000000000000000000023, 0x00000000000000000000000000000011),
(0x00000000000000000000000000000024, 0x00000000000000000000000000000014);
```

顺带修掉了原先的一处不一致：`student1` 的 `org_id` 原本写的是行内子查询 `(SELECT id FROM organization WHERE name = '1班')`，而 `admin`、`teacher1` 用的是变量。现在三者统一为字面量。

⚠️ **本节最容易出错的一点**：只改 `01` 而不改 `02`，新建库执行后所有角色 `level` 都会落到 `DEFAULT 0`，`level < 我的 level` 恒不成立，用户列表将对所有人返回空。`01` 与 `02` 必须成对修改。

#### 3.2.2 同步 `03` / `04`

两个模板脚本各有一处 `SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = '...')`。同样的隐患，改法一致 —— 模板改用固定字面量，`experiment_steps` 的 `id` 仍是生成的：

```sql
-- 03：高压模板
INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(0x00000000000000000000000000000031, 'HV_TRAIN_V1', '高压训练场景V1', 'high_voltage', 'training', '1.0',
 '高压场景下的用电信息采集终端安装与调试训练模式');

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000031, 1, 'FILL_TICKET', '填写工作票', 120, 20.00);
-- ... 其余步骤同理，template_id 全部用 0x...31
```

`04` 的写法与上面完全一致，只是模板字面量换成 `0x00000000000000000000000000000032`（`LV_TRAIN_V1`）。

步骤行的 `id` 保持 `UUID_TO_BIN(UUID())`：没有任何脚本引用它们，生成即可。

> 这两处 `03` / `04` 的改动超出了“本次只做 Phase 0/1”的原始范围，但它与 `02` 是同一个隐患，且你明确要求脚本要能两种方式执行。若你希望本次不碰实验模板脚本，说一声我撤掉 3.2.2，只修 `02`。

### 3.3 增量脚本 `06`（存量环境专用）

新建 `backend/docs/sql/06_admin_role_level.sql`：

```sql
-- 06. 角色层级：数值越大权限越高；用户可见范围 = level < 我的 level（2026-09-10）
-- 注意：本脚本所有变更均已同步至 01 / 02，重建库无需执行；
--       本脚本仅为「不重建库」的存量环境提供增量迁移，后期会删除
-- 数据库可整体重建时，直接执行 01 → 02 → 03 → 04 即可

ALTER TABLE roles ADD COLUMN level INT NOT NULL DEFAULT 0 COMMENT '角色层级，数值越大权限越高';

UPDATE roles SET level = 30 WHERE code = 'admin';
UPDATE roles SET level = 20 WHERE code = 'teacher';
UPDATE roles SET level = 10 WHERE code = 'student';

INSERT INTO roles (id, code, name, level) VALUES
(UUID_TO_BIN(UUID()), 'super_admin', '超级管理员', 40);

-- 超级管理员账号（密码哈希与 admin 一致，即初始密码与 admin 相同）
-- 自助改密入口在 Phase 4 提供，在此之前该账号密码只能由 DBA 直接改库
INSERT INTO users (id, username, password, name, org_id) VALUES
(UUID_TO_BIN(UUID()), 'superadmin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme',
 '超级管理员', (SELECT id FROM organization WHERE name = '闽江大学'));

INSERT INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'superadmin'),
 (SELECT id FROM roles WHERE code = 'super_admin'));
```

**为什么 `06` 用子查询而 `01`~`04` 用字面量**：`06` 面向的是**已存在的库**，其中的组织架构行是早先随机生成的 UUID，脚本无从预知。此时只能按 `name` 反查。

好消息是 `06` 天然没有会话变量问题 —— 它用的是**跨语句**子查询（`INSERT INTO user_roles ... (SELECT id FROM users WHERE ...)`），每个子查询都在独立语句里查已提交的数据，与执行方式无关，两种方式都安全。真正危险的是同一语句内既写又读同一张表，以及跨语句传递的会话变量，`06` 两者都不涉及。

顺序说明：`UPDATE` 排在 `INSERT super_admin` 之前，先补齐旧角色的 `level`，再插入新角色。`super_admin` 直接带 `level = 40` 插入，不需要 `UPDATE`。

除 `roles.level` 外不改动任何表结构。详情弹窗的实验记录靠 `user_experiments` 左连接 `experiment_templates` 取得，无需新表。

### 3.4 执行方式

**推荐的日常做法**：直接清库重建。

```
DROP DATABASE virtual_experiment;
CREATE DATABASE virtual_experiment ...;
执行 01 → 02 → 03 → 04
```

建完即是包含超管账号与角色层级的完整最新结构，不需要碰 `05` / `06`。

**仅在库中有不可丢弃的数据时**，才执行 `06` 做增量迁移。

### 3.5 脚本执行方式约定

`01`~`04` 必须同时支持两种执行方式，且结果完全一致：

| 方式 | 典型场景 |
| --- | --- |
| 手动逐句执行 | 在 GUI 里一条条选中执行 |
| 整文件一次性执行 | 「执行 SQL 文件」；客户端可能逐条新开连接 |

为此脚本遵守两条规则：

1. **不使用会话变量**（`SET @var`）—— 连接级状态在「逐条新连接」模式下会丢失
2. **不在同一语句内既写又读同一张表** —— 避免 MySQL 1093 及结果不确定

跨行引用一律通过固定 UUID 字面量（`01`~`04`）或跨语句子查询（`06`）完成。

验证项 0 会专门核对两种执行方式的结果一致。

## 四、后端设计

### 4.1 鉴权链路

```
请求 → AuthInterceptor.preHandle
         ├─ 路径命中白名单 /api/auth/login、/api/auth/register → 放行
         │  （注意：不是整个 /api/auth/** —— /api/auth/me 需经拦截器取 userId）
         ├─ 无 Authorization 头 / 非 Bearer 格式 → 401 {"message":"未登录"}
         ├─ JwtUtils.validateToken 失败（过期/篡改）→ 401 {"message":"登录已过期，请重新登录"}
         ├─ 解析 userId / roles / maxLevel → UserContext.set(...)
         └─ 方法标注 @RequireRole(n) 且 maxLevel < n → 403 {"message":"权限不足"}
      → Controller
      → AuthInterceptor.afterCompletion: UserContext.clear()   ← 必须，否则线程池串号
```

### 4.2 新增文件

```
backend/src/main/java/com/example/experiment/
├── config/
│   ├── AuthInterceptor.java      HandlerInterceptor，实现 4.1 链路
│   ├── RequireRole.java          方法级注解，value 为 Role 枚举
│   ├── Role.java                 枚举 STUDENT(10)/TEACHER(20)/ADMIN(30)/SUPER_ADMIN(40)
│   ├── MybatisPlusConfig.java    注册 PaginationInnerInterceptor
│   └── WebConfig.java            【改】注册拦截器，放行 login / register
└── utils/
    ├── UserContext.java          ThreadLocal 存 userId / roles / maxLevel
    └── JwtUtils.java             【改】generateToken 增加 maxLevel 参数
```

`@RequireRole` 语义：当前用户 `maxLevel >= value.level` 才放行。用枚举而非魔法数字，避免调用处写错数值。

`WebConfig` 现有内容只有 CORS 过滤器，本次追加 `addInterceptors`，不动 CORS 配置。

### 4.3 maxLevel 的存放位置

`maxLevel` 在签发 token 时查库一次，写入 JWT claim，之后每个请求零查询。

**已知代价**：JWT 是签发时刻的快照，token 有效期内（24 小时）若调整了某人的角色，对方仍持旧权限。本期无角色管理功能，触及不到此场景；后期实现角色管理时，在角色变更接口中引入 `token_version` 即可，改动集中在 `JwtUtils` 与 `AuthInterceptor` 两处。

### 4.4 API 契约

所有接口响应体沿用 `AuthController` 现有的 `Map.of("message", ...)` 风格，不引入新的错误包装类。

#### ① 分页列表

```
GET /api/admin/users?page=1&size=10&name=&username=&phone=&orgId=
@RequireRole(TEACHER)     ← 老师也需查学生，最低门槛为教师

→ {
    "records": [{
      "id": "...", "username": "student1", "name": "学生A",
      "gender": "女", "orgName": "闽江大学/.../1班", "phone": "138..."
    }],
    "total": 137, "page": 1, "size": 10
  }
```

- `WHERE` 硬条件：用户 `MAX(r.level) < :myMaxLevel`，每行都必须满足
- `name` / `username` / `phone`：可选，`LIKE '%x%'` 模糊匹配
- `orgId`：本期预留。传了则按 `organization.path LIKE '前缀%'` 过滤；前端本期不传
- `orgName` 由后端拼接 `organization.path`，前端不自行拼接

#### ② 详情（含实验记录）

```
GET /api/admin/users/{id}
@RequireRole(TEACHER)

→ {
    ...users 表全部字段, "orgName": "完整路径",
    "experiments": [{
      "templateName": "高压...", "category": "high_voltage",
      "status": 1, "score": 92.50,
      "startTime": "...", "totalDuration": 1800
    }]
  }
```

目标用户 `level >= 我的 maxLevel` → 403 `{"message":"无权操作该用户"}`
目标用户不存在 → 404 `{"message":"用户不存在"}`

#### ③ 重置他人密码

```
PUT /api/admin/users/{id}/password
@RequireRole(TEACHER)
{ "newPassword": "...", "confirmPassword": "..." }

服务端校验顺序：
  1. 目标用户存在，否则 404
  2. 目标用户 MAX(r.level) < 我的 maxLevel，否则 403 {"message":"无权操作该用户"}
  3. 两次输入一致，否则 400 {"message":"两次输入的密码不一致"}
  4. 强度校验（见 4.5），否则 400 {"message":"密码需6-20位，且同时包含字母和数字"}
  5. BCrypt 加密落库
→ { "message": "密码修改成功" }
```

**语义**：不改动对方已签发的 token，对方当前会话在有效期内仍可用，下次登录必须使用新密码。因该操作对他人不可逆，前端提交前需二次确认。

#### ④ 当前登录用户信息（登录态恢复）

```
GET /api/auth/me
（无 @RequireRole —— 拦截器已校验 token，任何已登录用户可调）

→ { "user": { ...UserVO }, "roles": ["admin"], "maxLevel": 30 }
```

**为什么需要它**：前端 `stores/auth.js` 中只有 `token` 从 `localStorage` 初始化，`user` 与 `roles` 均为内存态：

```js
const token = ref(localStorage.getItem('token') || '')
const user  = ref(null)   // 刷新后丢失
const roles = ref([])     // 刷新后丢失
```

目前无路由守卫，问题未暴露；一旦加上读 `roles` 判定 level 的 `beforeEach`，用户在后台页面按 F5 就会被踢回登录页。此接口由拦截器已写入的 `UserContext.userId` 反查用户，实现约 15 行。

放在 `AuthController` 而非 `AdminUserController`：它是所有登录用户的通用能力，且 `/api/auth/**` 已在拦截器白名单内 —— 注意白名单需放行 `me` 的**认证**（拦截器本身）但不能让它变成免鉴权接口，因此实现上要把它从白名单中排除，或改为拦截器只放行 `login` 与 `register`。

**接口契约修正**：拦截器白名单不是整个 `/api/auth/**`，而是精确的 `/api/auth/login` 与 `/api/auth/register`。`/api/auth/me` 必须经过拦截器以获得 `userId`。

### 4.5 密码强度规则（注册与重置统一）

现存注册只要求 `@Size(min=6)`，无字母数字要求。本次统一收紧为共用规则：

| 规则 | 值 |
| --- | --- |
| 长度 | 6-20 位 |
| 必须含 | 至少一个字母 `[A-Za-z]` |
| 必须含 | 至少一个数字 `[0-9]` |
| 禁止 | 空白字符 |

校验正则：`^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$`

后端定义为共用常量与校验方法，`RegisterDTO` 与 `UpdatePasswordDTO` 复用同一实现；前端在一处定义规则常量，`RegisterForm.vue` 与重置密码弹窗共用。

**影响面**：注册流程的校验规则与提示文案会变化；已有账号密码不受影响，仍可用旧密码登录。

### 4.6 后端改动清单

| 文件 | 动作 |
| --- | --- |
| `config/Role.java` | 新增 |
| `config/RequireRole.java` | 新增 |
| `config/AuthInterceptor.java` | 新增 |
| `config/MybatisPlusConfig.java` | 新增 |
| `utils/UserContext.java` | 新增 |
| `config/WebConfig.java` | 修改：注册拦截器，放行 `login` / `register` |
| `controller/AuthController.java` | 修改：新增 `GET /api/auth/me` |
| `service/UserService.java` + `impl/UserServiceImpl.java` | 修改：新增 `getCurrentUser(userId)` |
| `utils/JwtUtils.java` | 修改：`generateToken` 增加 `maxLevel` |
| `service/impl/UserServiceImpl.java` | 修改：登录时计算 `maxLevel` 并签发 |
| `dto/auth/RegisterDTO.java` | 修改：密码规则收紧 |
| `dto/auth/UserVO.java` | 修改：增加 `maxLevel`（登录返回给前端做路由守卫） |
| `controller/AdminUserController.java` | 新增 |
| `service/AdminUserService.java` + `impl/AdminUserServiceImpl.java` | 新增 |
| `dto/admin/UserQueryDTO.java` | 新增 |
| `dto/admin/UserListVO.java` | 新增 |
| `dto/admin/UserDetailVO.java` | 新增 |
| `dto/admin/UserExperimentVO.java` | 新增 |
| `dto/admin/UpdatePasswordDTO.java` | 新增 |
| `mapper/UsersMapper.java` | 修改：自定义分页查询（多表连接 + GROUP BY + HAVING） |
| `mapper/UsersMapper.xml` 或注解 SQL | 新增/修改 |
| `docs/sql/01_init_tables.sql` | 修改：`roles` 表增加 `level` 列 |
| `docs/sql/02_create_data.sql` | **重写**：角色 INSERT 带 `level`、追加 `superadmin`、弃用会话变量改固定 UUID |
| `docs/sql/03_high_experiment_templates.sql` | 修改：弃用 `@tpl_id`，模板改固定 UUID |
| `docs/sql/04_low_experiment_templates.sql` | 修改：同上 |
| `docs/sql/06_admin_role_level.sql` | 新增（存量环境增量迁移，后期删除） |

`01`~`04` 是权威建库脚本，即使 `03` / `04` 的改动超出 Phase 0/1 的功能范围，也必须一并修掉 —— 否则「清库重建」这条推荐路径仍会踩到会话变量的坑。

## 五、前端设计

### 5.1 文件结构

删除 `views/AdminView.vue`（原为占位），新建 `views/Admin/` 目录：

```
frontend/src/
├── views/Admin/
│   ├── AdminLayoutView.vue        布局壳：顶栏 + <router-view/>
│   └── UserManageView.vue         用户管理页
├── components/Admin/
│   ├── UserDetailDialog.vue       详情弹窗
│   └── ResetPasswordDialog.vue    重置密码弹窗
├── api/
│   ├── admin-user.js              用户管理接口层
│   └── auth.js                    【改】新增 getCurrentUser()
├── stores/auth.js                 【改】maxLevel 状态 + fetchMe()
├── constants/
│   └── password-rule.js           密码规则常量（与注册共用）
├── utils/request.js               【改】401 统一处理
└── router/index.js                【改】子路由 + beforeEach 守卫
```

### 5.2 路由与守卫

```js
{ path: '/admin', component: AdminLayoutView, meta: { minLevel: 20 },
  children: [
    { path: '', redirect: '/admin/users' },
    { path: 'users', component: () => import('@/views/Admin/UserManageView.vue') }
  ] }
```

全局 `beforeEach` 逻辑：

1. 若 `authStore.roles` 为空但 `token` 存在（刷新后的首次导航）→ 先 `await authStore.fetchMe()` 调 `GET /api/auth/me` 恢复 `user` / `roles` / `maxLevel`
2. `fetchMe` 失败（token 过期或无效）→ 清登录态，跳回登录页
3. 读 `authStore.maxLevel`，低于 `meta.minLevel` → 拦回登录页

**这是 UX 兜底，不是安全边界** —— 真正的拦截在后端 `@RequireRole`。守卫只决定「菜单显不显示、页面进不进得去」，即便被绕过，后端仍会返回 403。

`stores/auth.js` 相应改动：新增 `maxLevel` 状态、`fetchMe()` 方法；`logout()` 一并清空 `maxLevel`。

### 5.3 布局壳

```
┌────────────────────────────────────────────────────┐
│  虚拟实验平台   用户管理 │ 学生成绩 │ 班级管理 │ ... │  管理员 ▾ │
├────────────────────────────────────────────────────┤
│                      <router-view />                │
└────────────────────────────────────────────────────┘
```

顶栏用 `el-menu mode="horizontal"`；右侧 `el-dropdown` 显示当前用户姓名，展开项仅「退出登录」。

5 个入口**全部渲染**，后 4 个（学生成绩 / 班级管理 / 统计 / 个人消息）`disabled` + `el-tooltip` 提示「待开发」。布局骨架一次到位，后续每个页面上线只需改一处 `disabled` 状态，无需重排版。

### 5.4 用户管理页

**搜索区**（`el-form :inline="true"`）

| 控件 | 说明 |
| --- | --- |
| 姓名 / 用户名 / 电话 | 三个 `el-input`，`clearable`，回车即搜 |
| 搜索 | 重置 `page = 1` 后请求 |
| 重置 | 清空三个输入 + `page = 1` + 重新请求 |
| 刷新 | 保持当前搜索条件与页码重查 |

**表格**（`el-table`）

| 列 | 来源 |
| --- | --- |
| 用户名 | `username` |
| 姓名 | `name` |
| 性别 | `gender` |
| 单位/班级 | `orgName` |
| 联系电话 | `phone` |
| 操作 | 「修改密码」「查看详情」两个 `el-button link` |

**分页**（`el-pagination`）

```
layout="total, sizes, prev, pager, next, jumper"
:page-sizes="[10, 15, 20, 50]"
```

`total` 取后端返回的真实总数。原始需求中「或显示当前页能容纳的最大数据量」**不实现**：需在前端量算容器高度推导行数，窗口缩放即失效，且与 `sizes` 选择器语义冲突。若后续确需「自适应满屏」，改为在 `size` 选项里增加「自动」并由 `ResizeObserver` 计算。

**重置密码弹窗**（`ResetPasswordDialog.vue`）

新密码 + 确认密码两个 `show-password` 输入框。前端即时校验（两次一致 + 强度），后端再校验一遍。提交前弹 `ElMessageBox.confirm` 二次确认，文案包含目标用户名（"确定重置 学生A 的密码？此操作不可撤销"），**不显示明文密码**。

**详情弹窗**（`UserDetailDialog.vue`）

- 上半：`el-descriptions` 两列展示全部字段（用户名、姓名、性别、生日、学号、电话、邮箱、单位/班级、注册时间）
- 下半：`el-table` 展示高/低压实验记录 —— 模板名、类型标签（高压/低压用不同色 `el-tag`）、状态、得分、开始时间、用时
- 无记录时 `el-empty` 显示「暂无实验记录」

### 5.5 错误处理

`utils/request.js` 响应拦截器增加 401 分支：清 `localStorage.token` → 重置 authStore → 跳回登录页。

**循环依赖问题**：`router` → `views` → `api` → `request` → `router` 构成环。解决方式是用 `window.location.href = '/'` 跳转，而非 import `router`。

403 / 400 由各页面 `catch` 后 `ElMessage.error(err.response.data.message)`，直接透传后端文案，前端不维护第二套错误话术。

**分页越界**：搜索条件变化后 `page` 可能超出总页数（例如搜到仅剩 3 条但停在 `page = 5`）。后端返回空 `records`，前端在响应处理中判断 `records.length === 0 && page > 1` 则将 `page` 重置为 1 重查一次，避免用户看到空表格却不知可翻回。

## 六、验证方式

不引入自动化测试（与项目现状一致 —— 后端仅有 Spring Boot 默认上下文测试，前端零测试）。按以下清单人工验证。

**数据库（先做，后续验证依赖它）**

0. 清库后执行 `01` → `02` → `03` → `04`，然后核对：
   - `SELECT code, level FROM roles ORDER BY level DESC;` → `super_admin 40 / admin 30 / teacher 20 / student 10`
   - `SELECT username FROM users;` → 含 `superadmin`
   - `SELECT u.username, r.code FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id;` → `superadmin` 绑定 `super_admin`
   - `SELECT name, parent_id IS NULL AS 无父节点 FROM organization ORDER BY sort;` → **只有「闽江大学」的 `无父节点` 为 1**，其余 5 个组织均为 0
   - `SELECT username, org_id IS NULL AS 无组织 FROM users;` → **全部为 0**
   - **若 `level` 全是 0，说明 `02` 漏改** —— 此时用户列表会对所有人返回空

0b. **两种执行方式结果一致**（本次修掉的隐患，必须专门验证）：
   - 清库后用「整文件一次性执行」跑 `01`~`04`，记录 `organization` 与 `users` 的行数与 `parent_id` / `org_id` 是否为空
   - 再清库，改为**逐句选中执行**，重复上述核对
   - 两次结果必须相同；若第二次出现 `parent_id` 或 `org_id` 为 NULL，说明脚本又引入了会话变量

**Phase 0**

1. `superadmin` 用与 `admin` 相同的密码可登录
2. 不带 token 直接请求 `/api/experiment/unfinished` → 401
3. 用学生 token 请求 `/api/admin/users` → 403
4. 带正确 token 请求 `/api/experiment/unfinished` → 正常返回（**回归验证，确认拦截器未误伤现有实验流程**）
5. 学生登录后访问 `/admin/users` → 前端守卫拦回登录页
5b. 管理员在 `/admin/users` 页面按 F5 刷新 → 停留原页，不被踢回登录页（验证 `fetchMe` 登录态恢复）
5c. 手动清空 `localStorage.token` 后访问 `/admin/users` → 拦回登录页

**Phase 1**

6. 三种角色登录，列表条数符合「只看得到 level 低于自己的用户」：超管看到全部，管理员看不到超管与其他管理员，老师只看到学生
7. 三个搜索框各自单独生效、组合生效；重置能清空条件
8. 每页条数切换、跳转框、总条数显示正确
9. 详情弹窗展示全部字段；有实验记录的用户能看到高/低压记录并带类型标签；无记录显示空状态
10. 改密成功 → 退出 → 新密码登录成功，旧密码登录失败
11. 越权改密（老师改管理员）→ 403
12. 改密输入 `123456`（纯数字）→ 被拦；输入 `abc123` → 通过

## 七、待确认风险

| 风险 | 说明 | 处置 |
| --- | --- | --- |
| **`01` 改了但 `02` 漏改** | `02` 的角色 INSERT 不带 `level` 会落到 `DEFAULT 0`，`level < 我的 level` 恒不成立，用户列表对所有人返回空 | 本设计最高优先级风险，验证项 0 专门核对 |
| **会话变量在逐条执行时静默损坏数据** | `SET @var` 是连接级状态；逐条新连接执行时取到 NULL，而 `parent_id` / `org_id` 均可空，脚本不报错就写出断裂的组织树与无班级用户 | `01`~`04` 全面弃用会话变量，改固定 UUID 字面量；验证项 0b 专门核对两种执行方式一致 |
| 增量脚本与权威脚本漂移 | `06` 后期要删除，若它记录了 `01`/`02` 没有的变更，删除即丢变更 | 硬约束：`06` 必须是完全冗余的；本次已逐条回写 `01`/`02` |
| 拦截器影响现有接口 | 现有实验流程接口此前无鉴权，加上拦截器后若 token 传递有遗漏会整体 401 | 验证项 4 专门回归；白名单精确放行 `login` / `register` |
| 登录态刷新丢失 | `stores/auth.js` 的 `user` / `roles` 为内存态，刷新即丢 | 由 `GET /api/auth/me` + 守卫内 `fetchMe` 解决，验证项 5b / 5c |
| `superadmin` 密码无法自助修改 | 复用 admin 哈希，Phase 4 之前只能改库 | 已确认接受 |
| 注册密码规则收紧 | 新注册用户受影响，提示文案变化 | 已确认接受；存量账号不受影响 |
| JWT 内 maxLevel 快照 | 角色调整后最长 24 小时才生效 | 本期无角色管理，触及不到 |
| 老师可见范围全库 | 跨院系学生互相可见 | 已确认接受；接口预留 `orgId` 参数，后期可启用 |
