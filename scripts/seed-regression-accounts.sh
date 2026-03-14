#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="$ROOT_DIR/docker/mysql/regression_seed.sql"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-campus-medical-mysql}"

if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  echo "MySQL container '$MYSQL_CONTAINER' is not running." >&2
  exit 1
fi

docker exec -i "$MYSQL_CONTAINER" sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$SQL_FILE"

docker exec "$MYSQL_CONTAINER" sh -lc '
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
    SELECT u.username, u.role, u.email, u.is_active
    FROM users u
    WHERE u.username IN (\"reg_admin\", \"reg_doctor\", \"reg_student\")
    ORDER BY FIELD(u.username, \"reg_admin\", \"reg_doctor\", \"reg_student\");
  "
'
