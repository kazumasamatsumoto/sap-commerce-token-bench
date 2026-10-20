# 機能の設計書: ストアフロントの構成(チケット番号なし。基盤の設計)

読み手: ストアフロントを実装する人(cs-frontend)、レビューする人(cs-auditor)、SSR の不具合を調べる人(cs-sre)
状態: 合意済み(2026-10-20)

パスの書き方: `storefront/` からの相対で書く(`lib/` は `src/app/xyz-storefront-lib/` の略。リポジトリの最上位のファイルは `../` を付ける)。行番号は 2026-10-20 時点。

## 要約(用語なし、3 行まで)
お店の画面は、サーバーで一度作ってからブラウザに渡し、ブラウザで動き出す。
どのページに何を並べるかは管理画面のデータで決まり、画面の部品は名前で差し替えたり足したりできる。
画面の部品は通信を直接せず、決まった順の窓口を通してデータをもらう。

## 何を作るか
Composable Storefront(Spartacus)と同じ構成を、自作の疑似ライブラリ `@xyz/storefront`(`lib/`)でまねる(ソース: `tsconfig.json:15-19`、ADR 0001)。自社の部品は `src/app/custom/` に置く。自社の部品の設計は `docs/design/custom-components.md` を見る。

### フォルダ
| 場所 | 役目 | 根拠 |
|---|---|---|
| `lib/` | Spartacus の代わりの標準ライブラリ。案件では直さない | (ソース: `lib/public_api.ts:1`、`../CLAUDE.project.md:20`) |
| `src/app/spartacus/` | Spartacus のアプリと同じ置き場所の設定(`spartacus-configuration.module.ts`、`spartacus-features.module.ts`) | (ソース: `src/app/spartacus/spartacus.module.ts:6-10`) |
| `src/app/custom/` | 自社の部品 | `docs/design/custom-components.md` |
| `src/assets/i18n/` | 翻訳(ja / en) | (ソース: `src/assets/i18n/index.ts:1-5`) |
| `mocks/` | MSW(ブラウザの通信を横取りして決まった応答を返す道具)のハンドラーと、OCC と同じ形の JSON | (ソース: `mocks/handlers.ts:13-17`) |
| `e2e/` | Playwright | `docs/design/testing.md` |

### 設定(Config)
- `provideConfig({...})` で渡した設定の断片を、既定値(`provideDefaultConfig`)のあとに深くマージして 1 つにする。`OccConfig`、`CmsConfig`、`LayoutConfig`、`I18nConfig` はどれも同じ `Config` を指す(ソース: `lib/config/config.ts:3-41`、`lib/occ/occ-config.ts:8`)。
- サイトの設定: OCC は `http://localhost:9002` + `/occ/v2/`、サイト `electronics-spa`、言語 ja / en、通貨 JPY / USD、翻訳の予備の言語は en(ソース: `src/app/spartacus/spartacus-configuration.module.ts:8-24`)。
- OCC の端点は名前で引く。URL と `fields` は `lib/occ/default-occ-config.ts:12-46` に端点ごとに書く。`OccEndpointsService.buildUrl` が `baseUrl + prefix + baseSite + 端点` を組み立てる(ソース: `lib/occ/occ-endpoints.service.ts:17-38`)。OCC の URL をコードに直書きしない(ソース: `../CLAUDE.project.md:33`)。

### CMS の対応表(cmsComponents)
- CMS のコンポーネントの typeCode(`CMSFlexComponent` は flexType)と Angular の部品の対応表は `lib/cms-components/cms-components.module.ts:46-65`。標準は 16 種類(ソース: 同)。
- 差し替えるときは、同じキーで `provideConfig({ cmsComponents: { キー: { component: 自社の部品 } } })` する。`disabled: true` で表示しない(ソース: `lib/cms/config/cms-config.ts:4-21`)。
- `ComponentWrapperDirective` が対応表を引き、`CmsComponentData`(`uid` と `data$`)を渡して部品を作る。対応が無ければ何も描かない(ソース: `lib/cms/page/component-wrapper.directive.ts:7-32`)。

