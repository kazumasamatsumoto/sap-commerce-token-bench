# 0003. 商品検索は Solr を使わず、FlexibleSearch(SQL)で行う
日付: 2026-10-20
状態: 承認

## 背景
- 本物の SAP Commerce は、商品検索とファセット(絞り込みの件数)に Solr を使う(推論。一般的な構成)。
- この案件の疑似環境(ADR 0001、0002)には Solr が無い。検索、カテゴリの一覧、並べ替え、カテゴリとブランドの絞り込みは要る(ソース: `storefront/e2e/search.spec.ts:5,15`)。
- 商品は 1,000 件と少ない(ソース: `tools/README.md:19`)。

## 選択肢
1. FlexibleSearch で検索する(`FlexibleSearchProductSearchService`): 利点 = 追加のコンテナが要らない。データの更新がすぐ検索に出る(索引の作り直しが要らない)。欠点 = 関連度の順が無い。件数が多いと遅い。本物の Solr の設定(`solrfacetsearch`、索引の作り直し)を触る作業が起きない。コスト = 小。
2. Solr(か OpenSearch)のコンテナを足し、索引を作る: 利点 = 本物に近い。欠点 = 起動が重く、索引の仕組みを自作する手間が大きい。コスト = 大。
3. 何もしない(検索の端点を固定の応答にする): 欠点 = 検索の画面の開発とテストができない。

## 決定
案 1。
1. 商品 1,000 件なら SQL の `ILIKE` で足りる(推論)。
2. 計測のシナリオ(S01〜S10、B01〜B05)に Solr を触るものが無い(ソース: `claude.md` の「計測のシナリオ」)。
3. 追加のコンテナ無しで、OCC の `/products/search` の形(query、facets、pagination、sorts)を本物と同じにできる(ソース: `api/occ-openapi.yaml:1193-1226`)。

## 根拠
- 実装: `core-customize/hybris/bin/platform-lite/src/de/hybris/platform/commerceservices/search/impl/FlexibleSearchProductSearchService.java:24-151`。
  - 自由語は code、名前、要約、メーカー名に `ILIKE '%語%'`(同 69-75 行)。
  - 承認済み(`approvalStatus = 'approved'`)の商品だけ(同 67 行)。
  - 並べ方: `relevance`(code 順)、`name-asc/desc`、`topRated`、`price-asc/desc`(同 43-51 行)。
  - ファセットはカテゴリとブランドの上位 10 件(同 95-132 行)。
- bean は `productSearchService` の alias で差し替えられる(ソース: `core-customize/hybris/bin/platform-lite/resources/platformlite-spring.xml:87-92`)。

## 影響
- 画面: 「関連度順」は名前だけで、実際は商品コードの順になる(ソース: 同 50 行)。
- 性能: 件数を増やす計測をするときは遅くなる(推論)。
- 他チーム: 本物の検索の設定(索引のプロパティ、ファセットの定義)を足す作業は、この案件では起きない。見積もりの補正に書く(推論)。
- 戻し方: Solr を入れるときは、`ProductSearchService` を実装した別の bean を作り、`<alias name="..." alias="productSearchService"/>` で差し替える。OCC と画面は変えない。

## 未決事項
- `Product.xyzLaunchDate`(発売日)の説明に「発売日より前は一覧に出さない」とあるが、検索の条件に入っていない(ソース: `core-customize/hybris/bin/custom/xyzcore/resources/xyzcore-items.xml:60-63`)。入れるかを PO が決める。
- サジェスト(`/products/suggestions`)は承認の状態を見ていない(ソース: `FlexibleSearchProductSearchService.java:141-151`)。合わせるかを決める。
