#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILES=(-f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.spring.yml")

docker compose "${COMPOSE_FILES[@]}" up -d mysql mongo backend backend-spring frontend langgraph-app

"$ROOT_DIR/scripts/seed-regression-accounts.sh"

docker exec campus-medical-frontend sh -lc 'npm run test:run'
docker exec campus-medical-frontend sh -lc 'npm run test:regression:spring'