### Outlet(決まった場所に部品を足す仕組み)
- `<ng-template [cxOutlet]="'名前'">標準の中身</ng-template>` の場所に、`provideOutlet({ id, component, position })` で部品を足す。position は `REPLACE`(既定)、`BEFORE`、`AFTER`(ソース: `lib/outlet/outlet.directive.ts:5-32`、`lib/outlet/outlet.service.ts:6-17`)。
- 使える名前: ページのテンプレート名、スロットの位置名(ソース: `lib/layout/page-layout.component.ts:18`、`lib/layout/page-slot.component.ts:13`)、`PDP.PRICE`、`PDP.ADDTOCART`(ソース: `lib/cms-components/product/product-details.component.ts:20,37`)。

### 通信の層(Adapter → Connector → Facade)
| 層 | 一言で | カートでの実物 |
|---|---|---|
| Adapter | OCC を呼ぶクラス。抽象クラスと `Occ*` の実装に分ける | `CartAdapter` / `OccCartAdapter`(ソース: `lib/cart/connectors/cart.adapter.ts:4-10`、`lib/cart/occ/occ-cart.adapter.ts:8-37`) |
| Connector | Adapter を包む。差し替えの継ぎ目 | `CartConnector`(ソース: `lib/cart/connectors/cart.connector.ts:6-29`) |
| Store(NgRx) | 状態を持つ。Effects が Connector を呼ぶ | `cart.actions.ts`、`cart.effects.ts`、`cart.reducer.ts` |
| Facade | 部品が呼ぶ窓口 | `ActiveCartService`(ソース: `lib/cart/facade/active-cart.service.ts:11-109`) |

- 部品から HttpClient を直接呼ばない(ソース: `../CLAUDE.project.md:29`、`lib/product/facade/product.service.ts:8-11`)。
- Adapter の実装は `StorefrontModule.forRoot()` で登録する(ソース: `lib/storefront.module.ts:34-46`)。商品は `ProductModule` で登録する(ソース: `lib/product/product.module.ts:9-12`)。
- NgRx の状態は `cart`(今のカート、読み込み中、最後の変更)と `product`(商品ごとの詳細、検索結果)(ソース: `lib/cart/store/cart.reducer.ts:5-32`、`lib/product/store/product.reducer.ts:5-45`)。
- カートの明細を変えたら、Effects がカートを読み直す(`entryChangeSuccess` → `loadCart`)(ソース: `lib/cart/store/cart.effects.ts:80-86`)。

### 翻訳(i18n)
- キーは `chunk.key` の形。今の言語で無ければ予備の言語、それも無ければ `[キー]` を出す(抜けが画面で見える)(ソース: `lib/i18n/translation.service.ts:5-31`)。
- 部品では `{{ 'product.addToCart' | cxTranslate }}`、値の差し込みは `{{count}}`(ソース: `lib/i18n/translate.pipe.ts:4-11`)。
- 文言は `src/assets/i18n/ja/common.ts`、`src/assets/i18n/en/common.ts`。直書きしない(ソース: `../CLAUDE.project.md:31`)。
- 今の言語は設定の最初の言語(ja)で、画面で切り替える仕組みはまだ無い(ソース: `lib/i18n/translation.service.ts:12`)。

## 実現の仕方の仕分け
- ページに何を並べるか: CMS(ImpEx の `10-cms.impex`)。
- 標準の部品の見た目や動きを変える: ストアフロントのコード(cmsComponents の差し替え)。
- 決まった場所に足す: ストアフロントのコード(Outlet)。
- 新しいデータが要る: OCC の拡張(`docs/design/backend-extensions.md`)と、端点の設定の追加。

## 流れ(1 段 1 行。各段の責任)
### ページとスロット
| 段 | ファイル(行) | クラス / 関数 | 責任 |
|---|---|---|---|
| 1 | `src/app/app.html` / `lib/layout/storefront.component.ts:7-17` | `StorefrontComponent` | header / main / footer の外枠を作る |
| 2 | `lib/cms/page/page-context.service.ts:22-36` | `PageContextService.fromUrl` | `/product/:code` → ProductPage、`/category/:code` → CategoryPage、`/search` → `search`、`/` → `homepage`、それ以外はパスをラベルにした ContentPage |
| 3 | `lib/cms/facade/cms.service.ts:15-27` | `CmsService.getPage` | ページを 1 回だけ取ってため置く。失敗したら空のページにして画面を止めない |
| 4 | `lib/layout/page-layout.component.ts:34-44` | `PageLayoutComponent` | テンプレート名から、並べるスロットの位置を `LayoutConfig` で引く |
| 5 | `lib/layout/page-slot.component.ts:28-30` | `PageSlotComponent` | その位置のスロットのコンポーネントを順に描く |
| 6 | `lib/cms/page/component-wrapper.directive.ts:19-32` | `ComponentWrapperDirective` | 対応表で部品を作る |

