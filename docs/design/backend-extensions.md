# 機能の設計書: バックエンドの拡張(xyzcore / xyzfacades / xyzocc)と、項目の足し方(チケット番号なし。基盤の設計)

読み手: バックエンドを実装する人(cs-backend)、差分をレビューする人(cs-auditor)
状態: 合意済み(2026-10-20)

パスの書き方: リポジトリの最上位からの相対。ただし `core-customize/hybris/bin/` の下は頭を省いて `platform/`、`platform-lite/`、`custom/` から書く。行番号は 2026-10-20 時点。

## 要約(用語なし、3 行まで)
自社の機能は、標準の部品を書き換えずに、3 つの自社のフォルダに足していく。
データの形、データの扱い方、外に出す形を、それぞれ決まったフォルダの決まったファイルに書く。
ブランドの機能が、データから外に出すまでを一通り作った見本になっている。

## 何を作るか
### 3 つの拡張と、置くもの
| 拡張 | 依存 | 置くもの | 根拠 |
|---|---|---|---|
| `xyzcore` | `platformlite` | `xyzcore-items.xml`(型)、ImpEx、Service、DAO、インターセプター、動的属性、`xyzcore-spring.xml` | (ソース: `custom/xyzcore/extensioninfo.xml:3-5`) |
| `xyzfacades` | `xyzcore` | Facade、Populator、`xyzfacades-beans.xml`(Data)、`xyzfacades-spring.xml` | (ソース: `custom/xyzfacades/extensioninfo.xml:3-5`) |
| `xyzocc` | `xyzfacades` | OCC の Controller(`web/src`)、`xyzocc-beans.xml`(WsDTO)、`xyzocc-web-spring.xml`(fields の段階) | (ソース: `custom/xyzocc/extensioninfo.xml:3-6`) |

- 読み込む順は `core-customize/hybris/config/localextensions.xml` の順(`platformlite` → `xyzcore` → `xyzfacades` → `xyzocc`)(ソース: `core-customize/hybris/config/localextensions.xml:10-14`、`platform/build.gradle:15-27`)。
- `platform-lite` は標準のプラットフォームの代わりなので、案件では直さない(ソース: `CLAUDE.project.md:18,28`)。

### 層と、ブランドでの実例
| 層 | 一言で | ブランドでの実物 | 根拠 |
|---|---|---|---|
| 型(items.xml) | DB に持つデータの形 | `Brand`(表 `XyzBrands`、typecode 25000)、enum `BrandTier`、関連 `Brand2Products` | (ソース: `custom/xyzcore/resources/xyzcore-items.xml:8-52`) |
| Model | 型から生成される Java のクラス | `com.xyz.core.model.BrandModel`(`platform/bootstrap/gensrc/` に生成。直接書き換えない) | (ソース: `platform/build.gradle:41,78-89`) |
| DAO | FlexibleSearch で DB を引くクラス | `DefaultBrandDao`(`abstractItemDao` を親に持つ) | (ソース: `custom/xyzcore/src/com/xyz/core/brand/dao/impl/DefaultBrandDao.java:9-26`) |
| Service | 業務の決まりを持つクラス | `DefaultBrandService`。0 件なら `UnknownIdentifierException`、2 件以上なら `AmbiguousIdentifierException` | (ソース: `custom/xyzcore/src/com/xyz/core/brand/impl/DefaultBrandService.java:15-35`) |
| Data | 画面向けの入れ物(`*-beans.xml` から生成) | `BrandData`、`ProductData` に `brand` / `newArrival` / `material` を足す | (ソース: `custom/xyzfacades/resources/xyzfacades-beans.xml:5-18`) |
| Populator / Converter | Model の値を Data に写す | `BrandPopulator`、`xyzBrandConverter`、`XyzProductBrandPopulator` | (ソース: `custom/xyzfacades/src/com/xyz/facades/brand/populators/BrandPopulator.java:8-26`) |
| Facade | Controller が呼ぶ窓口 | `DefaultBrandFacade`(bean 名 `brandFacade`) | (ソース: `custom/xyzfacades/resources/xyzfacades-spring.xml:13-17`) |
| WsDTO | OCC の応答の形(`*-beans.xml` から生成) | `BrandWsDTO`、`BrandListWsDTO`、`ProductWsDTO` に 3 項目を足す | (ソース: `custom/xyzocc/resources/xyzocc-beans.xml:5-22`) |
| fieldSetLevelMapping | `fields=BASIC/DEFAULT/FULL` が指す項目 | `xyzBrandWsDTOFieldSetLevelMapping`、`xyzProductWsDTOFieldSetLevelMapping` | (ソース: `custom/xyzocc/web/webroot/WEB-INF/config/xyzocc-web-spring.xml:14-35`) |
| Controller | OCC の端点 | `BrandsController`(`GET /{baseSiteId}/brands`、`GET /{baseSiteId}/brands/{brandCode}`) | (ソース: `custom/xyzocc/web/src/com/xyz/occ/controllers/BrandsController.java:15-37`) |

