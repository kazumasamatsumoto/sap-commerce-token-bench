# 自社の部品(custom components)の一覧

ストアフロントの自社の部品(`storefront/src/app/custom/`)を、1 部品 1 行でまとめた表です。部品を足す・変える・消すときは、この表も同じ変更で直してください。

- 置き場所: `storefront/src/app/custom/<機能>/`。機能ごとに NgModule を 1 つ持ち、`custom/custom-features.module.ts` がまとめて import する。それを `src/app/spartacus/spartacus-features.module.ts` が import する(ソース)。
- 部品はすべて `standalone: false`、`ChangeDetectionStrategy.OnPush`、`.ts` / `.html` / `.scss` の 3 ファイル(ソース)。
- 文言は `src/assets/i18n/{ja,en}/custom.ts`(チャンク名は `xyz` で始まる)。`index.ts` で標準の `common.ts` とまとめる。en は `typeof ja` の型で、ja と同じ形かを確かめている(ソース)。
- 疑似ライブラリ(`xyz-storefront-lib/`)は変えていない(ソース)。

## 部品の種類

| 種類 | 意味 |
|---|---|
| outlet | `provideOutlet` で、決まった場所(Outlet)の前後に足す部品 |
| replace | `provideConfig({ cmsComponents })` で、標準の CMS 部品と差し替える部品。CMS のデータはそのまま読む |
| new | 自分で OCC を呼ぶ機能の部品(Adapter → Connector → Facade の順に呼ぶ) |
| browser-only | ブラウザでだけ動く機能の部品(localStorage など)。SSR では空か既定の表示にする |
| shared | 他の部品の中で使う小さな UI(Outlet や CMS には直接出ない) |

## Outlet の名前について

- `PDP.PRICE` / `PDP.ADDTOCART` … 標準の商品詳細(`ProductDetailsComponent`)の中の Outlet。部品には商品が渡されないので、`CurrentProductService`(`custom/shared/services/`)で今の商品を引く(ソース)。
- スロットの位置名(`Summary`、`TopContent`、`Footer` など)… どのページでも、その位置名のスロットがあれば出る。`TopContent` はカートとチェックアウトの両方にあるので、送料無料の部品は `/cart` のときだけ出す(ソース)。
- テンプレート名(`LandingPage2Template` など)… そのテンプレートのページ全体の前後に出る。`header` / `footer` はヘッダーとフッターの枠全体(ソース)。
- 同じ Outlet・同じ位置に複数の部品があるときは、`custom-features.module.ts` で import した順に出る(ソース)。

## 一覧(42 部品、機能のモジュール 12 個)

