# 虚拟实验平台 (Virtual Experiment Platform)

基于 **Vue 3 + Spring Boot 4** 的虚拟实验教学平台，模拟用电信息采集终端安装与调试场景，支持学生在线完成高压 / 低压实验操作、教师管理实验、管理员系统管理。

---

## 技术栈

| 层级        | 技术                            | 版本      |
| ----------- | ------------------------------- | --------- |
| 前端框架    | Vue 3 + Vite                    | 3.5 / 8.0 |
| UI 组件库   | Element Plus                    | 2.14      |
| 路由        | Vue Router                      | 4.6       |
| 状态管理    | Pinia                           | 3.0       |
| HTTP 客户端 | Axios                           | 1.16      |
| 后端框架    | Spring Boot                     | 4.0       |
| ORM         | MyBatis-Plus                    | 3.5       |
| 数据库      | MySQL                           | 8.0       |
| 身份认证    | JWT (jjwt)                      | 0.12      |
| 密码加密    | BCrypt (spring-security-crypto) | —         |
| 参数校验    | Jakarta Validation              | —         |

---

## 项目结构

```
virtual-experiment/
├── backend/                                 # 后端 Spring Boot 工程
│   ├── src/main/java/com/example/experiment/
│   │   ├── config/
│   │   │   ├── WebConfig.java               # CORS 跨域配置
│   │   ├── controller/
│   │   │   ├── HelloController.java         # 健康检查 /api/hello
│   │   │   ├── AuthController.java          # 注册 / 登录接口
│   │   │   └── ExperimentController.java    # 实验启动 / 步骤提交 / 存档 / 恢复
│   │   ├── service/
│   │   │   ├── UserService.java             # 用户服务接口
│   │   │   ├── ExperimentService.java       # 实验服务接口（含存档）
│   │   │   └── impl/
│   │   │       ├── UserServiceImpl.java     # 用户服务实现（注册 / 登录）
│   │   │       └── ExperimentServiceImpl.java # 实验服务实现（启动 / 提交 / 存档 / 删除）
│   │   ├── mapper/                          # MyBatis-Plus Mapper（8 张表）
│   │   ├── entity/                          # 数据库实体（8 张表）
│   │   │   ├── Users.java                   # 用户
│   │   │   ├── Organization.java            # 组织架构
│   │   │   ├── Roles.java                   # 角色
│   │   │   ├── UserRoles.java               # 用户角色关联
│   │   │   ├── ExperimentTemplates.java     # 实验模板
│   │   │   ├── ExperimentSteps.java         # 实验步骤
│   │   │   ├── UserExperiments.java         # 用户实验记录
│   │   │   └── UserExperimentSteps.java     # 用户实验步骤记录
│   │   ├── dto/                             # 数据传输对象（按域分包）
│   │   │   ├── auth/                        # 认证相关 DTO
│   │   │   │   ├── LoginDTO.java
│   │   │   │   ├── LoginVO.java
│   │   │   │   ├── RegisterDTO.java
│   │   │   │   └── UserVO.java
│   │   │   └── experiment/                  # 实验相关 DTO
│   │   │       ├── ExperimentStartDTO.java
│   │   │       ├── ExperimentStartVO.java
│   │   │       ├── ExperimentStepSubmitDTO.java
│   │   │       └── UnfinishedExperimentVO.java
│   │   ├── handler/
│   │   │   └── UUIDTypeHandler.java         # UUID ↔ BINARY(16) 类型转换
│   │   └── utils/
│   │       └── JwtUtils.java                # JWT 生成 / 解析
│   ├── src/main/resources/
│   │   ├── application.yml                  # 主配置（数据源 / MyBatis）
│   │   └── application.properties           # 数据源凭据
│   ├── docs/sql/                            # 手动建库 SQL 脚本（按顺序执行）
│   │   ├── 01_init_tables.sql               # 建表（8 张表）
│   │   ├── 02_create_data.sql               # 初始数据（组织 / 角色 / 用户）
│   │   ├── 03_high_experiment_templates.sql # 高压实验模板 + 步骤
│   │   └── 04_low_experiment_templates.sql  # 低压实验模板 + 步骤
│   └── pom.xml
│
├── .github/
│   └── instructions/
│       └── project-conventions.instructions.md  # 项目开发规范
│
├── frontend/                                # 前端 Vue 3 + Vite 工程
│   ├── src/
│   │   ├── api/
│   │   │   ├── auth.js                      # 登录 / 注册 API
│   │   │   └── experiment.js                # 实验 API（启动 / 提交 / 存档 / 恢复 / 删除）
│   │   ├── assets/                          # 图片等静态资源
│   │   ├── components/
│   │   │   ├── LoginForm.vue                # 登录表单
│   │   │   ├── RegisterForm.vue             # 注册表单
│   │   │   ├── LeftPreview.vue              # 左侧装饰图片
│   │   │   ├── VerifyCode.vue               # Canvas 验证码组件
│   │   │   ├── ScenarioSelector.vue         # 高/低压场景选择
│   │   │   ├── PromptModal.vue              # 通用弹窗（确认按钮叠加）
│   │   │   ├── HighVoltage/
│   │   │   │   ├── HWorkTicketForm.vue      # 高压工作票表单
│   │   │   │   ├── HWizardInventorySelection.vue # 工器具选择向导
│   │   │   │   └── HSceneHotspotOverlay.vue # 场景热区叠加层
│   │   │   └── LowVoltage/
│   │   │       └── LWorkTicketForm.vue      # 低压工作票表单
│   │   ├── views/
│   │   │   ├── LoginView.vue                # 登录页
│   │   │   ├── RegisterView.vue             # 学生注册页
│   │   │   ├── ExperimentView.vue           # 场景选择 + 启动实验 + 恢复
│   │   │   ├── AdminView.vue                # 管理后台页
│   │   │   ├── TestView.vue                 # 后端连通性测试页
│   │   │   ├── HighVoltage/
│   │   │   │   ├── HWorkTicketView.vue      # 工作票填写步骤页
│   │   │   │   ├── HToolSelectionView.vue   # 工器具选择步骤页
│   │   │   │   ├── HSceneOverviewView.vue   # 配电房全景图页
│   │   │   │   └── HCabinetLocalView.vue    # 柜体局部操作页（步骤3+4）
│   │   │   └── LowVoltage/
│   │   │       └── LWorkTicketView.vue      # 低压工作票步骤页
│   │   ├── stores/
│   │   │   └── auth.js                      # 用户认证状态（Pinia）
│   │   ├── router/
│   │   │   └── index.js                     # 路由配置
│   │   ├── utils/
│   │   │   ├── request.js                   # Axios 拦截器（JWT 注入 / 错误处理）
│   │   │   └── time.js                      # 本地时间格式化工具
│   │   ├── constants/
│   │   │   ├── images.js                   # 图片资源集中管理
│   │   │   └── tool-selection-config.js     # 工器具选择配置
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js                       # Vite 配置（含 /api 代理）
│   └── package.json
│
└── README.md
```

