# 虚拟实验平台 (Virtual Experiment Platform)

基于 **Vue 3 + Spring Boot 4** 的虚拟实验教学平台，支持学生在线实验操作、教师管理实验、管理员系统管理。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 3.5 / 5.4 |
| UI 组件库 | Element Plus | 2.5 |
| 路由 | Vue Router | 4.3 |
| 状态管理 | Pinia | 3.0 |
| HTTP 客户端 | Axios | 1.6 |
| 后端框架 | Spring Boot | 4.0 |
| ORM | MyBatis-Plus | 3.5 |
| 数据库 | MySQL | 8.0 |
| 数据库迁移 | Flyway | — |
| 身份认证 | JWT (jjwt) | 0.12 |
| 密码加密 | BCrypt (spring-security-crypto) | — |
| 参数校验 | Jakarta Validation | — |

---

## 项目结构

```
virtual-experiment/
├── backend/                                 # 后端 Spring Boot 工程
│   ├── src/main/java/com/example/experiment/
│   │   ├── config/
│   │   │   └── WebConfig.java               # CORS 跨域配置
│   │   ├── controller/
│   │   │   └── AuthController.java          # 注册 / 登录接口
│   │   ├── service/
│   │   │   ├── UserService.java             # 用户服务接口
│   │   │   └── impl/
│   │   │       └── UserServiceImpl.java     # 用户服务实现
│   │   ├── mapper/                          # MyBatis-Plus Mapper（8 张表）
│   │   ├── entity/                          # 数据库实体（8 张表）
│   │   ├── dto/                             # 数据传输对象
│   │   │   ├── LoginDTO.java                # 登录请求
│   │   │   ├── RegisterDTO.java             # 注册请求
│   │   │   ├── LoginVO.java                 # 登录响应（含 token + 角色）
│   │   │   └── UserVO.java                  # 用户信息返回
│   │   ├── handler/
│   │   │   └── UUIDTypeHandler.java         # UUID ↔ BINARY(16) 类型转换
│   │   └── utils/
│   │       └── JwtUtils.java                # JWT 生成 / 解析
│   ├── src/main/resources/
│   │   ├── application.yml                  # 主配置
│   │   └── db/migration/                    # Flyway 数据库迁移
│   │       ├── V1__init_tables.sql           # 建表
│   │       ├── V2__create_data.sql           # 初始数据
│   │       └── V3__update_bcrypt_passwords.sql  # BCrypt 密码更新
│   └── pom.xml
│
├── frontend/                                # 前端 Vue 3 + Vite 工程
│   ├── src/
│   │   ├── api/
│   │   │   └── auth.js                      # 登录 / 注册 API
│   │   ├── assets/                          # 图片等静态资源
│   │   ├── components/
│   │   │   ├── LoginForm.vue                # 登录表单（Element Plus）
│   │   │   ├── RegisterForm.vue             # 注册表单（Element Plus）
│   │   │   ├── LeftPreview.vue              # 左侧装饰图片
│   │   │   └── VerifyCode.vue               # 验证码组件
│   │   ├── views/
│   │   │   ├── LoginView.vue                # 登录页
│   │   │   ├── RegisterView.vue             # 学生注册页
│   │   │   ├── ExperimentView.vue           # 学生实验页
│   │   │   └── AdminView.vue                # 管理后台页
│   │   ├── stores/
│   │   │   └── auth.js                      # 用户状态（Pinia）
│   │   ├── router/
│   │   │   └── index.js                     # 路由配置
│   │   ├── utils/
│   │   │   └── request.js                   # Axios 拦截器
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js                       # Vite 配置（含 /api 代理）
│   └── package.json
│
└── README.md
```

---

## 快速开始

### 1. 数据库

```bash
mysql -u root -p -e "CREATE DATABASE virtual_experiment DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

启动后端后，Flyway 自动执行 `db/migration/` 下的 SQL 脚本。

### 2. 启动后端

```bash
cd backend

# Windows
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

| 用户名 | 密码 | 角色 | 登录后跳转 |
|--------|------|------|-----------|
| `student1` | student123 | student | /experiment |
| `teacher1` | teacher123 | teacher | /admin |
| `admin` | admin123 | admin | /admin |

学生可通过注册页面自助注册。

---

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 学生注册（自动分配 student 角色） |
| POST | `/api/auth/login` | 登录，返回 JWT + 角色 + 跳转路径 |

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

---

## 路由设计

| 路径 | 页面 | 访问权限 |
|------|------|---------|
| `/` | 登录页 | 公开 |
| `/register` | 学生注册页 | 公开 |
| `/experiment` | 学生实验页 | 学生角色 |
| `/admin` | 管理后台 | 教师 / 管理员角色 |

---

## 数据库表

| 表名 | 说明 |
|------|------|
| `organization` | 组织架构（树形：大学→学院→专业→年级→班级） |
| `users` | 用户 |
| `roles` | 角色（admin / teacher / student） |
| `user_roles` | 用户角色关联 |
| `experiment_templates` | 实验模板 |
| `experiment_steps` | 实验步骤 |
| `user_experiments` | 用户实验记录 |
| `user_experiment_steps` | 用户实验步骤记录 |

---

## 设计说明

- **注册**：前端只开放学生注册，后端自动分配 `student` 角色
- **登录**：登录页不分角色，后端根据 `user_roles` 返回角色列表，前端据此跳转和渲染导航
- **密码**：BCrypt 加密存储（UUIDTypeHandler 不会误拦截 BCrypt 哈希）
- **UUID**：`BINARY(16)` 列通过全局 `UUIDTypeHandler` 自动与 Java `String` 互转
