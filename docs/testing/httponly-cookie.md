# HttpOnly Cookie 验证记录

验证日期：2026-10-23。分支：`fixbug/httponly-cookie`。

## 自动化验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 后端格式化与测试 | `cd backend` 后执行 `mvn -o spotless:apply test` | 36 项通过：35 项新增认证测试、1 项原有上下文测试 |
| 前端格式化 | `cd frontend` 后执行 `npm run format` | 已执行；与本功能无关的历史格式差异未纳入修改 |
| 前端测试 | `cd frontend` 后执行 `npm test` | 35 项通过 |
| 前端构建 | `cd frontend` 后执行 `npm run build` | 通过；仍有原有的依赖注释和资源体积提示 |
| 差异检查 | `git diff --check` | 通过 |

首次使用时需要联网安装依赖，Maven 命令可去掉 `-o`。
前端测试依赖使用兼容项目既有 Node.js 20/22 要求的 Vitest 4 和 jsdom 26。

后端新增测试使用真实 JWT、Cookie 处理、来源过滤器、鉴权拦截器和控制器，模拟业务服务。
覆盖 Cookie 属性和 24 小时有效期、响应不含 token、登录失败、非法及过期凭证、Bearer 被拒绝、
角色拦截及上下文清理、全部实验接口、幂等退出、改密成功与失败，以及来源校验和 CORS 预检。
`Secure=true` 时签发与清除的属性一致性也有测试。

前端测试覆盖旧 token 清理、无 token 的登录响应、并发恢复、恢复失败重试、
迟到响应不覆盖新身份、退出成功与失败、角色守卫、401 分流及业务身份失效处理。

## 浏览器验证结果

使用 Playwright 驱动真实 Chromium，前端通过 Vite 代理访问测试后端。

| 场景 | 观察结果 |
| --- | --- |
| 学生从登录表单提交用户名、密码及验证码 | 登录返回 200，进入 `/experiment`，响应体无 token |
| 检查浏览器 Cookie | `auth_token` 为 HttpOnly，Path 为 `/api`，SameSite 为 Lax，有效期约 24 小时；本地 HTTP 的 Secure 为 false |
| 检查浏览器本地存储 | `localStorage.token` 不存在 |
| 刷新实验页面 | `/api/auth/me` 返回 200，恢复正确学生身份 |
| 在 `/api/auth/me` 页面读取 `document.cookie` | 无法读取 `auth_token`；检查路径与 Cookie Path 匹配 |
| 浏览器自动携带 Cookie 请求实验接口 | 返回 200，无需 Authorization 头 |
| 调用退出并再次查询当前用户 | 退出返回 200，Cookie 删除，随后 `/api/auth/me` 返回 401 |
| 退出后直接进入实验页面 | 回到登录页 |
| 教师访问 `/admin/profile` | 通过 Cookie 恢复正确教师身份和页面权限 |
| 管理端点击退出，模拟一次网络中断 | 提示“退出失败，请重试”，保留页面与 Cookie |
| 恢复网络后再次点击退出 | 返回 200，Cookie 删除，页面跳转到登录页 |

## 复现隔离浏览器环境

测试服务器仅位于 `src/test`，不会包含在生产应用中，也不会连接数据库。
它替换业务服务，保留真实 HTTP、Cookie、来源校验、JWT 与控制器链路。

1. 确保本地 8080 端口空闲，在 `backend/` 执行：

   ```bash
   mvn spring-boot:test-run -Dspring-boot.run.main-class=com.example.experiment.support.CookieBrowserTestServer
   ```

2. 在 `frontend/` 执行 `npm run dev`，浏览器访问 `http://localhost:5173`。
3. 测试服务器专用账号为 `cookie_student` 或 `cookie_teacher`，密码均为 `cookie123`。表单验证码照常填写。
4. 教师登录后可直接访问 `/admin/profile` 检查用户恢复和右上角退出菜单；该测试服务器只提供认证与实验接口，不提供管理数据接口。
5. 完成验证后停止两个本地服务。

## 验证边界与部署

本次环境未配置数据库密码，因此未执行真实数据库账号校验或实验数据持久化联调。
浏览器验证中的用户服务和实验服务为测试替身；生产数据库业务行为不属于本次验证结论。
没有执行正式 HTTPS 代理部署验证。

正式部署需设置 `AUTH_COOKIE_SECURE=true`，并将 `AUTH_ALLOWED_ORIGINS` 配置为实际站点来源，
保持前端与 `/api` 同源。已有用户升级后需重新登录。
JWT 仍沿用无状态机制，退出或改密只清除当前浏览器 Cookie，不撤销已复制的 JWT。
