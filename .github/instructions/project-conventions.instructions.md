---
description: "Use when writing or modifying code in this project, committing changes, or preparing pull requests. Covers file naming conventions, code style, formatting rules, and Git commit workflow."
applyTo: "**"
---

# 虚拟实验平台 — 项目开发规范

## 一、文件命名规范

### 前端 (`frontend/src/`)

| 类型                | 规范                            | 示例                                                                                                                             |
| ------------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Vue 组件            | **PascalCase**                  | `LoginForm.vue`, `ExperimentView.vue`, `HWorkTicketForm.vue`,`LWorkTicketForm.vue`                                               |
| 视图页面            | **PascalCase** + `View` 后缀    | `AdminView.vue`, `LoginView.vue`, `TestView.vue`                                                                                 |
| 高压场景文件        | **大写 H 开头** 区分低压         | `HWorkTicketView.vue`, `HSceneFrame.vue`, `HToolSelectionView.vue`                                                               |
| 低压场景文件        | **大写 L 开头** 区分高压         | `LWorkTicketView.vue`, `LWorkTicketForm.vue`                                                                                     |
| JS 工具/API         | **kebab-case**                  | `tool-selection-config.js`, `request.js`                                                                                         |
| CSS 样式            | **kebab-case**                  | `base.css`, `main.css`                                                                                                           |
| 图片资源            | **PascalCase** 或 **camelCase** | `ExperimentViewBackgroundImage.jpg`, `logo.png`, `HWTBackgroundImage.jpg`, `HighVoltageButton.png`, `HighVoltageButtonHover.png` |
| 目录（组件/视图组） | **PascalCase**                  | `HighVoltage/`, `LowVoltage/`                                                                                                    |
| 目录（工具/资源）   | **kebab-case**                  | `assets/styles/`, `utils/`, `api/`                                                                                               |

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
| SQL 迁移     | `V{n}__{description}.sql`（Flyway） | `V1__init_tables.sql`, `V2__create_data.sql`       |

---

## 二、代码格式规范

### 通用规则（`.editorconfig` 根目录）

- **缩进**：空格（space）
- **前端缩进宽度**：2 空格
- **Java 缩进宽度**：4 空格
- **换行符**：LF（Unix 风格）
- **字符集**：UTF-8
- **行尾空格**：自动去除（Markdown 除外）
- **文件末尾**：必须有空行

### 前端格式（Prettier）

配置文件：`frontend/.prettierrc`

| 规则             | 值                                    |
| ---------------- | ------------------------------------- |
| 分号             | ❌ 不加 (`semi: false`)               |
| 引号             | 单引号 (`singleQuote: true`)          |
| 尾逗号           | ❌ 不加 (`trailingComma: "none"`)     |
| 每行最大宽度     | 100 字符                              |
| 箭头函数参数括号 | 单参数时省略 (`arrowParens: "avoid"`) |

```js
// ✅ 正确：单引号、无分号、箭头单参省括号
import { ref, computed } from 'vue'
const msg = 'hello'
const fn = x => x * 2

// ❌ 错误：双引号、有分号、箭头单参有括号
import { ref, computed } from "vue"
const msg = "hello"
const fn = (x) => x * 2
```

### 后端格式（Spotless + Google Java Format）

- 使用 **Google Java Format** (v1.25.2)
- 自动移除未使用的 import
- 运行格式化：`mvn spotless:apply`

```java
// ✅ 正确：Google Java Format 风格，4 空格缩进
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
        // ...
    }
}
```

---

## 三、前端代码风格

### Vue 组件

- 必须使用 **`<script setup>`** 语法（Composition API）
- 模板注释放在 `<template>` 上方：`<!-- 组件说明 -->`
- 组件选项顺序：`template` → `script setup` → `style`
- 使用 `@/` 别名导入（不可使用相对路径 `../`）
- 模板中绑定的事件名必须与 `defineEmits` 声明及父组件实际函数名一致，禁止凭空捏造不存在的函数名

```vue
<!-- 登录表单组件 -->
<template>
  <div class="login-card">
    <el-form :model="form" @submit.prevent="handleLogin">
      <!-- ... -->
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const form = reactive({ username: "", password: "" });
</script>
```

### Pinia Store

- 使用 **Composition API 风格**（`defineStore` + setup 函数）
- 导出函数使用 camelCase

```js
// ✅ 正确
import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useAuthStore = defineStore("auth", () => {
  const token = ref("");
  const isLoggedIn = computed(() => !!token.value);

  async function login(dto) {
    /* ... */
  }

  return { token, isLoggedIn, login };
});
```

### API 请求

- 统一使用 `@/utils/request.js` 中的 axios 实例
- 导出函数使用 camelCase，动词开头
- 路径以 `/` 开头（基于 `/api` baseURL）

```js
// ✅ 正确
import request from "@/utils/request";

export function startExperiment(templateCode) {
  return request.post("/experiment/start", { templateCode });
}

export function getUnfinishedExperiments() {
  return request.get("/experiment/unfinished");
}
```

### 路由

- 静态路由使用直接 import
- 动态路由使用 `() => import(...)` 懒加载（首页除外）
- 路径使用 kebab-case 短路径名

```js
// ✅ 正确
import LoginView from "@/views/LoginView.vue";

const routes = [
  { path: "/", component: LoginView },
  {
    path: "/experiment",
    component: () => import("@/views/ExperimentView.vue"),
  },
  {
    path: "/HTS",
    component: () => import("@/views/HighVoltage/HToolSelectionView.vue"),
  },
];
```

