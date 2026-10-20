# S01 B2B の承認機能の設計と ADR(cs-architect)

- 担当: cs-architect
- 種類: 開発(ストーリー「B2B の承認」の設計)
- 前準備: なし
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
B2B のお客さまから「一定の金額を超える注文は、社内の承認者が承認してから確定したい」という要望が来ています。
cs-architect に頼んで、この承認機能の設計をしてください。いまの B2B の仕組み(組織、承認者のグループ、注文の状態)を調べたうえで、
どこまで標準の仕組みで実現でき、どこに自社の作り込みが要るかを整理し、設計書を docs/design/ に、決めたことを ADR として docs/adr/ に書いてください。
実装はまだしません。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
設計書か ADR のどちらかが docs/ にできていません。docs/design/ に設計書を、docs/adr/ に ADR を書いてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
git status --porcelain -- docs/design | grep -q . && git status --porcelain -- docs/adr | grep -q .
<!-- /check -->
