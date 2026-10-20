# 計測台に元からある食い違い(わざと直していない)

本物の案件にもある程度の食い違いとして残している。S02(設計書のレビュー)、S07(差分のレビュー)、S10(全体の監査)で、
担当がどれだけ見つけたかを見るときの参考にする。作業ツリーには bench/ を入れないので、担当には見えない。
2026-10-20 に、設計書を書いた担当が見つけたもの(コードは直していない)。

| # | 内容 | 場所 |
|---|---|---|
| 1 | items.xml の説明は「発売日より前は一覧に出さない」だが、検索の条件に xyzLaunchDate が無い(発売前の商品が約 5% 出る) | `xyzcore-items.xml`、`FlexibleSearchProductSearchService` |
| 2 | CategoryPageTemplate のスロット(Section1〜3)が、CMS のデータ(ProductLeftRefinements、ProductListSlot)と合っていない | `layout-config.ts`、`10-cms.impex` |
| 3 | server.ts のコメント「MSW のモックがここより先に応答する」は誤り(MSW はブラウザの中だけ) | `storefront/src/server.ts` |
| 5 | essential-data.impex の先頭のコメントに「配送方法」とあるが、入っていない | `platform-lite/resources/platformlite/import/common/essential-data.impex` |
| 6 | CMS の端点が fields を受け取らない(ストアフロントは fields=DEFAULT を付けて呼ぶ) | `CmsPagesController` |
| 7 | 並べ替えの relevance は実際には商品コードの順。サジェストは承認の状態を見ない | `FlexibleSearchProductSearchService` |
| 8 | 通貨を変えると、カートの値段と丸めの桁がずれる(推論) | `DefaultCommerceCartService`、`DefaultCalculationService` |
| 9 | SSR とブラウザで OCC の URL が違うので、HTTP の転送キャッシュが効かず、同じデータを 2 回取っている可能性(推論) | `app.module.server.ts` |
| 10 | xyzocc の Controller のテストが無い | `xyzocc/web/testsrc` |