---

## 四、后端代码风格

### 依赖注入

- 使用 **`@RequiredArgsConstructor`** + `private final` 字段（Lombok 构造器注入）
- 不使用 `@Autowired` 字段注入

```java
// ✅ 正确
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
}

// ❌ 错误
@RestController
public class AuthController {
    @Autowired
    private UserService userService;
}
```

### DTO/VO 命名与结构

- 请求 DTO 放在 `dto/{领域}/`，命名 `{操作}DTO`
- 响应 VO 放在 `dto/{领域}/`，命名 `{操作}VO`
- 使用 `@Valid` 校验请求参数

### 返回格式

- Controller 方法返回 `ResponseEntity<?>`
- 使用 `Map.of(...)` 返回简单消息

```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
    if (userService.existsByUsername(dto.getUsername())) {
        return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
    }
    // ...
    return ResponseEntity.ok(Map.of("message", "注册成功"));
}
```

### 数据库迁移（Flyway）

- 文件命名：`V{n}__{description}.sql`
- 版本号递增，双下划线分隔
- 描述使用小写 + 下划线

---

## 五、Git 提交规范

### 改动前同步

> ⚠️ **重要**：每次准备修改代码前，先拉取远程最新代码，避免产生不必要的合并冲突。

```bash
git pull origin develop
```

### 提交流程

> ⚠️ **重要**：每完成一个小功能点就进行一次提交，不要积攒多个功能一起提交。

1. **同步远程代码** → 2. **完成功能开发** → 3. **格式化代码** → 4. **提交**

### 格式化命令

提交前必须运行格式化：

| 端   | 命令                 | 目录        |
| ---- | -------------------- | ----------- |
| 前端 | `npm run format`     | `frontend/` |
| 后端 | `mvn spotless:apply` | `backend/`  |

> 前端已配置 `husky` + `lint-staged`，`git commit` 时会自动格式化暂存文件。

### 提交信息格式

```
<type>(<scope>): <subject>
```

#### 类型标识（type）

| 类型       | 说明                   | 示例                                            |
| ---------- | ---------------------- | ----------------------------------------------- |
| `feat`     | 新功能                 | `feat(experiment): 添加实验步骤提交接口`        |
| `fix`      | Bug 修复               | `fix(auth): 修复令牌过期未跳转登录页`           |
| `refactor` | 代码重构               | `refactor(component): 提取工器具选择为独立组件` |
| `style`    | 格式调整（不影响逻辑） | `style(frontend): 统一缩进为 2 空格`            |
| `docs`     | 文档更新               | `docs(readme): 添加项目启动说明`                |
| `perf`     | 性能优化               | `perf(query): 优化实验列表查询`                 |
| `chore`    | 构建/工具/依赖         | `chore(deps): 升级 Element Plus 到 2.14`        |

#### 范围标识（scope）— 必填

| 范围         | 涉及内容                                   |
| ------------ | ------------------------------------------ |
| `frontend`   | 前端通用变更                               |
| `backend`    | 后端通用变更                               |
| `auth`       | 认证/登录/注册相关                         |
| `experiment` | 实验业务相关                               |
| `scene`      | 场景页面（全景/柜体/热区）                 |
| `component`  | Vue 组件相关                               |
| `api`        | API 接口层                                 |
| `config`     | 配置文件                                   |
| `naming`     | 文件/路由/变量命名规范                     |
| `archive`    | 存档/恢复/草稿功能                         |
| `deps`       | 依赖管理                                   |

### 提交示例

```bash
# 新功能
git commit -m "feat(experiment): 添加未完成实验恢复功能"

# Bug 修复
git commit -m "fix(auth): 修复注册后未清除验证码状态"

# 格式调整（提交前已运行过格式化，确认无遗漏后提交）
git commit -m "style(backend): 统一应用 Google Java Format"

# 配置文件变更
git commit -m "chore(frontend): 添加 Prettier 格式化配置"
```

### 提交频率准则

- ✅ 完成一个 API 接口 → 提交一次
- ✅ 完成一个 Vue 组件 → 提交一次
- ✅ 完成一个 Bug 修复 → 提交一次
- ✅ 完成一次重构 → 提交一次
- ❌ 不要攒 3 天的代码一起提交
- ❌ 不要提交未完成的半成品代码

---

## 六、技术栈速览

| 层          | 技术                          | 版本        |
| ----------- | ----------------------------- | ----------- |
| 前端框架    | Vue 3 + Vite                  | 3.5 / 8.x   |
| UI 组件库   | Element Plus                  | 2.14        |
| 状态管理    | Pinia                         | 3.0         |
| 路由        | Vue Router                    | 4.6         |
| HTTP 客户端 | Axios                         | 1.16        |
| 后端框架    | Spring Boot                   | 4.0         |
| ORM         | MyBatis-Plus                  | 3.5         |
| 数据库      | MySQL                         | —           |
| 连接池      | Druid                         | 1.2         |
| 迁移工具    | Flyway                        | —           |
| JWT         | jjwt                          | 0.12        |
| 代码简化    | Lombok                        | 1.18        |
| 前端格式化  | Prettier                      | —           |
| 后端格式化  | Spotless + Google Java Format | 2.44 / 1.25 |