---

## 实验流程

```mermaid
flowchart LR
    A[登录] --> B[选择实验场景<br/>高/低压]
    B --> C[启动实验]
    C --> D[步骤 1<br/>填写工作票]
    D --> E[步骤 2<br/>工器具选择]
    E --> F[全景图<br/>配电房漫游]
    F --> G[步骤 3<br/>柜体操作<br/>设围栏 + 挂牌]
    G --> H[教学视频]
    H --> I[步骤 4<br/>三步验电]
    I --> J[实验完成]
```

### 已实现

| 步骤 | 页面路由 | 组件 | 说明 |
|------|---------|------|------|
| 场景选择 | `/experiment` | `ExperimentView` | 高/低压实验场景选择、未完成实验恢复 |
| 全景漫游 | `/HSO` | `HSceneOverviewView` | 配电房全景图、梯形热区定位、柜体入口 |
| 步骤 1 | `/HWT` | `HWorkTicketView` | 填写工作票（手动校验、存档/恢复） |
| 步骤 2 | `/HTS` | `HToolSelectionView` | 工器具选择向导（分页、选项卡、存档/恢复） |
| 步骤 3 | `/HCL` | `HCabinetLocalView` | 柜体局部操作：设围栏 + 挂告示牌（4 物品拖放） |
| 步骤 4 | `/HCL` | `HCabinetLocalView` | 三步验电（电源→柜体→电源）、提交完成 |

### 已实现功能

- ✅ 用户注册/登录（JWT + BCrypt）
- ✅ 实验启动、步骤提交、进度存档、草稿恢复
- ✅ 未完成实验检测与恢复/删除
- ✅ 鼠标跟随物品拖放、命中检测
- ✅ 三步验电流程（电压检测笔交互）
- ✅ 配电房全景图 + 梯形热区（CSS `clip-path`）
- ✅ 响应式布局（`vw`/`vh`/`%` 相对定位）
- ✅ 教学视频占位过渡
- ✅ 操作统计（时长、操作次数、错误次数）与评分

### 待完善

- 低压场景全部步骤
- 考试模式（当前仅支持训练模式）
- 教师管理后台功能
- 教学视频录制

---

## 快速开始

### 1. 数据库

```bash
mysql -u root -p -e "CREATE DATABASE virtual_experiment DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

数据库为**手动管理**：依次执行 `backend/docs/sql/` 下的脚本（01 → 02 → 03 → 04）。

### 2. 启动后端

```bash
cd backend

