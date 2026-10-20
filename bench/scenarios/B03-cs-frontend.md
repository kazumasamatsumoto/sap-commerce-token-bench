# B03 SSR のときだけ落ちる画面(cs-frontend)

- 担当: cs-frontend
- 種類: バグ修正
- 前準備: `bench/bugs/B03-ssr-window.patch` を当てる(bench/run.sh が行う)
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
運用の担当から報告です:「商品詳細のページを直接開く(URL を貼る、再読み込みする)と 500 エラーになる。
トップから商品をクリックして移ったときは表示される。」
cs-frontend に頼んで、原因を調べて直してください。直したら、npm run build:ssr と単体テストが通り、
SSR で /product/XYZ-000001 が 200 で返ることを確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、まだ /product/XYZ-000001 を直接開くとエラーになります。SSR で 200 が返るまで直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_storefront_build && bench_storefront_unit && bench_restart_storefront && bench_ssr_status /product/XYZ-000001 | grep -q '^200$'
<!-- /check -->

## 答え(担当には渡さない)
bench/bugs/B03-ssr-window.patch を参照(自社の部品のどれかが、サーバーで window を直接触る)。
