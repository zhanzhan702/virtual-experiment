# 虚拟实验平台 — 项目开发规范

## 一、文件命名规范

### 前端 (`frontend/src/`)

| 类型                | 规范                         | 示例                                                                                               |
| ------------------- | ---------------------------- | -------------------------------------------------------------------------------------------------- |
| Vue 组件            | **PascalCase**               | `LoginForm.vue`, `ExperimentView.vue`, `HWorkTicketForm.vue`, `LWorkTicketForm.vue`                |
| 视图页面            | **PascalCase** + `View` 后缀 | `AdminView.vue`, `LoginView.vue`, `TestView.vue`                                                   |
| 高压场景文件        | **大写 H 开头** 区分低压     | `HWorkTicketView.vue`, `HToolSelectionView.vue`, `HSceneOverviewView.vue`, `HCabinetLocalView.vue` |
| 低压场景文件        | **大写 L 开头** 区分高压     | `LWorkTicketView.vue`, `LWorkTicketForm.vue`                                                       |
| JS 工具/API         | **kebab-case**               | `tool-selection-config.js`, `request.js`, `time.js`                                                |
| CSS 样式            | **kebab-case**               | `base.css`, `main.css`                                                                             |
| 图片资源            | **PascalCase**（大驼峰）     | `ExperimentViewBackground.jpg`, `HighVoltageButton.png`, `HighVoltageButtonHover.png`              |
| 目录（组件/视图组） | **PascalCase**               | `HighVoltage/`, `LowVoltage/`                                                                      |
| 目录（工具/资源）   | **kebab-case**               | `assets/styles/`, `utils/`, `api/`, `constants/`                                                   |

### 后端 (`backend/src/main/java/com/example/experiment/`)

| 类型         | 规范                                | 示例                                               |
| ------------ | ----------------------------------- | -------------------------------------------------- |
| Java 类      | **PascalCase**（标准 Java）         | `AuthController.java`, `UserService.java`          |
| Controller   | `{领域}Controller`                  | `AuthController.java`, `ExperimentController.java` |
| Service 接口 | `{领域}Service`                     | `UserService.java`, `ExperimentService.java`       |
| Service 实现 | `{领域}ServiceImpl`（放在 `impl/`） | `UserServiceImpl.java`                             |
| Mapper       | `{Entity}Mapper`                    | `UsersMapper.java`, `RolesMapper.java`             |
| DTO（请求）  | `{操作}DTO`                         | `LoginDTO.java`, `RegisterDTO.java`                |
| DTO（响应）  | `{操作}VO`                          | `LoginVO.java`, `UserVO.java`                      |
| Entity       | **复数形式**                        | `Users.java`, `Roles.java`, `UserExperiments.java` |

### 路由命名（首字母缩写）

| 路径          | 页面                            |   模式   |
| ------------- | ------------------------------- | :------: |
| `/`           | 登录                            |  全路径  |
| `/register`   | 注册                            |  全路径  |
| `/experiment` | 实验选择                        |  全路径  |
| `/admin`      | 管理后台                        |  全路径  |
| `/HWT`        | 高压工作票                      | H + 缩写 |
| `/HTS`        | 高压工器具选择                  | H + 缩写 |
| `/HSO`        | 高压配电室总览                  | H + 缩写 |
| `/HCL`        | 高压柜体局部操作（步骤3/4共用） | H + 缩写 |
| `/LWT`        | 低压工作票                      | L + 缩写 |

---

## 二、代码格式规范

### 通用规则

- **缩进**：空格（space）；前端 2 空格，Java 4 空格
- **换行符**：LF（Unix 风格）；**字符集**：UTF-8
- **文件末尾**：必须有空行

### 前端格式（Prettier，规则以 `frontend/.prettierrc` 为准）

### 后端格式（Spotless + Google Java Format）

- 运行格式化：`mvn spotless:apply`
- 自动移除未使用的 import

---

## 三、前端代码风格

### Vue 组件

- 必须使用 **`<script setup>`** 语法（Composition API）
- 组件选项顺序：`template` → `script setup` → `style`
- 使用 `@/` 别名导入（不可使用相对路径 `../`）
- 模板中绑定的事件名必须与 `defineEmits` 声明及父组件实际函数名一致，**禁止凭空捏造不存在的函数名**
- 页面固定的操作按钮（保存进度、查看工作任务）使用绝对定位且各页面保持风格一致

