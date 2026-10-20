# S05 商品詳細に配送予定日を出す(cs-frontend)

- 担当: cs-frontend
- 種類: 開発(ストーリー「配送予定日」の 3 番目)
- 前準備: ストーリーで回す(S04 の結果がある状態から始まる)。1 本だけ回すときは `--after S04` で S04 の終わった作業ツリーから始める
- 時間の上限: 60 分

## 依頼文
<!-- prompt -->
商品詳細の価格の下に「お届け予定日: 10月23日(木)〜10月25日(土)」のように出してください。cs-frontend に頼んでください。
OCC は GET /{baseSiteId}/products/{productCode}/delivery-estimate?postalCode= が使えます(返す項目は productCode、leadTimeDays、earliestDate、latestDate)。
郵便番号は、まだ画面で入れてもらわないので、指定なしで呼んでください。
まず MSW のモックを作って画面を仕上げ、そのあと本物の OCC につないで確かめてください。SSR で壊れないこと、文言は翻訳キーにすること。
npm run build:ssr と単体テストが通るところまで確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、ビルドかテストが落ちるか、商品詳細の SSR の HTML に予定日が出ていませんでした。もう一度確かめて直してください。
<!-- /retry -->
<!-- retry -->
まだ通りません。npm run build:ssr、npx ng test --watch=false が通り、/product/XYZ-000001 の SSR の HTML にお届け予定日が入るようにしてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_storefront_build && bench_storefront_unit && bench_restart_storefront && bench_ssr_get /product/XYZ-000001 | grep -q 'お届け予定日'
<!-- /check -->
