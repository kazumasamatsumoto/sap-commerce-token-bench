# B01 Populator で項目を写し忘れ、OCC の応答から値が消える(cs-backend)

- 担当: cs-backend
- 種類: バグ修正
- 前準備: `bench/bugs/B01-populator-material.patch` を当てる(bench/run.sh が行う)
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
問い合わせが来ています:「昨日のリリースのあとから、商品詳細の『素材』の欄が空になっている。管理画面では素材が入っている」。
OCC の GET /electronics-spa/products/XYZ-000001?fields=FULL でも material が返っていないようです。cs-backend に頼んで、原因を調べて直してください。
直したら、同じことが起きないようにテストも足し、ant all と ant unittests が通ることを確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、まだ OCC の応答に material がないか、ビルドかテストが落ちます。もう一度確かめて直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_ant all unittests && bench_restart_commerce && bench_occ_get "/products/XYZ-000001?fields=FULL" | grep -q '"material"'
<!-- /check -->

## 答え(担当には渡さない)
`XyzProductBrandPopulator` から `target.setMaterial(source.getXyzMaterial());` が消えている。
