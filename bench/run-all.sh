#!/usr/bin/env bash
# 全部のシナリオを、1 回目 → 2 回目 → 3 回目の順に回す(人がいない間に回す用)。
#   bench/run-all.sh [回の数(既定 3)]
# 決まり:
#   - BENCH_DEADLINE(既定: 翌朝 09:00。"HH:MM" か "YYYY-MM-DD HH:MM")を過ぎたら、新しい計測を始めない
#   - 1 本ごとに results をコミットし、1 周ごとに push する(BENCH_PUSH=0 で push しない)
#   - 1 本が失敗しても止めずに次へ進む。失敗は bench/results/run-all.log に残す
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ROUNDS="${1:-3}"
LOG="$ROOT/bench/results/run-all.log"
ORDER="${BENCH_ORDER:-story S01 S02 S08 S09 S10 B01 B02 B03 B04 B05}"
DEADLINE_TEXT="${BENCH_DEADLINE:-09:00}"

deadline_epoch() {
  case "$DEADLINE_TEXT" in
    *-*) date -d "$DEADLINE_TEXT" +%s ;;
    *)
      local today; today=$(date -d "today $DEADLINE_TEXT" +%s)
      [ "$today" -gt "$(date +%s)" ] && echo "$today" || date -d "tomorrow $DEADLINE_TEXT" +%s ;;
  esac
}
DEADLINE=$(deadline_epoch)

say() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" | tee -a "$LOG"; }

commit_results() {
  node "$ROOT/bench/summarize.mjs" >/dev/null 2>&1
  git -C "$ROOT" add bench/results >/dev/null 2>&1
  git -C "$ROOT" -c user.name="${GIT_AUTHOR_NAME:-$(git -C "$ROOT" config user.name)}" commit -qm "bench: 計測の結果 ($1)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_018TvVU271qrDKy9eQUSZm5Y" >/dev/null 2>&1 || true
}

say "開始: ${ROUNDS} 回 × [$ORDER]、締め切り $(date -d "@$DEADLINE" '+%Y-%m-%d %H:%M')"
for round in $(seq 1 "$ROUNDS"); do
  for sid in $ORDER; do
    if [ "$(date +%s)" -ge "$DEADLINE" ]; then
      say "締め切りを過ぎたので止める(${round} 回目の $sid の前)"
      commit_results "締め切りで停止"
      [ "${BENCH_PUSH:-1}" = 1 ] && git -C "$ROOT" push -q origin HEAD 2>>"$LOG"
      exit 0
    fi
    say "開始 $sid ${round} 回目"
    start=$(date +%s)
    if bash "$ROOT/bench/run.sh" "$sid" "$round" >>"$LOG" 2>&1; then
      say "終了 $sid ${round} 回目($(( $(date +%s) - start )) 秒)"
    else
      say "失敗 $sid ${round} 回目(終了コード $?、$(( $(date +%s) - start )) 秒)。次へ進む"
    fi
    commit_results "$sid ${round} 回目"
  done
  say "${round} 回目が終わった"
  [ "${BENCH_PUSH:-1}" = 1 ] && git -C "$ROOT" push -q origin HEAD 2>>"$LOG" && say "push した"
done
say "全部終わった"
