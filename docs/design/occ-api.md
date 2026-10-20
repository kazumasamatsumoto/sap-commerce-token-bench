# 機能の設計書: OCC API の約束ごとと端点の一覧(チケット番号なし。基盤の設計)

読み手: OCC を呼ぶ・足す人(cs-frontend、cs-backend、cs-occ-liaison)、テストを書く人(cs-qa)
状態: 合意済み(2026-10-20)

パスの書き方: リポジトリの最上位からの相対。ただし `core-customize/hybris/bin/` の下は頭を省いて `platform/`、`platform-lite/`、`custom/` から書く。行番号は 2026-10-20 時点。

## 要約(用語なし、3 行まで)
画面は、決まった形の住所(URL)に問い合わせて、商品やカートの情報を受け取る。
住所の書き方、返す項目の量の選び方、ログインの扱い、失敗したときの返し方を、ここで決める。
問い合わせ先は全部で 50 本あり、正式な一覧は別の仕様書にある。

## 何を作るか
- 正式な仕様: `api/occ-openapi.yaml`(OpenAPI 3.0.3、50 本)(ソース: `api/occ-openapi.yaml:1-53`、実測: `operationId` を数えて 50)。
- この文書は約束ごとと一覧だけを書く。1 本ずつの詳しい説明は、必要になったら `kit` の API の型で `docs/api/<API>.md` に書く。
- 本物の SAP Commerce の OCC v2 の形に合わせる(推論。名前と形を本物に合わせて作ってあるため)。

### 約束ごと
| 項目 | 決まり | 根拠 |
|---|---|---|
| 頭の URL | `http://localhost:9002/occ/v2`。Controller には `/occ/v2` を除いた道を書く | (ソース: `platform-lite/src/de/hybris/platform/lite/config/OccWebConfiguration.java:28-61`) |
| base site | `{baseSiteId}` は `electronics-spa`。無いサイトは 400 `InvalidResourceError` | (ソース: `platform-lite/src/de/hybris/platform/commercewebservices/core/filter/OccContextInterceptor.java:51-60`) |
| 言語・通貨 | `?lang=` / `?curr=`。無ければサイトの既定の言語(ja)と、ストアの既定の通貨(JPY) | (ソース: 同 61-79 行) |
| `{userId}` | `current` = トークンの本人(トークンが無ければ 401)、`anonymous` = ゲスト、それ以外の uid = 本人か `ROLE_TRUSTED_CLIENT` だけ(他人なら 403) | (ソース: 同 84-121 行) |
| カートの id | ログインした人は `code` か `guid`、ゲスト(`anonymous`)は `guid` だけ | (ソース: `platform-lite/src/de/hybris/platform/commercefacades/order/impl/DefaultCartFacade.java:27-38`) |
| 権限 | Controller に `@Secured` を本物と同じ役の名前で書く(`ROLE_CUSTOMERGROUP`、`ROLE_TRUSTED_CLIENT`、`ROLE_CLIENT`、`ROLE_ANONYMOUS` など) | (ソース: `OccWebConfiguration.java:32-36`) |
| CORS | `http://localhost:*` と `http://127.0.0.1:*` から許す | (ソース: 同 70-75 行) |
| JSON | null の項目は出さない。日付は `yyyy-MM-dd'T'HH:mm:ssZ`。知らない項目は無視 | (ソース: `platform-lite/resources/application.properties:21-25`) |
| ページ送り | `currentPage`(既定 0)、`pageSize`(既定 20、1 〜 100 に丸める) | (ソース: `platform-lite/src/de/hybris/platform/commercewebservices/core/v2/controller/BaseController.java:14-29`) |

