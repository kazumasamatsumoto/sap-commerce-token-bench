# S04 配送予定日の OCC 端点を足す(cs-backend)

- 担当: cs-backend
- 種類: 開発(ストーリー「配送予定日」の 2 番目)
- 前準備: なし(ストーリーで回すときは S03 の依頼書がある状態から始まる)
- 時間の上限: 60 分

## 依頼文
<!-- prompt -->
商品詳細に「お届け予定日」を出すための OCC を作ってください。cs-backend に頼んでください。
条件: 商品ごとに出荷までの日数(1〜5 日)を持たせ、サンプルデータの全商品に値を入れる。届け先の郵便番号から配送にかかる日数を足す(北海道と沖縄は 3 日、それ以外は 1 日。郵便番号が無ければ 1 日)。
最短の日は「今日 + 出荷までの日数 + 配送の日数」、最長の日は最短の 2 日後。
端点は GET /{baseSiteId}/products/{productCode}/delivery-estimate?postalCode=<7 桁の郵便番号>。
返す項目は productCode、leadTimeDays、earliestDate、latestDate(日付は yyyy-MM-dd)。
docs/occ-requests/delivery-estimate.md があれば、それに合わせてください。
items.xml、ImpEx、WsDTO、Populator、Controller と、Spock の単体テストまで作り、ant all と ant unittests が通るところまで確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、ビルドかテストが落ちるか、端点が期待どおりに返りませんでした。ant all、ant unittests と、端点の応答をもう一度確かめて直してください。
<!-- /retry -->
<!-- retry -->
まだ通りません。GET /electronics-spa/products/XYZ-000001/delivery-estimate?postalCode=0600001 が 200 で、leadTimeDays と earliestDate と latestDate を返すようにしてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_ant all unittests initialize && bench_restart_commerce && bench_occ_get "/products/XYZ-000001/delivery-estimate?postalCode=0600001" | grep -q '"earliestDate"' && bench_occ_get "/products/XYZ-000001/delivery-estimate?postalCode=0600001" | grep -q '"leadTimeDays"'
<!-- /check -->