### テンプレートごとのスロット
| テンプレート | LayoutConfig の並び(`lib/layout/layout-config.ts:14-28`) | CMS のデータにある位置(`tools/generate-sampledata.mjs:306-316`) |
|---|---|---|
| header(全テンプレート共通) | PreHeader, SiteContext, SiteLinks, SiteLogo, SearchBox, SiteLogin, MiniCart, NavigationBar | PreHeader, SiteLogo, SearchBox, MiniCart, NavigationBar(+ Footer)を全テンプレートに割り当て |
| LandingPage2Template(ホーム) | Section1, 2A, 2B, 3, 4, 5 | 同じ |
| ContentPage1Template(案内・キャンペーン 92 ページ) | Section2A, Section2B | 同じ |
| ProductDetailsPageTemplate | Summary, UpSelling, CrossSelling, Tabs, PlaceholderContentSlot | Summary, UpSelling, CrossSelling, Tabs |
| ProductListPageTemplate | ProductLeftRefinements, ProductListSlot | 同じ |
| SearchResultsListPageTemplate | Section2, ProductLeftRefinements, SearchResultsListSlot, Section4 | SearchResultsListSlot |
| CategoryPageTemplate | Section1, Section2, Section3 | ProductLeftRefinements, ProductListSlot(食い違い。下の「根拠の確かさ」) |
| CartPageTemplate | TopContent, CenterRightContentSlot, EmptyCartMiddleContent, BottomContentSlot | TopContent, CenterRightContentSlot, EmptyCartMiddleContent |
| MultiStepCheckoutSummaryPageTemplate | TopContent, BodyContent, SideContent, BottomContent | BodyContent, SideContent |
| OrderConfirmationPageTemplate | BodyContent, SideContent | BodyContent |

- データに無い位置は、空のスロットとして何も描かない(ソース: `lib/cms/facade/cms.service.ts:29-35`)。
- 商品とカテゴリのページは、code によらず既定のページ(`productDetails`、`productList`)が返る(ソース: `../core-customize/hybris/bin/platform-lite/src/de/hybris/platform/cmsfacades/pages/impl/DefaultCmsPageFacade.java:44-47`)。

## 使う API
| 端点の名前 | 道と fields | 呼ぶ Adapter |
|---|---|---|
| `pages` | `cms/pages?fields=DEFAULT` | `OccCmsPageAdapter` |
| `productSearchList` | `products/search?fields=products(code,name,summary,price(FULL),images(DEFAULT),stock(FULL),averageRating,brand(BASIC),newArrival),facets,...` | `OccProductAdapter.search` |
| `product` | `products/${productCode}?fields=FULL` | `OccProductAdapter.load` |
| `createCart` / `cart` / `addEntries` / `updateEntries` / `removeEntries` | `users/${userId}/carts...` | `OccCartAdapter` |
| `cartEmail` / `setDeliveryAddress` / `deliveryModes` / `setDeliveryMode` / `paymentDetails` / `placeOrder` / `guestOrder` | `docs/design/checkout-flow.md` | `OccCheckoutAdapter` |

(ソース: `lib/occ/default-occ-config.ts:12-46`)。約束ごとは `docs/design/occ-api.md`。

