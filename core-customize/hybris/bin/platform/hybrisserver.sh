#!/usr/bin/env bash
# 疑似 Commerce を起動する。本物と同じく hybris/bin/platform で ./hybrisserver.sh。
#   ./hybrisserver.sh        … 起動して、ログを表示し続ける(Ctrl+C でログの表示だけ止まる)
#   ./hybrisserver.sh start  … 起動だけして戻る
#   ./hybrisserver.sh stop   … 止める
here="$(cd "$(dirname "$0")" && pwd)"
compose=(docker compose -f "$(cd "$here/../../../.." && pwd)/docker-compose.yml")
case "${1:-}" in
  stop)  exec "${compose[@]}" stop commerce ;;
  start) exec "${compose[@]}" up -d commerce ;;
  *)     "${compose[@]}" up -d commerce && exec "${compose[@]}" logs -f commerce ;;
esac
