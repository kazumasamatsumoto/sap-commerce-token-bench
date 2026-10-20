# B05 OCC の fields の指定漏れで、画面の値が出ない(cs-frontend)

- 担当: cs-frontend
- 種類: バグ修正
- 前準備: `bench/bugs/B05-fields-price.patch` を当てる(bench/run.sh が行う)
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
問い合わせが来ています:「カテゴリの一覧と検索の結果で、商品の価格が出ていない。商品をクリックして詳細を開くと価格は出る」。
cs-frontend に頼んで、原因を調べて直してください。直したら、npm run build:ssr と単体テストが通り、
SSR の /category/cat-01 の HTML に価格(¥)が入ることを確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、まだカテゴリの一覧に価格が出ていません。もう一度確かめて直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_storefront_build && bench_storefront_unit && bench_restart_storefront && bench_ssr_get /category/cat-01 | grep -Eq 'class="price"[^>]*>¥'
<!-- /check -->

## 答え(担当には渡さない)
bench/bugs/B05-fields-price.patch を参照(自社の設定で上書きした一覧の fields から price(FULL) が抜けている)。
