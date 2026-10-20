#!/usr/bin/env bash
# 計測のシナリオを 1 回回して、トークンと時間を記録する。Git Bash(Windows)/ Mac / Linux で動く。
#
#   bench/run.sh <シナリオ> <回>              例: bench/run.sh B01 1
#   bench/run.sh story <回>                   S03 → S04 → S05 → S06 → S07 を 1 つの作業ツリーで続けて回す
#   bench/run.sh <シナリオ> <回> --after <作業ツリー>   前のシナリオの終わった作業ツリーから始める(S05〜S07 を 1 本だけ回すとき)
#   bench/run.sh clean                        作った作業ツリーを全部消す
#
# 変えられる値(環境変数):
#   BENCH_BASE         計測の前の状態のコミット(既定: bench/BASE_COMMIT に書いたもの。無ければ HEAD)
#   BENCH_CLAUDE       claude コマンド(既定: claude)
#   BENCH_MODEL        モデル(既定: 空 = 設定のまま。Bedrock なら人が ~/.claude/settings.json で決める)
#   BENCH_CLAUDE_FLAGS claude に渡す追加の引数(既定: 下の DEFAULT_FLAGS。人がいなくても止まらないよう、ツールは許し、
#                      キットの deny と ask は残す。ask に当たった操作は断られる。MCP は読ませない)
#   BENCH_MAX_RETRY    やり直しの文を送る回数の上限(既定: シナリオに書いた数)
#   BENCH_RUNS_DIR     作業ツリーを作る場所(既定: リポジトリの隣の <リポジトリ名>-runs)
#
# 記録: bench/results/cost-log.csv(会話ごとの累計。cs-cost の形。ラベルは S05/cs-frontend/2回目)、
#       bench/results/cost-by-agent.csv(本会話と担当 1 回ごとの内訳)、bench/results/runs.csv(時間、判定、条件)。
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RESULTS="$ROOT/bench/results"
# 作業ツリーはリポジトリの外に作る(中に作ると、Claude Code が親のフォルダの CLAUDE.md(計測台の指示)まで読んでしまうため)
RUNS_DIR="${BENCH_RUNS_DIR:-$(dirname "$ROOT")/$(basename "$ROOT")-runs}"
CLAUDE_BIN="${BENCH_CLAUDE:-claude}"
DEFAULT_FLAGS="--permission-mode acceptEdits --allowedTools Bash PowerShell Read Edit Write Glob Grep --strict-mcp-config"
CLAUDE_FLAGS="${BENCH_CLAUDE_FLAGS:-$DEFAULT_FLAGS}"
mkdir -p "$RESULTS" "$RUNS_DIR"

log() { printf '[bench %s] %s\n' "$(date +%H:%M:%S)" "$*" >&2; }
die() { log "ERROR: $*"; exit 2; }
# Node に渡すパスは Windows の形にする(/c/Users/... のままだと Node が C:\c\Users... と読むため)
winpath() { cygpath -m "$1" 2>/dev/null || echo "$1"; }

# ------------------------------------------------------------------ 判定で使う道具(シナリオの check から呼ぶ)
OCC="http://localhost:9002/occ/v2/electronics-spa"
SSR="http://localhost:4000"

bench_ant() { (cd core-customize/hybris/bin/platform && bash ./ant "$@"); }
bench_wait_url() { for _ in $(seq 1 90); do curl -fs -o /dev/null "$1" && return 0; sleep 2; done; return 1; }
bench_restart_commerce() { docker compose restart commerce >/dev/null 2>&1 && bench_wait_url "http://localhost:9002/occ/v2/basesites"; }
bench_restart_storefront() { docker compose --profile storefront up -d storefront >/dev/null 2>&1 && docker compose restart storefront >/dev/null 2>&1 && bench_wait_url "$SSR/"; }
bench_occ_get() { curl -s "$OCC$1"; }
bench_occ_status() { curl -s -o /dev/null -w '%{http_code}' "$OCC$1"; }
bench_ssr_get() { curl -s "$SSR$1"; }
bench_ssr_status() { curl -s -o /dev/null -w '%{http_code}' "$SSR$1"; }
bench_storefront_build() { (cd storefront && npm run build:ssr); }
bench_storefront_unit() { (cd storefront && npx ng test --watch=false); }
bench_e2e_real() { (cd storefront && npx playwright test --project=real --reporter=line); }
export -f bench_ant bench_wait_url bench_restart_commerce bench_restart_storefront bench_occ_get bench_occ_status bench_ssr_get bench_ssr_status \
  bench_storefront_build bench_storefront_unit bench_e2e_real