## SSR での扱い
- すべての URL を要求ごとにサーバーで描く(ソース: `src/app/app.routes.server.ts:3-9`)。
- SSR の中では OCC の接続先を環境変数 `OCC_BASE_URL` から読む(Docker では `http://commerce:9002`)。画像の URL の頭はブラウザから届く `http://localhost:9002` のまま(ソース: `src/app/app.module.server.ts:8-17`、`../docker-compose.yml:56-58`)。
- Express は `/occ` を Angular に回さず 404 を返す。回すと SSR が自分を呼び続けて止まるため(ソース: `src/server.ts:27-33`)。
- SSR を受け付けるホスト名は `localhost` と `127.0.0.1`(`angular.json` の `security.allowedHosts`)(ソース: `angular.json:44-49`)。別の名前で開くと SSR されない(推論。手順書 `docs/runbook/local-environment.md` の T5)。
- **ブラウザだけで使えるもの(window、document、localStorage)は、`isPlatformBrowser` で確かめてから触る**(ソース: `../CLAUDE.project.md:32`)。実例: `ActiveCartService` はゲストのカートの guid を localStorage に覚えるが、SSR ではカートを読まない(ソース: `lib/cart/facade/active-cart.service.ts:11-18,95-108`)。このため SSR の HTML のミニカートは 0 で、ブラウザで動き出してから正しい数になる(推論)。
- ブラウザ側は SSR の HTML を引き継ぐ(hydration、`withEventReplay`)(ソース: `src/app/app-module.ts:11`)。引き継ぐ前に押した操作は取りこぼされることがある(ソース: `e2e/support.ts:6-13`)。
- Transfer State(SSR で取ったデータをブラウザに渡す仕組み)は、この案件では明示して使っていない。SSR とブラウザで OCC の URL が違う(`commerce:9002` と `localhost:9002`)ので、ブラウザで同じデータを取り直す(推論)。
- CMS の取得に失敗しても、空のページで HTML を返す(ソース: `lib/cms/facade/cms.service.ts:6-9,21`)。

## 翻訳キーと CMS
| キー / コンポーネント | ja | en |
|---|---|---|
| `product.addToCart` | カートに入れる | Add to cart |
| `searchBox.placeholder` | 商品を探す | Search products |
| `miniCart.label` | カート({{count}} 点) | Cart ({{count}} items) |
| `productList.results` | {{count}} 件 | {{count}} results |

(ソース: `src/assets/i18n/ja/common.ts:7-26`、`src/assets/i18n/en/common.ts:8-26`)。キーの一覧はファイルそのものを正とする。CMS のコンポーネントの一覧は上の「CMS の対応表」。

### MSW のモック
- `npm run start:mock` で :4200 に、SSR なしの開発サーバーを出す。`environment.mock.ts` に差し替わり、起動前に MSW を動かす(ソース: `package.json` の scripts、`angular.json:75-87`、`src/main.ts:5-12`)。
- MSW 3 では、2 系の `onUnhandledRequest` が `onUnhandledFrame` に変わった。モックに無い要求(画像など)は外へ流す(`bypass`)(ソース: `mocks/browser.ts:4-12`)。
- 応答の元は本物の OCC から `npm run mocks:capture`(`mocks/capture-fixtures.mjs`)で録った JSON。商品は `XYZ-000001` 〜 `XYZ-000012` の 12 件(ソース: `mocks/capture-fixtures.mjs:1-29`)。
- カートと注文はハンドラーの中で覚え、sessionStorage にも置く(読み直しで消えないように)(ソース: `mocks/handlers.ts:42-53`)。税は合計 × 10 / 110 で、本物と同じ出し方(ソース: `mocks/handlers.ts:60-77`)。

## テスト
- 単体: `npx ng test --watch=false`(Vitest)(ソース: `angular.json:106-108`、`../CLAUDE.project.md:51`)。
- E2E: `docs/design/testing.md`。

## 根拠の確かさ
- (推論): SSR の HTML のミニカートは 0 で、動き出してから正しい数になる。
- (推論): SSR とブラウザで OCC の URL が違うため、ブラウザで同じ GET を取り直している。性能を見るとき(S08)に確かめる。
- (未確認): `CategoryPageTemplate` の並び(Section1〜3)が CMS のデータ(ProductLeftRefinements、ProductListSlot)と合っていない。今はこのテンプレートのページ(`categoryLanding`)が既定のページでないので画面に出ない(ソース: `tools/generate-sampledata.mjs:366-368`)。直すかどうかを CMS の担当に確かめる。
- (未確認): `src/server.ts:28-29` の説明に「MSW のモックがここより先に応答する」とあるが、MSW はブラウザの中だけで動き、モックの構成は SSR しない(`angular.json:79`)。説明の直しが要る。
- (未確認): `spartacus-features.module.ts` の説明に「部品は 2 段目で足す」とあるが、`imports` は空で、部品は `lib/cms-components` から入る(ソース: `src/app/spartacus/spartacus-features.module.ts:3-10`)。