| 機能(モジュール) | 部品(クラス) | セレクター | 種類 | 出る場所 | OCC の呼び出し | data-testid |
|---|---|---|---|---|---|---|
| shared(CustomSharedModule) | RatingStarsComponent | `xyz-rating-stars` | shared | 評価の要約、レビュー、商品カードの中 | なし | なし |
| shared | PriceBadgeComponent | `xyz-price-badge` | shared | 札の部品の中(新着、プレミアムなど) | なし | なし |
| shared | QuantityStepperComponent | `xyz-quantity-stepper` | shared | お気に入りのパネルの中 | なし | なし |
| shared | ProductCardComponent | `xyz-product-card` | shared | 最近見た商品、お気に入りのパネルの中 | なし(商品は ProductService から) | なし |
| shared | SectionHeadingComponent | `xyz-section-heading` | shared | 各部品の見出し(h2) | なし | なし |
| shared | EmptyStateComponent | `xyz-empty-state` | shared | レビュー 0 件、お気に入り 0 件のとき | なし | なし |
| product-badges(ProductBadgesModule) | ProductBadgesComponent | `xyz-product-badges` | outlet | 商品詳細 `PDP.PRICE` / BEFORE | なし(商品は ProductService の `product`、fields=FULL) | `xyz-product-badges` |
| product-badges | BrandBadgeComponent | `xyz-brand-badge` | shared | 上の部品の中。ブランドのロゴと名前、PREMIUM / OUTLET の札 | なし | `xyz-brand-badge` |
| product-badges | NewArrivalBadgeComponent | `xyz-new-arrival-badge` | shared | 上の部品の中。`newArrival = true` のときだけ | なし | `xyz-new-arrival-badge` |
| product-badges | StockBadgeComponent | `xyz-stock-badge` | shared | 上の部品の中。`lowStock` のときだけ「残りわずか」 | なし | なし |
| product-info(ProductInfoModule) | TaxNoteComponent | `xyz-tax-note` | outlet | 商品詳細 `PDP.PRICE` / AFTER(「税込」) | なし | なし |
| product-info | ProductRatingSummaryComponent | `xyz-product-rating-summary` | outlet | 商品詳細 `PDP.PRICE` / AFTER(星、`averageRating`、`numberOfReviews`。押すとレビューの欄へ移る) | なし | `xyz-rating-summary` |
| product-info | MaterialInfoComponent | `xyz-material-info` | outlet | 商品詳細 `PDP.ADDTOCART` / AFTER(`material` があるときだけ) | なし | `xyz-material` |
| product-info | ShippingPolicyNoteComponent | `xyz-shipping-policy-note` | outlet | 商品詳細 `PDP.ADDTOCART` / AFTER(送料無料の条件と返品の決まり。固定の文言。届く日の目安は出さない) | なし | なし |
| product-info | ShareLinkComponent | `xyz-share-link` | outlet(browser-only) | 商品詳細 `PDP.ADDTOCART` / AFTER(URL をクリップボードにコピー) | なし | なし |
| product-reviews(ProductReviewsModule) | ProductReviewsComponent | `xyz-product-reviews` | new | 商品詳細 `Tabs` / AFTER(要約、並べ替え、最初の 5 件、もっと見る) | `GET /products/{productCode}/reviews?fields=DEFAULT`(端点の名前 `xyzProductReviews`) | `xyz-product-reviews` |
| product-reviews | ReviewSummaryComponent | `xyz-review-summary` | new | 上の部品の中(平均と星ごとの件数の棒) | なし(親が読んだものを使う) | なし |
| product-reviews | ReviewSortComponent | `xyz-review-sort` | new | 上の部品の中(新しい順 / 評価の高い順 / 低い順) | なし | なし |
| product-reviews | ReviewItemComponent | `xyz-review-item` | new | 上の部品の中(レビュー 1 件) | なし | `xyz-review-item` |
| brands(BrandsModule) | BrandStripComponent | `xyz-brand-strip` | new | トップ `LandingPage2Template` / AFTER(ブランドのロゴの帯。PREMIUM を先に並べる) | `GET /brands?fields=DEFAULT`(端点の名前 `xyzBrands`) | `xyz-brand-strip` |
| brands | BrandLogoComponent | `xyz-brand-logo` | new | 上の部品と「ブランドについて」の中。押すとブランド名で検索(`/search?query=`) | なし | なし |
| brands | BrandInfoComponent | `xyz-brand-info` | new | 商品詳細 `UpSelling` / BEFORE(ブランドについて) | `GET /brands/{brandCode}?fields=FULL`(端点の名前 `brand`。疑似ライブラリの既定) | `xyz-brand-info` |
| cart-extras(CartExtrasModule) | FreeShippingProgressComponent | `xyz-free-shipping-progress` | outlet | カート `TopContent` / AFTER(`/cart` で明細があるときだけ。5,000 円まであといくら) | なし(カートは ActiveCartService) | `xyz-free-shipping-progress` |
| cart-extras | SecureCheckoutNoteComponent | `xyz-secure-checkout-note` | outlet | カート `CenterRightContentSlot` / BEFORE | なし | なし |
| cart-extras | CartPointsComponent | `xyz-cart-points` | outlet | カート `CenterRightContentSlot` / AFTER(小計の 1% のポイント) | なし(カートは ActiveCartService) | `xyz-cart-points` |
| checkout-extras(CheckoutExtrasModule) | CheckoutStepsComponent | `xyz-checkout-steps` | outlet | チェックアウト `MultiStepCheckoutSummaryPageTemplate` / BEFORE(手続きの流れ。固定の表示) | なし | `xyz-checkout-steps` |
| checkout-extras | CheckoutHelpComponent | `xyz-checkout-help` | outlet | `SideContent` / AFTER(チェックアウトの横。問い合わせ先) | なし | なし |
| checkout-extras | OrderNextStepsComponent | `xyz-order-next-steps` | outlet | 注文確認 `OrderConfirmationPageTemplate` / AFTER(このあとの流れ) | なし | なし |
| content-replacements(ContentReplacementsModule) | XyzBannerComponent | `xyz-banner` | replace | `SimpleBannerComponent` の差し替え(トップの Section1 / 2A / 2B など)。見出しを画像に重ね、「詳しく見る」を足す。http(s) の URL は href で開く | なし | `xyz-banner` |
| content-replacements | XyzParagraphComponent | `xyz-paragraph` | replace | `CMSParagraphComponent` の差し替え(全ページ)。uid に Notice を含むか本文に「お知らせ」があれば目立たせる(role="note")。class `cx-paragraph` は残す | なし | なし |
| content-replacements | XyzFooterComponent | `xyz-footer` | replace | `FooterComponent`(flexType)の差し替え。リンクの 3 列と著作権の 1 行 | なし | `xyz-footer` |
| header-extras(HeaderExtrasModule) | NoticeBarComponent | `xyz-notice-bar` | outlet(browser-only の状態あり) | 全ページ `PreHeader` / BEFORE(お知らせの帯。閉じた状態は sessionStorage) | なし | `xyz-notice-bar` |
| header-extras | LanguageSwitcherComponent | `xyz-language-switcher` | outlet | 全ページ `SiteContext` / AFTER(画面の文言の言語。OCC に送る言語は変えない) | なし | なし |
| footer-extras(FooterExtrasModule) | TrustBadgesComponent | `xyz-trust-badges` | outlet | 全ページ `Footer` / BEFORE(お店の約束 3 つ) | なし | `xyz-trust-badges` |
| footer-extras | PaymentMethodsComponent | `xyz-payment-methods` | outlet | 全ページ `Footer` / AFTER | なし | なし |
| footer-extras | BackToTopComponent | `xyz-back-to-top` | outlet(browser-only) | 全ページ `Footer` / AFTER(画面に固定しない。他のボタンに重ねないため) | なし | `xyz-back-to-top` |
| footer-extras | CookieConsentComponent | `xyz-cookie-consent` | outlet(browser-only) | 全ページ `footer`(フッターの枠)/ AFTER。未回答のときだけ、描画のあとに出す(SSR では出さない)。答えは localStorage `xyz-cookie-consent` | なし | `xyz-cookie-consent` |
| recently-viewed(RecentlyViewedModule) | RecentlyViewedTrackerComponent | `xyz-recently-viewed-tracker` | browser-only | 商品詳細 `Summary` / AFTER(何も出さない。見た商品を localStorage `xyz-recently-viewed` に覚える) | なし | なし |
| recently-viewed | RecentlyViewedListComponent | `xyz-recently-viewed-list` | browser-only | 商品詳細 `CrossSelling` / AFTER と、トップ `Section5` / AFTER(今の商品は除く。最大 6 件) | なし(商品は ProductService の `product`) | `xyz-recently-viewed` |
| wishlist(WishlistModule) | WishlistButtonComponent | `xyz-wishlist-button` | browser-only | 商品詳細 `PDP.ADDTOCART` / AFTER(お気に入りの切り替え。aria-pressed) | なし | `xyz-wishlist-button` |
| wishlist | WishlistHeaderLinkComponent | `xyz-wishlist-header-link` | browser-only | 全ページ `MiniCart` / AFTER(件数と、パネルの開閉) | なし | `xyz-wishlist-toggle` |
| wishlist | WishlistPanelComponent | `xyz-wishlist-panel` | browser-only | 上の部品の中(数量を選んでカートに入れる、外す)。localStorage `xyz-wishlist` | なし(商品は ProductService、カートは ActiveCartService) | `xyz-wishlist-panel` |