### 图片资源集中管理

- 所有图片统一在 `frontend/src/constants/images.js` 中 import + export，组件使用 `Images.xxx`，CSS 背景图使用 `var(--img-xxx)`（由 `main.js` 注入）
- 更换图片格式只需修改 `images.js` 一处 import 路径
- **图片命名规则**：
  - 选择页版本（工器具选择页，3:5）→ **默认无前缀**：`Images.crossScrewdriver`, `Images.seal`
  - 工具栏版本（柜体局部操作 HCL，1:1）→ **`bar` 前缀**：`Images.barCrossScrewdriver`, `Images.barSeal`
- 图片按使用场景分类存放于 `assets/images/` 子目录：`common/`, `scenario/`, `scene/`, `cabinet/`（含 `toolbar/`）, `tool-selection/`（含 `ppe/`）, `metering-room/`

### Pinia Store

- 使用 **Composition API 风格**（`defineStore` + setup 函数）

### API 请求

- 统一使用 `@/utils/request.js` 中的 axios 实例
- 导出函数使用 camelCase，动词开头；路径以 `/` 开头（基于 `/api` baseURL）

---

## 四、后端代码风格

- 依赖注入：使用 **`@RequiredArgsConstructor`** + `private final` 字段，不使用 `@Autowired`
- 请求 DTO 放 `dto/{领域}/`，命名 `{操作}DTO`；响应 VO 命名 `{操作}VO`；使用 `@Valid` 校验
- Controller 返回 `ResponseEntity<?>`，使用 `Map.of(...)` 返回简单消息

### 数据库

- **Flyway 已移除**，数据库手动管理，迁移/初始化脚本在 `docs/sql/`（按编号执行）
- 结构变更需手动执行对应 SQL 或直接改库

---

## 五、Git 提交规范

### 改动前同步

> ⚠️ 每次准备修改代码前，先拉取远程最新代码，避免合并冲突。

```bash
git pull origin develop
```

### 提交流程

1. **同步远程代码** → 2. **完成功能开发** → 3. **格式化代码** → 4. **用户测试确认** → 5. **提交**
2. 每完成一个小功能点就提交一次，不要积攒多个功能一起提交

> ⚠️ **提交前必须确认没有错误**：代码完成格式化后先由用户测试验证，确认实现没有问题后再提交。禁止在用户未确认的情况下提交代码；用户测试发现问题时先修复，直到用户确认通过。文档类变更同样遵循此流程。

### 格式化命令

| 端   | 命令                 | 目录        |
| ---- | -------------------- | ----------- |
| 前端 | `npm run format`     | `frontend/` |
| 后端 | `mvn spotless:apply` | `backend/`  |

### 提交信息格式

```
<type>(<scope>): <subject>
```

#### 类型标识（type）

| 类型       | 说明                   |
| ---------- | ---------------------- |
| `feat`     | 新功能                 |
| `fix`      | Bug 修复               |
| `refactor` | 代码重构               |
| `style`    | 格式调整（不影响逻辑） |
| `docs`     | 文档更新               |
| `perf`     | 性能优化               |
| `chore`    | 构建/工具/依赖         |

#### 范围标识（scope）— 必填

| 范围         | 涉及内容                   |
| ------------ | -------------------------- |
| `frontend`   | 前端通用变更               |
| `backend`    | 后端通用变更               |
| `auth`       | 认证/登录/注册相关         |
| `experiment` | 实验业务相关               |
| `scene`      | 场景页面（全景/柜体/热区） |
| `component`  | Vue 组件相关               |
| `api`        | API 接口层                 |
| `config`     | 配置文件                   |
| `naming`     | 文件/路由/变量命名规范     |
| `archive`    | 存档/恢复/草稿功能         |
| `deps`       | 依赖管理                   |

### 提交示例

```bash
git commit -m "feat(experiment): 添加未完成实验恢复功能"
git commit -m "fix(auth): 修复注册后未清除验证码状态"
git commit -m "style(backend): 统一应用 Google Java Format"
git commit -m "chore(frontend): 添加 Prettier 格式化配置"
```

### 提交频率准则

- ✅ 完成一个 API 接口 / Vue 组件 / Bug 修复 / 一次重构 → 提交一次
- ❌ 不要攒多天代码一起提交；不要提交未完成的半成品代码

---
