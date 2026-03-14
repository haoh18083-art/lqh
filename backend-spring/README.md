# backend-spring

Spring Boot 2.7.0 + Java 8 的后端迁移骨架。当前只完成工程结构、基础依赖、容器化入口和迁移文档，不包含现有 FastAPI 业务逻辑的 Java 实现。

## 目录约定

```text
backend-spring/
├── pom.xml
├── Dockerfile
├── src/main/java/com/campusmedical
│   ├── common
│   ├── config
│   ├── security
│   ├── module
│   │   ├── auth
│   │   ├── department
│   │   ├── doctor
│   │   ├── student
│   │   ├── appointment
│   │   ├── schedule
│   │   ├── consultation
│   │   ├── medicine
│   │   ├── medicalrecord
│   │   ├── document
│   │   ├── system
│   │   ├── audit
│   │   ├── notification
│   │   ├── pharmacy
│   │   └── chat
│   └── infrastructure
│       └── persistence
│           ├── mysql
│           └── mongo
│
├── src/main/resources
│   ├── application.yml
│   └── db/migration
└── src/test
```

## Docker 方式运行

依赖全部走 Docker，不需要在宿主机安装 JDK 或 Maven。

启动 MySQL、MongoDB 和 Spring 骨架服务：

```bash
docker compose -f docker-compose.yml -f docker-compose.spring.yml up -d mysql mongo backend-spring
```

在容器中执行测试：

```bash
docker compose -f docker-compose.yml -f docker-compose.spring.yml run --rm backend-spring mvn test
```

构建 Maven 产物：

```bash
docker compose -f docker-compose.yml -f docker-compose.spring.yml run --rm backend-spring mvn -DskipTests package
```

## 当前阶段边界

- 已保留 `/api/v1` 作为统一上下文路径。
- 已预留 JWT、CORS、全局异常包装、WebSocket 配置。
- 已按 FastAPI 现有领域拆分模块目录，便于一比一迁移路由、服务、实体、DTO。
- 尚未实现任何业务接口，因此前端还不能切流到该服务。
