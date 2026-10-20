# B04 @Secured の付け間違いで、ログインしていない人がブランド一覧を見られない(cs-backend)

- 担当: cs-backend
- 種類: バグ修正
- 前準備: `bench/bugs/B04-secured-brands.patch` を当てる(bench/run.sh が行う)
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
問い合わせが来ています:「ログインしないでトップを開くと、ブランドの一覧が出ない。ログインすると出る」。
ブラウザで見ると、GET /occ/v2/electronics-spa/brands が 401 を返しています。cs-backend に頼んで、原因を調べて直してください。
ブランドの一覧と詳細は、ログインしていない人にも見せるものです。直したら ant all と ant unittests が通ることを確かめてください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、まだログインしていない状態で /brands が 200 になりません。もう一度確かめて直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
bench_ant all unittests && bench_restart_commerce && test "$(bench_occ_status /brands)" = 200 && test "$(bench_occ_status /brands/brand-01)" = 200
<!-- /check -->

## 答え(担当には渡さない)
`BrandsController` に `@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })` が付いている。