### bean の書き方の決まり
- 標準の bean を差し替えるときは、同じ id を上書きせず alias を使う(例: `<alias name="defaultBrandService" alias="brandService"/>`)(ソース: `custom/xyzcore/resources/xyzcore-spring.xml:2-16`)。
- 標準の Converter に Populator を足すときは `modifyPopulatorList` を使う。`XyzProductBrandPopulator` は `productConverter`(詳細用)と `productSearchResultConverter`(一覧用)の両方に入れてある(ソース: `custom/xyzfacades/resources/xyzfacades-spring.xml:19-30`)。
- 2 つの Converter の中身: 詳細用は基本・価格・画像・在庫・カテゴリ・レビューの 6 つ、一覧用は基本・価格・画像・在庫の 4 つ(ソース: `platform-lite/resources/platformlite-spring.xml:144-170`)。
- Controller は `com.xyz.occ.controllers` の下に置けば読み込まれ、`/occ/v2` の下に出る(ソース: `custom/xyzocc/web/webroot/WEB-INF/config/xyzocc-web-spring.xml:2-12`、`platform-lite/src/de/hybris/platform/lite/config/OccWebConfiguration.java:56-61`)。
- 標準の bean の名前は本物に合わせてある(`modelService`、`flexibleSearchService`、`productConverter` など)(ソース: `platform-lite/src/de/hybris/platform/lite/config/PlatformLiteConfiguration.java:25-76`)。

### インターセプター(保存の前に値を確かめる・整える仕組み)
- 保存の順番は「PrepareInterceptor → 必須の属性の確かめ → ValidateInterceptor(unique の確かめを含む)→ DB に書く」(ソース: `platform-lite/src/de/hybris/platform/servicelayer/model/impl/DefaultModelService.java:32-35`)。
- `BrandValidateInterceptor` は、ブランドの code が `[a-z0-9][a-z0-9-]*` に合わなければ `InterceptorException` を投げる(ソース: `custom/xyzcore/src/com/xyz/core/interceptors/BrandValidateInterceptor.java:8-20`)。
- 登録は `InterceptorMapping` の bean に `typeCode="Brand"` を書く(ソース: `custom/xyzcore/resources/xyzcore-spring.xml:18-22`)。

### 動的属性(DB に持たず、読むたびに計算する項目)
- `Product.xyzNewArrival` は `persistence type="dynamic"`、`attributeHandler="xyzNewArrivalAttributeHandler"`、書き込み不可(ソース: `custom/xyzcore/resources/xyzcore-items.xml:64-68`)。
- 値は `XyzNewArrivalAttributeHandler.get` が出す。発売日(`xyzLaunchDate`)から 30 日未満なら true、発売前と発売日が無い商品は false(ソース: `custom/xyzcore/src/com/xyz/core/attributehandlers/XyzNewArrivalAttributeHandler.java:10-27`)。日数は bean の `newArrivalDays`(30)で変えられる(ソース: `custom/xyzcore/resources/xyzcore-spring.xml:24-27`)。
- 生成された `ProductModel.getXyzNewArrival()` は、bean 名で引いたハンドラーを呼ぶ(ソース: `platform/bootstrap/gensrc/de/hybris/platform/core/model/product/ProductModel.java:390-393`、`platform-lite/src/de/hybris/platform/core/model/ItemModel.java:133-138`)。

