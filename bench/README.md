# 計測の手順(bench/)

SAP Commerce Cloud の開発とバグ修正で、Claude Code の担当(サブエージェント)がどれだけトークンを使うかを測る。
決まりは最上位の `claude.md`(作るときの指示)の「計測のしかた」を正とする。ここは、その手順を実際に回すための説明。

## 中身

| 場所 | 中身 |
|---|---|
| `scenarios/` | シナリオ 1 本 1 ファイル(依頼文とやり直しの文は固定)。一覧と「1 ストーリー」の決まりは `scenarios/README.md` |
| `bugs/` | バグ修正のシナリオで当てるパッチ(B01〜B05) |
| `incidents/` | S09 で渡す障害のログ(`generate-s09-logs.mjs` で作る。種は固定) |
| `run.sh` | 1 本(またはストーリー)を回して記録する |
| `summarize.mjs` | 記録を集計して `results/summary.md` を書く |
| `rates.bedrock.json` | Bedrock の単価(**未確認**。AWS の料金ページで確かめてから使う) |
| `results/` | `cost-log.csv`(4 種のトークン)、`runs.csv`(時間、判定、やり直しの回数)、`summary.md` |
| `BASE_COMMIT` | 計測の前の状態のコミット。変えたら、それまでの結果と混ぜない |

## 回す前に(人がすること)

1. Claude Code の版を固定する(`claude --version` が `2.1.304`)。モデル、考える量(effort)、`~/.claude/settings.json` も固定し、`results/` に控えを残す。
2. Bedrock で回すときは、`CLAUDE_CODE_USE_BEDROCK=1`、`AWS_REGION`、使うモデルを各自のシェルか `~/.claude/settings.json` に入れる(資格情報は Claude に読ませない)。最初の 1 回で、`~/.claude/projects/` に使用量が残るかを確かめる(未確認)。
3. Docker Desktop を起動しておく。ポート 4000、5432、9002 を空けておく。
4. 作業ツリーはリポジトリの隣(`../<リポジトリ名>-runs/`)に作られる。中に作ると、Claude Code が親のフォルダの `claude.md`(計測台の指示)まで読んでしまうため。

## 回し方(Git Bash で、リポジトリの最上位から)

```bash
bench/run.sh B01 1        # バグ修正 B01 の 1 回目
bench/run.sh story 1      # ストーリー(S03 → S07)の 1 回目
bench/run.sh S08 1        # 性能の分析の 1 回目
bench/run.sh S05 2 --after ../token-use-runs/story-r2   # 前の作業ツリーから S05 だけ回す
node bench/summarize.mjs  # 集計(--rates global で Global の単価)
bench/run.sh clean        # 作業ツリーを全部消す(結果の CSV は消えない)
```

`run.sh` が 1 回ごとにすること:

1. `BASE_COMMIT` から作業ツリーを作り、`claude.md` を `CLAUDE.project.md` で置き換える。`bench/`、`kit/`、`doc.md`、`.build-log/` は外す(答えが入っているため)。
2. ビルド、`ant initialize`、ストアフロントの `npm ci` と `build:ssr`、Docker の起動(ここは計測に入らない)。
3. バグのパッチや、障害のログを置く(シナリオの「前準備」)。
4. `claude -p "<依頼文>"` を新しい会話で 1 回送る。
5. 判定のコマンドが通らなければ、やり直しの文を決まった順に送る(`--resume` で同じ会話に続ける)。
6. cs-cost で、その会話(担当の分を含む)の 4 種のトークンを `results/cost-log.csv` に、時間と判定を `results/runs.csv` に書く。ラベルは `S05/cs-frontend/2回目`。

## 気をつけること

- 各シナリオを 3 回以上回す。予算は最大値の側で見る。
- 同じ条件で入力が桁違いに跳ねる回があったら(以前の試しで 51 回中 10 回)、外すときは外した回と理由を `results/` に残す。
- 権限は既定で `--permission-mode acceptEdits`。キットの `.claude/settings.json` の allow / ask / deny がそのまま効く。ask に当たると止まるので、
  その回は「不通過」として残し、どの操作で止まったかを書く(止まったこと自体も、本物の案件で起きることの記録になる)。
- 疑似プラットフォームは本物の Commerce より小さい。本物では、プラットフォームのソースや jar を読みに行く、ビルドのログがずっと長い、起動に時間がかかる、の分だけ増える(推論)。報告では「補正の目安」として別の行に書く。
