---
name: commit
description: 分析工作区改动并按功能点差异化拆分提交，提交信息遵循项目 CLAUDE.md 的 Git 规范（<type>(<scope>): 中文描述）。当用户说「提交」「commit」「push 前提交」或准备提交代码时使用。禁止模型自动触发（有副作用），仅用户手动调用。
argument-hint: [message]
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git add:*), Bash(git commit:*), Bash(git branch:*)
---

# 中文 Git 提交（差异化拆分）

## 动态上下文

- 当前分支: !`git branch --show-current`
- 改动概览: !`git status --short`
- 变更统计: !`git diff --stat HEAD`

> 完整 diff 按需查看：先看 `git diff HEAD` 定位每个文件的改动性质，再对不确定的文件用 `git diff HEAD -- <file>` 深入确认。改动量大时不要一次性注入全部 diff。

## 任务流程

### 1. 差异化分析

读取动态上下文，将全部改动**按逻辑功能点分组**：

- 每个独立的功能 / Bug 修复 / 重构 / 文档变更 = 一组（一个提交）
- 同一文件内若有多个互不相关的改动（例如前端样式 + 后端接口），也要拆分到不同组
- 同一功能的连带改动（如一个功能的前后端文件）归入同一组
- 无法归类到任何功能点的杂项 → 归入 `chore` 组
- 未跟踪的新文件根据其用途归入对应组

### 2. 展示提交计划（必须等待确认）

以表格展示分组方案，**等待用户确认或调整后才能提交**：

| 组 | 涉及文件 | type(scope) | 提交信息 |
|----|----------|-------------|----------|

⚠️ 项目规范（CLAUDE.md 五、Git 提交规范）：**禁止在用户未确认的情况下提交代码**。用户可调整分组或提交信息。

### 3. 逐组提交

用户确认后，按顺序逐组执行（一组 = 一次 commit）：

```bash
git add <该组的所有文件>
git commit -m "<type>(<scope>): <中文描述>"
```

- 若用户提供了 `$ARGUMENTS`，作为该提交的补充说明或覆盖信息
- 一次只 add 当前组的文件，**严禁 `git add -A` 一次性全部暂存**

### 4. 验证并汇报

全部提交完成后展示结果：

- `git log --oneline -N`（N = 本次提交次数）
- `git status --short` 确认工作区干净

## 提交信息规范（项目 CLAUDE.md）

格式：`<type>(<scope>): <subject>` —— **subject 使用中文**

### type 枚举

| type | 说明 | type | 说明 |
|------|------|------|------|
| `feat` | 新功能 | `perf` | 性能优化 |
| `fix` | Bug 修复 | `chore` | 构建/工具/依赖 |
| `refactor` | 代码重构 | `docs` | 文档更新 |
| `style` | 格式调整（不影响逻辑） | | |

### scope 枚举（必填）

| scope | 涉及内容 | scope | 涉及内容 |
|-------|----------|-------|----------|
| `frontend` | 前端通用变更 | `component` | Vue 组件相关 |
| `backend` | 后端通用变更 | `api` | API 接口层 |
| `auth` | 认证/登录/注册 | `config` | 配置文件 |
| `experiment` | 实验业务相关 | `naming` | 文件/路由/变量命名 |
| `scene` | 场景页面（全景/柜体/热区） | `archive` | 存档/恢复/草稿 |
| | | `deps` | 依赖管理 |

### 示例

```bash
git commit -m "feat(experiment): 添加未完成实验恢复功能"
git commit -m "fix(auth): 修复注册后未清除验证码状态"
git commit -m "style(backend): 统一应用 Google Java Format"
git commit -m "chore(frontend): 添加 Prettier 格式化配置"
```

## 提交前检查清单

- [ ] 代码已格式化（前端 `npm run format` / 后端 `mvn spotless:apply`）
- [ ] 改动已由用户测试确认通过
- [ ] 提交计划已展示并获用户确认
- [ ] 一个功能点 = 一次提交，不攒批