## 実現の仕方の仕分け
すべて「OCC の拡張(Commerce 側)」。`platform-lite` には手を入れない。理由は、本物の案件でも標準の拡張は直さず、自社の拡張から足すため(ソース: `CLAUDE.project.md:28`)。

## 流れ(1 段 1 行。各段の責任)
### `GET /occ/v2/electronics-spa/brands/{brandCode}?fields=FULL`
| 段 | ファイル(行) | クラス / 関数 | 責任 |
|---|---|---|---|
| 1 | `platform-lite/src/de/hybris/platform/commercewebservices/core/filter/OccContextInterceptor.java:47-82` | `preHandle` | サイトと利用者を決める |
| 2 | `custom/xyzocc/web/src/com/xyz/occ/controllers/BrandsController.java:32-36` | `getBrand` | Facade を呼び、`fields` で絞った `BrandWsDTO` にする |
| 3 | `custom/xyzfacades/src/com/xyz/facades/brand/impl/DefaultBrandFacade.java:21-25` | `getBrandForCode` | Model を Data に変える |
| 4 | `custom/xyzcore/src/com/xyz/core/brand/impl/DefaultBrandService.java:15-29` | `getBrandForCode` | 0 件・重複を例外にする |
| 5 | `custom/xyzcore/src/com/xyz/core/brand/dao/impl/DefaultBrandDao.java:11-19` | `findBrandsByCode` | `SELECT {pk} FROM {Brand} WHERE {code} = ?code` を流す |
| 6 | `custom/xyzfacades/src/com/xyz/facades/brand/populators/BrandPopulator.java:10-25` | `populate` | code、名前、説明、tier、ロゴを写す |
| 7 | `platform-lite/src/de/hybris/platform/webservicescommons/mapping/impl/DefaultDataMapper.java:50-61` | `map` | Data を Map にして、`fields` に無い項目を落とし、WsDTO にする |
| 8 | `platform-lite/src/de/hybris/platform/commercewebservices/core/errors/OccExceptionHandler.java:37-41` | `unknown` | 無い code は 400 の `UnknownIdentifierError` |

### 項目を 1 つ足す手順(items.xml から OCC まで)
例: 商品に「素材」`xyzMaterial` を足して、詳細の OCC で `material` として返す(すでに入っている実例)。

| # | やること | ファイル | 確かめ方 |
|---|---|---|---|
| 1 | 標準の型に属性を足す。`autocreate="false" generate="false"` で同じ code の itemtype を書く | `custom/xyzcore/resources/xyzcore-items.xml:54-63` | — |
| 2 | Model を作り直す | `platform/` で `./ant clean all`(Windows は `ant.cmd clean all`) | `BUILD SUCCESSFUL`。`gensrc` の `ProductModel` に getter が出る |
| 3 | DB に列を足す | `./ant updatesystem` | ログに `adding column ...` が出る(ソース: `platform-lite/src/de/hybris/platform/lite/persistence/SchemaManager.java:68-77`) |
| 4 | データを入れる | ImpEx の見出しに列を足す。サンプルデータは `tools/generate-sampledata.mjs` を直して作り直す | `./ant importImpex -Dresource=...` |
| 5 | Data に項目を足す。標準の Data でも、自分の `*-beans.xml` に同じ class の bean を書けば足される | `custom/xyzfacades/resources/xyzfacades-beans.xml:14-18` | `./ant all` で `ProductData.setMaterial` ができる |
| 6 | Populator で写す | `custom/xyzfacades/src/com/xyz/facades/product/populators/XyzProductBrandPopulator.java:22-23` | 単体テスト(Spock) |
| 7 | WsDTO に項目を足す | `custom/xyzocc/resources/xyzocc-beans.xml:18-22` | `./ant all` |
| 8 | `fields` の段階に入れる。同じ `dtoClass` の mapping を書くと、標準の段階に足される | `custom/xyzocc/web/webroot/WEB-INF/config/xyzocc-web-spring.xml:25-35` | `GET .../products/XYZ-000001?fields=FULL` に `material` が出る |
| 9 | 新しい端点が要るときだけ Controller を足す。`BaseController` を継ぎ、`getDataMapper().map(data, XxxWsDTO.class, fields)` で返す | `custom/xyzocc/web/src/com/xyz/occ/controllers/` | `node tools/occ-smoke.mjs` に呼び出しを足す |
| 10 | 仕様書を直す | `api/occ-openapi.yaml` | — |