### fields(返す項目の量)
- `fields` には段階 `BASIC` / `DEFAULT` / `FULL` か、項目名の並び(`code,name,price(FULL)`)を書く。既定は `DEFAULT`(ソース: `BaseController.java:12-13`)。
- 段階が指す項目は WsDTO ごとに `fieldSetLevelMapping` で決める。標準は `platform-lite/resources/platformlite-web-spring.xml`(Product、Price、Image、Stock、Category、ProductSearchPage、Pagination、OrderEntry、Cart、Order)(ソース: 同ファイル 13-121 行)。
- 同じ `dtoClass` の mapping が複数あると、段階の中身が足し合わされる。自社の項目は自分の `*-web-spring.xml` に書く(ソース: `platform-lite/src/de/hybris/platform/webservicescommons/mapping/config/FieldSetLevelMapping.java:17`、`custom/xyzocc/web/webroot/WEB-INF/config/xyzocc-web-spring.xml:25-35`)。
- 例: Product の BASIC は標準の `code,name,url,purchasable` に `newArrival` が足される。DEFAULT には `brand(BASIC)`、FULL には `brand(DEFAULT),material` が足される(ソース: `platformlite-web-spring.xml:17-19`、`xyzocc-web-spring.xml:30-32`)。
- 同じ項目が 2 回出たら(`DEFAULT,entries(...)` など)中の項目を足し合わせる(ソース: `platform-lite/src/de/hybris/platform/webservicescommons/mapping/impl/DefaultDataMapper.java:127-155`)。
- 知らない項目名は黙って無視する(ソース: 同 105-109 行)。mapping が無い WsDTO は、どの段階でも全部の項目を返す(ソース: 同 91-98 行、`platformlite-web-spring.xml:5`)。
- 中に WsDTO を持つ項目を括弧なしで書くと、中は BASIC になる(ソース: `DefaultDataMapper.java:115-118`)。
- Data と WsDTO で名前が違う項目は読み替える(例: `ProductSearchPageData.results` → `products`)(ソース: `platform-lite/src/de/hybris/platform/lite/config/OccCommonConfiguration.java:37-46`)。

### OAuth(ログインのトークン)
- `POST http://localhost:9002/authorizationserver/oauth/token`(`/occ/v2` の外)。本文は `application/x-www-form-urlencoded`(ソース: `platform-lite/src/de/hybris/platform/lite/web/OAuthTokenController.java:19-45`、`api/occ-openapi.yaml:14-22`)。
- `grant_type=password` + `client_id=mobile_android` + `client_secret=secret` + `username` + `password` で顧客のトークン。役は `ROLE_CLIENT` と、所属グループごとの `ROLE_<グループの uid の大文字>`(例: `ROLE_CUSTOMERGROUP`)(ソース: `OAuthTokenController.java:52-73`)。
- `grant_type=client_credentials` + `client_id=trusted_client` で `ROLE_TRUSTED_CLIENT`(ソース: 同 74-78 行)。
- トークンの寿命は 43,200 秒(`oauth.token.validity.seconds`)。メモリだけに持つので、サーバーを止めると消える(ソース: `OccCommonConfiguration.java:48-52`、`OAuthTokenStore.java:9-12`)。
- `Authorization: Bearer <token>` で送る。知らないトークンは 401 `InvalidTokenError`。トークンが無いと `ROLE_ANONYMOUS` 扱い(ソース: `platform-lite/src/de/hybris/platform/lite/web/BearerTokenFilter.java:14-46`、`api/occ-openapi.yaml:24`)。

### エラーの形
`{"errors":[{"type":"...Error","message":"...","subject":"...","reason":"..."}]}`(`ErrorListWsDTO`)(ソース: `platform-lite/src/de/hybris/platform/commercewebservices/core/errors/OccExceptionHandler.java:31,123-134`)。

| 状態 | type | 主な原因 |
|---|---|---|
| 400 | `UnknownIdentifierError` | 無い商品・ブランドなど |
| 400 | `InvalidResourceError`(reason `UnknownResourceError`) | 無い base site |
| 400 | `CartError`(reason `notFound` / `invalid`) | カートが無い、明細を変えられない |
| 400 | `InvalidCartError` | 注文の確定に足りないものがある |
| 400 | `ValidationError` / `MissingServletRequestParameterError` | 入力の誤り、必須のパラメータが無い |
| 400 | `DuplicateUidError` / `CMSItemNotFoundError` | 登録済みの uid、無い CMS ページ |
| 401 / 403 | `UnauthorizedError` / `ForbiddenError` | ログインが要る / 権限が無い |
| 404 / 405 / 500 | `NotFoundError` / `HttpRequestMethodNotSupportedError` / 例外のクラス名 | 無い道、違うメソッド、そのほか |

