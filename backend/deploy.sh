#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.deploy.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker 未安装，请先安装 Docker Desktop 或 Docker Engine。" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "docker 当前不可用，请先启动 Docker。" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "未检测到 docker compose / docker-compose。" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
  python3 - <<'PY' "$ENV_FILE"
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
text = path.read_text()
text = re.sub(
    r'^DATABASE_URL=.*$',
    'DATABASE_URL=postgres://visionhub:visionhub@postgres:5432/visionhub?sslmode=disable',
    text,
    flags=re.MULTILINE,
)
text = re.sub(r'^REDIS_ADDR=.*$', 'REDIS_ADDR=redis:6379', text, flags=re.MULTILINE)
text = re.sub(r'^KAFKA_BROKERS=.*$', 'KAFKA_BROKERS=redpanda:9092', text, flags=re.MULTILINE)
path.write_text(text)
PY
  echo "已创建 backend/.env，并写入容器内默认地址。"
fi

"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

HEALTH_URL="http://localhost:3000/healthz"
echo "等待后端健康检查通过：$HEALTH_URL"
for _ in {1..30}; do
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    echo "backend 已启动。"
    echo "健康检查：$(curl -fsS "$HEALTH_URL")"
    exit 0
  fi
  sleep 2
done

echo "backend 启动超时，请执行以下命令查看日志：" >&2
echo "${COMPOSE_CMD[*]} -f $COMPOSE_FILE --env-file $ENV_FILE logs --tail=200" >&2
exit 1
