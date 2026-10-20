# B02 ImpEx の書き間違いで初期化が途中で落ちる(cs-backend)

- 担当: cs-backend
- 種類: バグ修正
- 前準備: `bench/bugs/B02-impex-enum-typo.patch` を当て、ant initialize を流して失敗のログを `logs/initialize.log` に置く(bench/run.sh が行う)
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
検証環境を作り直そうとしたら、ant initialize が途中で失敗しました。そのときの出力を logs/initialize.log に置いています。
cs-backend に頼んで、原因を調べて直してください。ant initialize はこちらで流すので、直したあとはそのファイルの ImpEx だけを ant importImpex で入れ直して、
エラーが出ないことを確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
こちらで ant initialize を流し直しましたが、まだ失敗します。もう一度確かめて直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_ant initialize
<!-- /check -->

## 答え(担当には渡さない)
`04-brands.impex` の tier の値が `PREMUIM` になっている(正しくは `PREMIUM`)。