種類ごとの数: outlet 18(うち browser-only の性質を持つもの 4)、replace 3、new 7、browser-only 5、shared 9(実測: 上の表を数えた)。

## サービス(部品ではないもの)

| 機能 | クラス | 役目 | OCC |
|---|---|---|---|
| shared | CurrentProductService | URL から今の商品を引く(Outlet の部品用) | ProductService 経由で `product`(fields=FULL) |
| shared | BrowserStorageService | localStorage の包み。SSR では読まず書かない | なし |
| product-reviews | ProductReviewAdapter / OccProductReviewAdapter / ProductReviewConnector / ProductReviewService | レビューの Adapter → Connector → Facade。商品ごとに 1 回だけ読み、失敗したら空 | `xyzProductReviews` |
| brands | BrandAdapter / OccBrandAdapter / BrandConnector / BrandService | ブランドの Adapter → Connector → Facade。一覧は 1 回、1 件はブランドごとに 1 回。失敗したら空 | `xyzBrands`、`brand` |
| recently-viewed | RecentlyViewedService | 最近見た商品(新しい順、最大 8 件) | なし |
| wishlist | WishlistService | お気に入り(最大 50 件) | なし |

OCC の端点の名前は、各機能のモジュールの `provideConfig({ backend: { occ: { endpoints } } })` で足している(`xyzProductReviews`、`xyzBrands`)。`brand` は疑似ライブラリの既定にある(ソース)。