- 手順 2 と 3 の順が逆だと、Model に無い列は作られない(推論)。`updatesystem` は表と列を足すだけで、消さない(ソース: `platform-lite/src/de/hybris/platform/lite/persistence/SchemaManager.java:15-17`)。
- 手順 8 を忘れると、`fields=DEFAULT` などの段階の指定では項目が出ない。段階の外の項目は落とされるため(ソース: `platform-lite/src/de/hybris/platform/webservicescommons/mapping/impl/DefaultDataMapper.java:89-103`)。
- 手順 6 を忘れると、項目は WsDTO にあっても値が null になり、JSON から消える(null は出さない設定)(ソース: `platform-lite/resources/application.properties:22`)。

## 使う API
- `GET /{baseSiteId}/brands`、`GET /{baseSiteId}/brands/{brandCode}`(`api/occ-openapi.yaml:1474-1529`)。約束ごとは `docs/design/occ-api.md`。

## SSR での扱い
バックエンドなので該当なし。ストアフロントはブランドを `brand(BASIC)` として一覧の fields で取る(ソース: `storefront/src/app/xyz-storefront-lib/occ/default-occ-config.ts:14-15`)。

## 翻訳キーと CMS
該当なし。ブランド名は items.xml で `localized:java.lang.String` なので、言語ごとに DB に持つ(ソース: `custom/xyzcore/resources/xyzcore-items.xml:34-36`)。

## テスト
| テスト | 種類 | 確かめること | 根拠 |
|---|---|---|---|
| `DefaultBrandServiceSpec` | `@UnitTest`(Spock) | 1 件・0 件・重複のとき | (ソース: `custom/xyzcore/testsrc/com/xyz/core/brand/impl/DefaultBrandServiceSpec.groovy:10-36`) |
| `BrandServiceIntegrationSpec` | `@IntegrationTest`(DB) | 保存して引ける、tier の既定値、大文字の code を断る | (ソース: `custom/xyzcore/testsrc/com/xyz/core/brand/BrandServiceIntegrationSpec.groovy:12-38`) |
| `XyzNewArrivalAttributeHandlerSpec` | `@UnitTest` | 発売日からの日数と結果 | (ソース: `custom/xyzcore/testsrc/com/xyz/core/attributehandlers/XyzNewArrivalAttributeHandlerSpec.groovy:12-19`) |
| `XyzProductBrandPopulatorSpec` | `@UnitTest` | 有効なブランドは写し、無効なブランドは写さない | (ソース: `custom/xyzfacades/testsrc/com/xyz/facades/product/populators/XyzProductBrandPopulatorSpec.groovy:11-34`) |

`xyzocc` の Controller のテストはまだ無い。OCC は `tools/occ-smoke.mjs` で確かめる(ソース: `tools/occ-smoke.mjs:63-65`)。詳しくは `docs/design/testing.md`。

## 根拠の確かさ
- (推論): items.xml を変えて `ant all` をせずに `ant updatesystem` を流すと、列は増えない。
- (未確認): `xyzLaunchDate` の説明に「発売日より前は一覧に出さない」とあるが、商品検索(`FlexibleSearchProductSearchService`)には発売日の条件が無い(ソース: `custom/xyzcore/resources/xyzcore-items.xml:60-63`、`platform-lite/src/de/hybris/platform/commerceservices/search/impl/FlexibleSearchProductSearchService.java:65-93`)。仕様として要るかを PO に確かめる。