export OCC SSR

# ------------------------------------------------------------------ シナリオのファイルを読む
scenario_file() { ls "$ROOT"/bench/scenarios/"$1"-*.md 2>/dev/null | head -1; }
# <!-- name --> と <!-- /name --> の間を、n 番目(1 から)だけ取り出す
section() { awk -v name="$2" -v want="${3:-1}" '
  $0 ~ "<!-- " name " -->" { n++; if (n == want) { on = 1; next } }
  $0 ~ "<!-- /" name " -->" { if (on) exit; }
  on { print }' "$1"; }
count_section() { grep -c "<!-- $2 -->" "$1"; }
agent_of() { sed -n 's/^- 担当: \([a-z0-9-]*\).*/\1/p' "$1" | head -1; }
patch_of() { grep -o 'bench/bugs/[A-Za-z0-9._-]*\.patch' "$1" | head -1; }

# ------------------------------------------------------------------ 作業ツリーを作る
prepare_worktree() {
  local id="$1" base="$2"
  local wt="$RUNS_DIR/$id"
  [ -e "$wt" ] && die "$wt がもうあります(bench/run.sh clean で消すか、別の回の番号にしてください)"
  log "作業ツリーを作る: $wt (base $base)"
  git -C "$ROOT" -c core.autocrlf=false worktree add --detach "$wt" "$base" >/dev/null 2>&1 || die "git worktree add に失敗"
  (
    cd "$wt" || exit 2
    git config core.autocrlf false
    # 計測のときは案件の CLAUDE.md だけを読ませる。計測台の材料(答えが入っている)は外す。
    cp CLAUDE.project.md claude.md && rm -f CLAUDE.project.md doc.md
    # キットは作業ツリーの .claude/ に置く(本物の案件と同じ場所)。計測台の材料(答えが入っている)は外す。
    mkdir -p .claude && cp -r kit/.claude/. .claude/
    rm -rf bench kit .build-log
    git add -A >/dev/null && git -c user.name=bench -c user.email=bench@example.com commit -qm "bench: measurement start state" || true
  )
  echo "$wt"
}

setup_environment() {
  local wt="$1"
  log "ビルドと初期化(計測に入らない)"
  (cd "$wt" && bench_ant clean all initialize) >"$wt/../$(basename "$wt").setup.log" 2>&1 || log "初期化のログ: $wt/../$(basename "$wt").setup.log"
  (cd "$wt/storefront" && npm ci --no-audit --no-fund --prefer-offline >/dev/null 2>&1 && npm run build:ssr >/dev/null 2>&1) || log "storefront の準備に失敗"
  (cd "$wt" && docker compose up -d commerce >/dev/null 2>&1 && docker compose --profile storefront up -d storefront >/dev/null 2>&1 && bench_restart_commerce && bench_restart_storefront) \
    || log "サーバーの起動を確かめられませんでした"
}

apply_bug() {
  local wt="$1" sid="$2" file
  file="$(scenario_file "$sid")"
  local patch; patch="$(patch_of "$file")"
  [ -z "$patch" ] && return 0
  log "バグを当てる: $patch"
  git -C "$wt" apply "$ROOT/$patch" || die "パッチが当たりません: $patch"
  git -C "$wt" -c user.name=bench -c user.email=bench@example.com commit -qam "bench: inject $sid" >/dev/null
  case "$sid" in
    B01|B04) (cd "$wt" && bench_ant all && bench_restart_commerce) >/dev/null 2>&1 ;;
    B02)
      mkdir -p "$wt/logs"
      (cd "$wt" && bench_ant initialize) >"$wt/logs/initialize.log" 2>&1 || true ;;
    B03|B05) (cd "$wt" && bench_storefront_build && bench_restart_storefront) >/dev/null 2>&1 ;;
  esac
  if [ "$sid" = "B02" ]; then
    git -C "$wt" add -A logs && git -C "$wt" -c user.name=bench -c user.email=bench@example.com commit -qm "bench: initialize log" >/dev/null
  fi
}