(ソース: `OccExceptionHandler.java:37-121`)。本物と同じく「見つからない」は 404 でなく 400 で返す(推論)。

## 実現の仕方の仕分け
端点を足すのは「OCC の拡張(Commerce 側)」。手順は `docs/design/backend-extensions.md` の「項目を 1 つ足す手順」。

## 流れ(1 段 1 行。各段の責任)
| 段 | ファイル(行) | クラス / 関数 | 責任 |
|---|---|---|---|
| 1 | `platform-lite/src/de/hybris/platform/lite/web/BearerTokenFilter.java:24-46` | `doFilterInternal` | トークンを読み、役を付ける |
| 2 | `platform-lite/src/de/hybris/platform/commercewebservices/core/filter/OccContextInterceptor.java:47-82` | `preHandle` | サイト、言語、通貨、利用者を決める |
| 3 | 各 Controller(`@Secured`) | — | 役を確かめ、Facade を呼ぶ |
| 4 | `platform-lite/src/de/hybris/platform/webservicescommons/mapping/impl/DefaultDataMapper.java:50-61` | `map` | `fields` で絞って WsDTO にする |
| 5 | `platform-lite/src/de/hybris/platform/commercewebservices/core/errors/OccExceptionHandler.java` | `@RestControllerAdvice` | 例外をエラーの形にする |

## 使う API
50 本の一覧。道は `/occ/v2` の後ろ。「誰が」は `@Secured` の役(書いていないものは誰でも)。

| 分野 | メソッドと道 | 誰が | Controller(行) |
|---|---|---|---|
| BaseSites | GET `/basesites` | 誰でも | `BaseSitesController.java:23` |
| Products | GET `/{site}/products/search` | 誰でも | `ProductsController.java:39` |
| | GET `/{site}/products/suggestions` | 誰でも | 同 48 |
| | GET `/{site}/products/{productCode}` | 誰でも | 同 61 |
| | GET `/{site}/products/{productCode}/stock` | 誰でも | 同 68 |
| | GET `/{site}/products/{productCode}/reviews` | 誰でも | 同 76 |
| | POST `/{site}/products/{productCode}/reviews` | CUSTOMERGROUP、TRUSTED_CLIENT | 同 91-92 |
| Catalogs | GET `/{site}/catalogs` | 誰でも | `CatalogsController.java:36` |
| | GET `/{site}/catalogs/{catalogId}/{catalogVersionId}/categories/{categoryId}` | 誰でも | 同 63 |
| | GET `/{site}/categories/{categoryId}/products` | 誰でも | 同 72 |
| CmsPages | GET `/{site}/cms/pages` | 誰でも | `CmsPagesController.java:32` |
| | GET `/{site}/cms/pages/{pageId}` | 誰でも | 同 41 |
| | GET `/{site}/cms/components` | 誰でも | 同 48 |
| | GET `/{site}/cms/components/{componentId}` | 誰でも | 同 58 |
| Miscs | GET `/{site}/languages`、`/currencies`、`/countries`、`/titles`、`/cardtypes`(5 本) | 誰でも | `MiscsController.java:33-70` |
| Users | POST `/{site}/users`(登録) | CLIENT、TRUSTED_CLIENT、ANONYMOUS | `UsersController.java:33-34` |
| | GET / PATCH `/{site}/users/{userId}`(2 本) | CUSTOMERGROUP、TRUSTED_CLIENT | 同 57-66 |
| Address | GET / POST `/{site}/users/{userId}/addresses`(2 本) | CUSTOMERGROUP、TRUSTED_CLIENT | `AddressController.java:24-40` |
| | GET / PATCH / DELETE `/{site}/users/{userId}/addresses/{addressId}`(3 本) | 同上 | 同 49-74 |
| Carts | GET / POST `/{site}/users/{userId}/carts`(2 本) | CUSTOMERGROUP、TRUSTED_CLIENT、ANONYMOUS、CLIENT | `CartsController.java:40,53-62` |
| | GET / DELETE `.../carts/{cartId}`(2 本) | 同上 | 同 70-77 |
| | GET / POST `.../carts/{cartId}/entries`(2 本) | 同上 | 同 85-95 |
| | PATCH / DELETE `.../carts/{cartId}/entries/{entryNumber}`(2 本) | 同上 | 同 108-121 |
| | PUT / DELETE `.../carts/{cartId}/addresses/delivery`(2 本) | 同上 | 同 129-154 |
| | GET `.../carts/{cartId}/deliverymodes` | 同上 | 同 162 |
| | PUT / GET `.../carts/{cartId}/deliverymode`(2 本) | 同上 | 同 173-181 |
| | POST `.../carts/{cartId}/paymentdetails` | 同上 | 同 189 |
| | PUT `.../carts/{cartId}/email` | 同上 | 同 204 |
| Orders | POST `/{site}/users/{userId}/orders?cartId=` | CUSTOMERGROUP、TRUSTED_CLIENT、ANONYMOUS、CLIENT | `OrdersController.java:33-34` |
| | GET `/{site}/users/{userId}/orders` | CUSTOMERGROUP、TRUSTED_CLIENT | 同 43-44 |
| | GET `/{site}/users/{userId}/orders/{code}` | 同上 | 同 58-59 |
| | GET `/{site}/orders/{guid}`(ゲストの注文) | 誰でも(guid を知っている人) | 同 66-67 |
| OrgUnits | GET `/{site}/orgUnits`、`/{site}/orgUnits/{orgUnitId}`(2 本) | B2BCUSTOMERGROUP、TRUSTED_CLIENT | `OrgUnitsController.java:17-33` |
| Brands(自社) | GET `/{site}/brands`、`/{site}/brands/{brandCode}`(2 本) | 誰でも | `custom/xyzocc/web/src/com/xyz/occ/controllers/BrandsController.java:23-33` |

