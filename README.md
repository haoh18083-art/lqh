# 校园医疗管理系统

当前可运行版本基于以下服务：
- `frontend-spring`：Vue 3 + TypeScript + Vite
- `backend-spring`：Spring Boot + MySQL + MongoDB
- `agents`：FastAPI + LangGraph

统一使用 [`docker-compose.linked-stack.yml`](/Users/issac.j/lqh/docker-compose.linked-stack.yml) 启动。

## 项目结构

```text
lqh/
├── frontend-spring/              # 前端
├── backend-spring/               # Spring Boot 后端
├── agents/                       # AI Agents 服务
├── docker/
│   ├── mysql/init.sql            # MySQL 完整初始化 + 测试数据
│   └── mongo/init.js             # Mongo 完整初始化 + 测试数据
├── backend/storage/docs/         # 诊断单 / 处方单文档挂载目录
└── docker-compose.linked-stack.yml
```

## Docker 启动

### 使用的 compose

```bash
docker compose -f docker-compose.linked-stack.yml up -d
```

### 完整启动顺序

容器依赖和健康检查顺序如下：

1. `mysql` 启动
2. `mongo` 启动
3. `mysql` healthcheck 通过
4. `mongo` healthcheck 通过
5. `backend-spring` 启动
6. `backend-spring` 的 `/health/ready` 通过
7. `agents` 启动
8. `agents` 的 `/health` 通过
9. `frontend-spring` 启动

也就是说，前端一定是在后端和 agents 都 ready 之后才会起来。

### 启动后的访问地址

- 前端：`http://localhost:3100`
- 后端：`http://localhost:8080`
- Agents：`http://localhost:8001`

常用接口：
- 后端健康检查：`http://localhost:8080/health/ready`
- Agents 健康检查：`http://localhost:8001/health`
- Agents 文档：`http://localhost:8001/docs`

## 数据库初始化与自动迁移

首次在任意机器上启动时，如果 MySQL / Mongo 的 Docker volume 为空，会自动执行：

- [`docker/mysql/init.sql`](/Users/issac.j/lqh/docker/mysql/init.sql)
- [`docker/mongo/init.js`](/Users/issac.j/lqh/docker/mongo/init.js)

初始化内容包括：
- MySQL 完整表结构
- Mongo 完整集合与索引
- 一套可直接联调的完整测试数据

因此在新机器上执行：

```bash
docker compose -f docker-compose.linked-stack.yml up -d
```

就会自动得到完整可用的测试环境，不需要手工导库。

如果你想强制重建一套全新测试数据，需要先删除数据库卷：

```bash
docker compose -f docker-compose.linked-stack.yml down
docker volume rm campus-medical_mysql_data campus-medical_mongo_data
docker compose -f docker-compose.linked-stack.yml up -d
```

## 默认测试账号

初始化脚本内置了三类角色账号，均可直接登录。

### 管理员

- 邮箱：`admin@campus-medical.com`
- 密码：`admin123`

### 学生

- 邮箱：`student@campus-medical.com`
- 密码：`test123456`

### 医生

- 邮箱：`doctor@campus-medical.com`
- 密码：`test123456`

附加回归测试账号：
- 邮箱：`reg_student@campus-medical.com`
- 密码：`test123456`

## 运行说明

### 查看容器状态

```bash
docker compose -f docker-compose.linked-stack.yml ps
```

### 查看日志

```bash
docker compose -f docker-compose.linked-stack.yml logs -f
```

只看某个服务：

```bash
docker compose -f docker-compose.linked-stack.yml logs -f frontend-spring
docker compose -f docker-compose.linked-stack.yml logs -f backend-spring
docker compose -f docker-compose.linked-stack.yml logs -f agents
```

### 停止服务

```bash
docker compose -f docker-compose.linked-stack.yml down
```

## 说明

- 前端容器以开发模式运行，源码挂载，支持热更新。
- `backend/storage/docs/` 是后端文档挂载目录，预约详情中的诊断单和处方单会从这里读取。
- 如果未配置有效的 `DEEPSEEK_API_KEY` 或系统里的 LLM 配置，`agents` 服务可以启动并通过健康检查，但 AI 对话接口不会真正调用外部模型。
