# S02 設計書のレビュー(cs-docs)

- 担当: cs-docs
- 種類: 開発(文書)
- 前準備: なし
- 時間の上限: 30 分

## 依頼文
<!-- prompt -->
来週から新しいメンバーが入るので、docs/design/checkout-flow.md を読んでもらう予定です。
cs-docs に頼んで、この設計書をレビューしてください。コードと食い違っている所、読み手が迷う所、足りない所を挙げ、
直すべき順に並べて docs/review/checkout-flow-review.md にまとめてください。設計書そのものはまだ直さないでください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
docs/review/checkout-flow-review.md ができていません。レビューの結果をこのファイルに書いてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
test -s docs/review/checkout-flow-review.md && git diff --quiet -- docs/design/checkout-flow.md
<!-- /check -->