place_materials() {
  local wt="$1" sid="$2"
  if [ "$sid" = "S09" ]; then
    mkdir -p "$wt/logs/incident-2026-10-19"
    cp "$ROOT"/bench/incidents/S09/* "$wt/logs/incident-2026-10-19/"
    git -C "$wt" add -A logs && git -C "$wt" -c user.name=bench -c user.email=bench@example.com commit -qm "bench: incident logs" >/dev/null
  fi
}

# ------------------------------------------------------------------ 1 本回す
run_one() {
  local wt="$1" sid="$2" round="$3"
  local file; file="$(scenario_file "$sid")"; [ -n "$file" ] || die "シナリオ $sid がありません"
  local agent; agent="$(agent_of "$file")"
  local label="$sid/$agent/${round}回目"
  local prompt; prompt="$(section "$file" prompt)"
  local check; check="$(section "$file" check)"
  local retries; retries="${BENCH_MAX_RETRY:-$(count_section "$file" retry)}"
  local out="$RUNS_DIR/$(basename "$wt").$sid.json"
  local minutes; minutes="$(sed -n 's/^- 時間の上限: \([0-9]*\) 分.*/\1/p' "$file" | head -1)"; minutes="${minutes:-60}"
  local deadline=$(( $(date +%s) + minutes * 60 ))

  log "開始 $label"
  local start; start=$(date +%s)
  # shellcheck disable=SC2086
  (cd "$wt" && timeout $(( minutes * 60 )) "$CLAUDE_BIN" -p "$prompt" --output-format json ${BENCH_MODEL:+--model "$BENCH_MODEL"} $CLAUDE_FLAGS) >"$out" 2>"$out.err"
  local exit_code=$?
  local session; session="$(sed -n 's/.*"session_id":"\([^"]*\)".*/\1/p' "$out" | head -1)"
  if [ -z "$session" ]; then
    # 時間切れなどで JSON が出なかったときは、作業ツリーのいちばん新しい会話の記録を使う(cs-cost が探す)
    log "会話の ID が JSON に無い(終了コード $exit_code)。作業ツリーのいちばん新しい会話の記録から数える"
  fi

  local passed=0 used=0
  while :; do
    if (cd "$wt" && eval "$check") >"$out.check.log" 2>&1; then passed=1; break; fi
    [ "$used" -ge "$retries" ] && break
    [ -z "$session" ] && break
    local left=$(( deadline - $(date +%s) )); [ "$left" -le 60 ] && { log "時間の上限なので、やり直しはしない"; break; }
    used=$((used + 1))
    log "判定が通らないので、やり直しの文 $used を送る"
    local retry; retry="$(section "$file" retry "$used")"
    # shellcheck disable=SC2086
    (cd "$wt" && timeout "$left" "$CLAUDE_BIN" -p "$retry" --resume "$session" --output-format json ${BENCH_MODEL:+--model "$BENCH_MODEL"} $CLAUDE_FLAGS) >>"$out" 2>>"$out.err"
  done
  local end; end=$(date +%s)

  # トークン(4 種、担当の分も含む)は cs-cost で記録する。作業ツリーの会話記録を読む。
  (cd "$wt" && node "$(winpath "$ROOT/kit/.claude/skills/cs-cost/scripts/cost.mjs")" ${session:+--session "$session"} --project-dir "$(winpath "$wt")" \
    --label "$label" --log "$(winpath "$RESULTS/cost-log.csv")" --by-agent "$(winpath "$RESULTS/cost-by-agent.csv")") | tee "$out.cost.md" >&2
  [ -n "$session" ] || session="$(sed -n 's/^会話 ID: \([^ ]*\).*/\1/p' "$out.cost.md" | head -1)"
  [ -f "$RESULTS/runs.csv" ] || echo "日時,ラベル,会話ID,秒,判定,やり直し,作業ツリー,claudeの版,モデル,引数" >"$RESULTS/runs.csv"
  local version; version="$("$CLAUDE_BIN" --version 2>/dev/null | head -1 | cut -d' ' -f1)"
  echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),$label,$session,$((end - start)),$([ $passed = 1 ] && echo 通過 || echo 不通過),$used,$(basename "$wt"),$version,${BENCH_MODEL:-既定},\"$CLAUDE_FLAGS\"" >>"$RESULTS/runs.csv"
  log "終了 $label: $((end - start)) 秒、判定 $([ $passed = 1 ] && echo 通過 || echo 不通過)、やり直し $used 回"
}

# ------------------------------------------------------------------ 入口
case "${1:-}" in
  selftest)
    # 自己点検(Claude は呼ばない): バグを当てると判定が通らず、戻すと通ることを、B01〜B05 で確かめる。
    BASE="${BENCH_BASE:-$(git -C "$ROOT" rev-parse HEAD)}"
    WT="$(prepare_worktree "selftest-$(date +%Y%m%d%H%M%S)" "$BASE")"
    setup_environment "$WT"
    ok=0; ng=0
    for sid in ${2:-B01 B02 B03 B04 B05}; do
      file="$(scenario_file "$sid")"; check="$(section "$file" check)"
      before="$(git -C "$WT" rev-parse HEAD)"
      apply_bug "$WT" "$sid"
      if (cd "$WT" && eval "$check") >"$WT.$sid.broken.log" 2>&1; then
        log "NG $sid: バグを当てても判定が通ってしまう($WT.$sid.broken.log)"; ng=$((ng + 1))
      else
        log "OK $sid: バグを当てると判定が通らない"
      fi
      git -C "$WT" reset -q --hard "$before"   # バグ(と、B02 の初期化のログ)を当てる前に戻す
      case "$sid" in
        B01|B04) (cd "$WT" && bench_ant all && bench_restart_commerce) >/dev/null 2>&1 ;;
        B03|B05) (cd "$WT" && bench_storefront_build && bench_restart_storefront) >/dev/null 2>&1 ;;
      esac
      if (cd "$WT" && eval "$check") >"$WT.$sid.fixed.log" 2>&1; then
        log "OK $sid: 戻すと判定が通る"; ok=$((ok + 1))
      else
        log "NG $sid: 戻しても判定が通らない($WT.$sid.fixed.log)"; ng=$((ng + 1))
      fi
    done
    log "自己点検の結果: 通過 $ok、問題 $ng"
    exit $([ "$ng" = 0 ] && echo 0 || echo 1) ;;
  clean)
    for wt in "$RUNS_DIR"/*/; do [ -d "$wt" ] && git -C "$ROOT" worktree remove --force "$wt"; done
    git -C "$ROOT" worktree prune
    log "作業ツリーを消しました"; exit 0 ;;
  ""|-h|--help)
    sed -n '2,16p' "$0"; exit 0 ;;
esac

SID="$1"; ROUND="${2:?回の番号を指定してください(例: 1)}"; AFTER=""
[ "${3:-}" = "--after" ] && AFTER="${4:?--after の作業ツリーを指定してください}"
BASE="${BENCH_BASE:-$(cat "$ROOT/bench/BASE_COMMIT" 2>/dev/null || git -C "$ROOT" rev-parse HEAD)}"

if [ "$SID" = "story" ]; then
  WT="$(prepare_worktree "story-r$ROUND" "$BASE")"
  setup_environment "$WT"
  for s in S03 S04 S05 S06 S07; do run_one "$WT" "$s" "$ROUND"; done
elif [ -n "$AFTER" ]; then
  [ -d "$AFTER" ] || die "$AFTER がありません"
  (cd "$AFTER" && docker compose up -d commerce >/dev/null 2>&1 && bench_restart_commerce && bench_restart_storefront)
  run_one "$AFTER" "$SID" "$ROUND"
else
  WT="$(prepare_worktree "$SID-r$ROUND" "$BASE")"
  setup_environment "$WT"
  apply_bug "$WT" "$SID"
  place_materials "$WT" "$SID"
  run_one "$WT" "$SID" "$ROUND"
fi