# Windows PowerShell
$env:MYSQL_PASSWORD="你的密码"

# Windows CMD
set MYSQL_PASSWORD=你的密码

# Linux / Mac
export MYSQL_PASSWORD=你的密码

# 启动
.\mvnw spring-boot:run
```

默认端口：`http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`http://localhost:5173`（已配置 `/api` 代理到后端 8080）

---

## 测试账号

| 用户名     | 密码       | 角色    | 登录后跳转  |
| ---------- | ---------- | ------- | ----------- |
| `student1` | student123 | student | /experiment |
| `teacher1` | teacher123 | teacher | /admin      |
| `admin`    | admin123   | admin   | /admin      |

学生可通过注册页面自助注册，密码使用 BCrypt 加密存储。

---

## API 接口

### 认证

| 方法 | 路径                 | 说明                              | 认证 |
| ---- | -------------------- | --------------------------------- | ---- |
| POST | `/api/auth/register` | 学生注册（自动分配 student 角色） | 无   |
| POST | `/api/auth/login`    | 登录，返回 JWT + 角色 + 跳转路径  | 无   |

### 实验

| 方法   | 路径                          | 说明                   | 认证       |
| ------ | ----------------------------- | ---------------------- | ---------- |
| POST   | `/api/experiment/start`       | 启动实验               | Bearer JWT |
| POST   | `/api/experiment/step/submit` | 提交步骤结果（评分）   | Bearer JWT |
| POST   | `/api/experiment/step/draft`  | 保存进度草稿（不评分） | Bearer JWT |
| GET    | `/api/experiment/unfinished`  | 查询未完成实验         | Bearer JWT |
| GET    | `/api/experiment/step/draft`  | 恢复步骤草稿数据       | Bearer JWT |
| DELETE | `/api/experiment/{id}`        | 删除未完成实验（级联） | Bearer JWT |

### 登录响应示例

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "74341904353443fcab03633f8cfebfa3",
    "username": "student1",
    "name": "学生A"
  },
  "roles": ["student"],
  "redirectUrl": "/experiment"
}
```

### 启动实验请求示例

```json
{
  "templateCode": "HV_TRAIN_V1"
}
```

### 启动实验响应示例

```json
{
  "experimentId": "abc123...",
  "templateName": "高压训练场景V1",
  "startTime": "2026-06-17T10:30:00",
  "steps": [
    { "stepId": "def456...", "stepName": "填写工作票", "stepOrder": 1 }
  ]
}
```

---

## 路由设计

| 路径          | 页面             | 访问权限          |
| ------------- | ---------------- | ----------------- |
| `/`           | 登录页           | 公开              |
| `/register`   | 学生注册页       | 公开              |
| `/experiment` | 实验场景选择页   | 登录即可          |
| `/admin`      | 管理后台         | 教师/管理员       |
| `/HSO`        | 高压配电房全景图 | 登录即可          |
| `/HWT`        | 高压工作票填写   | 登录即可          |
| `/HTS`        | 高压工器具选择   | 登录即可          |
| `/HCL`        | 柜体局部操作     | 登录即可（步骤3/4共用） |
| `/LWT`        | 低压工作票填写   | 登录即可          |

---

## 数据库表

| 表名                    | 说明                                       |
| ----------------------- | ------------------------------------------ |
| `organization`          | 组织架构（树形：大学→学院→专业→年级→班级） |
| `users`                 | 用户（BCrypt 密码）                        |
| `roles`                 | 角色（admin / teacher / student）          |
| `user_roles`            | 用户角色关联                               |
| `experiment_templates`  | 实验模板                                   |
| `experiment_steps`      | 实验步骤                                   |
| `user_experiments`      | 用户实验记录                               |
| `user_experiment_steps` | 用户实验步骤记录（含操作统计 / 结果 JSON） |

---

## 设计说明

- **注册**：前端只开放学生注册，后端自动分配 `student` 角色
- **登录**：登录页不分角色，后端根据 `user_roles` 返回角色列表，前端据此跳转和渲染导航
- **密码**：BCrypt 加密存储，`UUIDTypeHandler` 不会误拦截 BCrypt 哈希（通过 hex 模式 + 16 字节长度双重校验）
- **UUID**：`BINARY(16)` 列通过全局 `UUIDTypeHandler` 自动与 Java `String`（32 位 hex）互转
- **DTO 按域分包**：`dto/auth/` 存放认证相关，`dto/experiment/` 存放实验相关
- **实验存档**：支持保存进度草稿（全量表单数据），下次进入时询问是否继续
- **级联删除**：重新开始时仅删除未完成实验（status=0），已完成实验受保护
- **操作统计**：自动记录操作次数、错误次数、耗时，作为评分依据
- **评分机制**：满分 100 分，每错误一次扣 10 分，最低 0 分
