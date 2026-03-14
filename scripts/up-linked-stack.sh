#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.linked-stack.yml"

if docker ps -a --format '{{.Names}}' | grep -qx 'campus-langgraph-app'; then
  docker rm -f campus-langgraph-app >/dev/null
fi

docker compose -f "$COMPOSE_FILE" up -d --build mysql mongo backend-spring agents frontend-spring

docker compose -f "$COMPOSE_FILE" ps
