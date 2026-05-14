virtual-experiment/
├── backend/                          # 后端 SpringBoot 工程
│   ├── src/main/java/com/example/experiment/
│   │   ├── config/                   # 安全、跨域、OSS等配置
│   │   ├── controller/               # 接口控制器
│   │   ├── service/                  # 业务逻辑层
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口
│   │   ├── entity/                   # 数据库实体类
│   │   ├── dto/                      # 数据传输对象（请求/响应）
│   │   └── utils/                    # JWT工具、文件上传等工具类
│   ├── src/main/resources/
│   │   ├── application.yml# 主配置（端口、数据源、Redis等）
│   │   ├── application.properties
│   │   └── db/migration/             # SQL 初始化脚本（Flyway可选）
│   └── pom.xml
├── frontend/                         # 前端 Vue3 工程
│   ├── public/
│   ├── src/
│   │   ├── api/                      # Axios 请求封装（按模块：user, experiment）
│   │   ├── assets/                   # 图片、字体等静态资源
│   │   ├── components/               # 公用组件（步骤栏、工具栏等）
│   │   ├── views/                    # 页面级组件
│   │   │   ├── WorkTicket.vue        # 工作票填写
│   │   │   ├── ToolSelection.vue     # 工器具选择
│   │   │   ├── ExperimentScene.vue   # 主实验画布（LeaferJS）
│   │   │   └── Admin/                # 管理员后台页面
│   │   ├── stores/                   # Pinia 状态管理
│   │   ├── router/                   # Vue Router 路由配置
│   │   └── utils/                    # 工具函数（截图、日期格式化等）
│   ├── package.json
│   └── vite.config.ts
├── docs/                             # 项目文档（需求、接口文档、设计图）
├── .gitignore
└── README.md