#!/bin/sh
set -eu

COMPOSE_FILES="-f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml"

docker compose $COMPOSE_FILES up -d --build backend-spring langgraph-app frontend-spring
./scripts/seed-regression-accounts.sh
docker compose $COMPOSE_FILES run --rm frontend-spring npm run build
docker compose $COMPOSE_FILES run --rm frontend-spring npm run test:run
docker exec campus-medical-frontend-spring node scripts/smoke-frontend-spring.mjs
docker exec campus-medical-frontend-spring node scripts/regression-frontend-spring.mjs