標準の Controller は `platform-lite/src/de/hybris/platform/commercewebservices/core/v2/controller/` にある。数えると 48 本 + Brands 2 本 = 50 本(ソース: 上の各行)。OAuth の端点と画像の `GET /medias/{name}` は `/occ/v2` の外で、50 本に入らない(ソース: `platform-lite/src/de/hybris/platform/lite/web/MediaController.java:14`)。

### 商品検索の `query`
`<自由語>:<並べ方>:<項目>:<値>:...` の形(例: `:price-asc:category:cat-01:brand:<code>`)。並べ方は `relevance`(code 順)、`name-asc`、`name-desc`、`topRated`、`price-asc`、`price-desc`。`sort=` を付けると、そちらが勝つ(ソース: `platform-lite/src/de/hybris/platform/commerceservices/search/impl/FlexibleSearchProductSearchService.java:24-63`)。仕組みは ADR 0003。

## SSR での扱い
SSR の中から呼ぶ先は `http://commerce:9002`、ブラウザからは `http://localhost:9002`。詳しくは `docs/design/storefront.md`。

## 翻訳キーと CMS
該当なし。言語別の値は `?lang=` で切り替わる(ソース: `OccContextInterceptor.java:61-69`)。

## テスト
`node tools/occ-smoke.mjs` が、誰でも見られるもの → ゲストの購入 → ログインした人の購入 → B2B の順に、期待の状態コード(200、201、400、401、403)を確かめる(ソース: `tools/occ-smoke.mjs:38-128`)。

## 根拠の確かさ
- (推論): 「見つからない」を 400 で返すのは本物と同じ。
- (推論): `relevance` は名前だけで、実際は code の順。Solr を使わないため(ADR 0003)。
- (未確認): CMS の端点は `fields` を受け取らない(ストアフロントは `fields=DEFAULT` を付けて呼ぶが、無視される)(ソース: `CmsPagesController.java:32-62`、`storefront/src/app/xyz-storefront-lib/occ/default-occ-config.ts:19-21`)。本物と同じ動きにするかを OCC の担当に確かめる。
