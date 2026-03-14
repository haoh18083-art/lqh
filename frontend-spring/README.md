# frontend-spring

基于 `Vue 3 + TypeScript + Vite + ECharts + SockJS/STOMP` 的前后端分离前端工程，用于替换现有 React 前端，并继续对接 `backend-spring` 与 `langgraph-app`。

## 目标

- 独立于 `backend-spring` 部署和运行
- 保持现有多角色路由和接口契约
- 所有构建、运行和测试都通过 Docker 完成
- 保留对 `langgraph-app` 的 Agent 能力接入

## Docker 运行

```bash
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml up -d --build backend-spring langgraph-app frontend-spring
```

## Docker 测试

```bash
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml run --rm frontend-spring npm run build
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml run --rm frontend-spring npm run test:run
docker exec campus-medical-frontend-spring node scripts/smoke-frontend-spring.mjs
docker exec campus-medical-frontend-spring node scripts/regression-frontend-spring.mjs
```
