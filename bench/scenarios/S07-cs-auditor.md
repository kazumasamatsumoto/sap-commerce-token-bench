# S07 S04〜S06 の差分のレビュー(cs-auditor)

- 担当: cs-auditor
- 種類: 開発(ストーリー「配送予定日」の 5 番目)
- 前準備: ストーリーで回す(S06 の結果がある状態から始まる)。1 本だけ回すときは `--after S06`
- 時間の上限: 30 分

## 依頼文
<!-- prompt -->
「お届け予定日」の作業が終わったので、プルリクエストを出す前に cs-auditor に頼んでレビューしてください。
対象は、まだコミットしていない作業ツリーの差分全部(git status と git diff で見えるもの)です。
案件のルール、セキュリティ、SSR、テストの抜けの観点で見て、指摘を重い順に docs/review/delivery-estimate-review.md にまとめてください。コードは直さないでください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
docs/review/delivery-estimate-review.md ができていません。レビューの結果をこのファイルに書いてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
test -s docs/review/delivery-estimate-review.md
<!-- /check -->