## SSR の決まり(自社の部品で守っていること)

- `window`、`localStorage`、`sessionStorage`、`navigator` は、`isPlatformBrowser(PLATFORM_ID)` で確かめたあとか、`afterNextRender` の中でだけ触る(ソース)。
- ブラウザだけの状態(お気に入りの件数、最近見た商品、Cookie の同意)は、SSR とブラウザの最初の描画をそろえるため、描画のあと(`afterNextRender`)に出す(ソース)。そろえないと hydration(サーバーの HTML にブラウザの Angular をつなぐ処理)で食い違いが起きる(推論)。
- OCC を呼ぶ部品(レビュー、ブランド)は SSR の HTML にも入る(実測: `/product/XYZ-000001` の SSR の HTML に `xyz-product-reviews`、`xyz-brand-info` がある)。
- 配送予定日(届く日の目安)の部品は、ここには無い。計測のシナリオ S04〜S06 で足す(ソース: CLAUDE.md)。

## テスト

単体テスト(`npx ng test --watch=false`、vitest)は次の 10 ファイル(ソース)。

| ファイル | 確かめること |
|---|---|
| `shared/components/rating-stars/rating-stars.component.spec.ts` | 星の丸め方、読み上げの文言 |
| `shared/components/quantity-stepper/quantity-stepper.component.spec.ts` | 数の上下限、ボタン、ラベル |
| `cart-extras/free-shipping-progress/free-shipping-progress.component.spec.ts` | 残りの計算、ポイント、`/cart` だけで出す |
| `product-reviews/facade/product-review.service.spec.ts` | OCC の URL、1 回だけ読む、失敗で空 |
| `product-reviews/review-summary/review-summary.component.spec.ts` | 平均と内訳、並べ替え、読み上げの文言 |
| `brands/facade/brand.service.spec.ts` | OCC の URL、1 回だけ読む、失敗で undefined、並び順 |
| `content-replacements/xyz-paragraph/xyz-paragraph.component.spec.ts` | お知らせの見分け、HTML のまま出す |
| `recently-viewed/recently-viewed.service.spec.ts` | 覚える順、壊れた値、SSR で触らない |
| `wishlist/wishlist.service.spec.ts` | 切り替え、読み直し、SSR で書かない |
| `footer-extras/cookie-consent/cookie-consent.component.spec.ts` | 未回答なら出す、選ぶと覚える |

テスト用の設定は `custom/testing/custom-testing.ts` の `provideCustomTesting()`。E2E(Playwright)の 8 本は自社の部品の testid を使っていない(ソース)。

## モック

`storefront/mocks/handlers.ts` に、レビュー(`/products/:code/reviews`)とブランド(`/brands`、`/brands/:code`)の簡単な応答を足した(ソース)。中身は手で書いた少しの例で、本物の OCC から録ったものではない。

## 自社の OCC の設定(部品ではない)

| モジュール | 場所 | 中身 |
|---|---|---|
| `XyzOccConfigModule` | `storefront/src/app/custom/config/xyz-occ-config.module.ts` | 標準の端点 `productSearchList`(一覧と検索)の fields を上書きし、商品カードで使う `brand(BASIC)`、`newArrival`、`numberOfReviews` を足す。標準の項目(`price(FULL)`、`images(DEFAULT)`、`stock(FULL)` など)は落とさない |
