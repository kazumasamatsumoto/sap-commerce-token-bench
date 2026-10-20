# 機能の設計書: テストの段階と回し方(チケット番号なし。基盤の設計)

読み手: テストを書く・回す人(cs-qa、cs-backend、cs-frontend)、差分をレビューする人(cs-auditor)
状態: 合意済み(2026-10-20)

パスの書き方: リポジトリの最上位からの相対。ただし `core-customize/hybris/bin/` の下は頭を省いて `platform/`、`platform-lite/`、`custom/` から書く。行番号は 2026-10-20 時点。

## 要約(用語なし、3 行まで)
確かめ方を 4 つの段階に分け、小さく速いものから順に回す。
部品ひとつずつ、データベースを使う組み合わせ、問い合わせ口 50 本、実際の画面の操作の 4 つである。
画面の操作のテストは、ページが動き出すのを待ってから押す。

## 何を作るか
| 段階 | 道具 | 対象 | DB | 回すコマンド |
|---|---|---|---|---|
| 1. 単体 | Spock(Groovy のテストの道具)+ `@UnitTest` | Service、Populator、動的属性 | 使わない | `platform/` で `./ant unittests` |
| 2. 結合 | Spock + `@IntegrationTest` | DAO、インターセプター、ModelService | 使う | `./ant integrationtests` |
| 3. OCC | `tools/occ-smoke.mjs`(Node だけ) | OCC の 50 本と OAuth | 使う(サーバーが動いていること) | 最上位で `node tools/occ-smoke.mjs` |
| 4. E2E | Playwright 1.64(real / mock) | 検索、商品詳細、カート、チェックアウト | real は使う | `storefront/` で `npm run e2e -- --project=real` |
| (フロントの単体) | Vitest(`@angular/build:unit-test`) | Angular の部品 | 使わない | `npx ng test --watch=false` |

(ソース: `platform/resources/ant/ant-impl.sh:38-40`、`tools/occ-smoke.mjs:1-3`、`storefront/playwright.config.ts:3-36`、`storefront/angular.json:106-108`)

## 実現の仕方の仕分け
該当なし(テストの決まり)。

## 流れ(1 段 1 行。各段の責任)
### 段階 1・2: `ant unittests` / `ant integrationtests`
| 段 | ファイル(行) | 中身 | 責任 |
|---|---|---|---|
| 1 | `platform/resources/ant/ant-impl.sh:38-40` | `unittests` → `gradle test`、`integrationtests` / `alltests` → `gradle test -Pintegration` | ant の名前を Gradle に読み替える |
| 2 | `platform/build.gradle:56-62` | `test` の sourceSet | 各拡張の `testsrc` と `web/testsrc` の Groovy を集める |
| 3 | `platform/build.gradle:108-123` | `test` タスク | `-Pintegration` が無ければ tag `integration` を外す。あれば DB の URL を渡し、全部回す |
| 4 | `platform-lite/testsrc/de/hybris/bootstrap/annotations/UnitTest.java:10-16` / `IntegrationTest.java:10-16` | `@UnitTest`(tag `unit`)/ `@IntegrationTest`(tag `integration`) | 印を付ける |
| 5 | `platform-lite/testsrc/de/hybris/platform/servicelayer/ServicelayerTransactionalSpockSpecification.groovy:8-15` | 親クラス | Spring を起動し、テストごとにトランザクションを巻き戻す(作ったデータは残らない) |

- **DB を使う Spock のテストには、`@IntegrationTest` と `spock.lang.Tag("integration")` の両方を付ける。** 付けないと `ant unittests` で DB につなぎに行く(ソース: `ServicelayerTransactionalSpockSpecification.groovy:10`、実例 `custom/xyzcore/testsrc/com/xyz/core/brand/BrandServiceIntegrationSpec.groovy:12-14`)。JUnit の `@Tag` を Spock が読まないためと考えられる(推論)。
- `ant integrationtests` の前に `ant initialize` が要る(ソース: `IntegrationTest.java:10`)。
- 一部だけ回す: `./ant unittests -Dtestclasses.packages=com.xyz.core.*`(`-D` は Gradle の `-P` に変わり、`includeTestsMatching` に渡る)(ソース: `platform/resources/ant/ant-impl.sh:10`、`platform/build.gradle:120-122`)。PowerShell では `'"-Dtestclasses.packages=com.xyz.core.*"'` と引用符で囲む(ソース: `platform/ant.cmd:3`)。
- 結果はテストごとに `passed` / `failed` / `skipped` が出て、失敗は例外を全部出す(ソース: `platform/build.gradle:110-113`)。

今あるテスト(2026-10-20):
| テスト | 種類 | ファイル |
|---|---|---|
| `DefaultBrandServiceSpec` | 単体 | `custom/xyzcore/testsrc/com/xyz/core/brand/impl/` |
| `XyzNewArrivalAttributeHandlerSpec` | 単体 | `custom/xyzcore/testsrc/com/xyz/core/attributehandlers/` |
| `XyzProductBrandPopulatorSpec` | 単体 | `custom/xyzfacades/testsrc/com/xyz/facades/product/populators/` |
| `BrandServiceIntegrationSpec` | 結合 | `custom/xyzcore/testsrc/com/xyz/core/brand/` |
| `ModelServiceIntegrationSpec` | 結合(プラットフォーム) | `platform-lite/testsrc/de/hybris/platform/lite/` |

単体テストは Spring を起動せず、`Mock()` で依存を差し込む(推論。単体の印の説明「DB も Spring も使わない」から: `UnitTest.java:10`)。

### 段階 3: OCC のスモークテスト
| 段 | ファイル(行) | 責任 |
|---|---|---|
| 1 | `tools/occ-smoke.mjs:38-66` | 誰でも見られるもの(検索、詳細、カタログ、CMS、ブランド、無いサイトで 400) |
| 2 | 同 68-84 | ゲストの購入(カート作成 → 明細 → メール → 届け先 → 配送方法 → 支払い → 注文 → guid で注文を見る) |
| 3 | 同 86-119 | ログインした人(トークン、401 / 403、住所、カート、注文、レビュー、登録) |
| 4 | 同 121-128 | B2B(`hanako@example.com` で orgUnits) |
| 5 | 同 130-133 | 期待と違った回数を出し、1 回でも違えば終了コード 1 |

- 別のサーバーに向けるとき: `node tools/occ-smoke.mjs http://<host>:9002`(ソース: 同 2,5 行)。
- 注文と顧客を作るので、何度も回すとデータが増える。元に戻すときは `ant initialize`(推論)。

### 段階 4: Playwright
| project | 相手 | 回るテスト | コマンド |
|---|---|---|---|
| `real` | Docker Compose の SSR(:4000)と疑似 Commerce(:9002) | 全部(8 本) | `docker compose --profile storefront up -d` のあと `npm run e2e -- --project=real` |
| `mock` | `npm run start:mock`(:4200、SSR なし、MSW) | 名前に `@mock` が付いた 4 本 | `E2E_MOCK=1 npm run e2e -- --project=mock`(開発サーバーを自動で起動) |

(ソース: `storefront/playwright.config.ts:3-36`、`claude.md` の 5 段目。PowerShell では `$env:E2E_MOCK='1'; npm run e2e -- --project=mock`)

| ファイル | テスト | `@mock` |
|---|---|---|
| `e2e/search.spec.ts` | キーワード検索 / カテゴリから一覧を開き並べ替える | 1 本目だけ |
| `e2e/product-details.spec.ts` | 名前・価格・在庫(real では SSR の HTML も確かめる)/ 一覧から詳細へ / 在庫切れでボタンが出ない | 1 本目だけ |
| `e2e/cart.spec.ts` | カートに入れて数量を変える / 空のカート | 1 本目だけ |
| `e2e/checkout.spec.ts` | ゲストで注文を確定する | あり |

(ソース: `storefront/e2e/*.spec.ts` の `test(` の行)

- real の接続先は `E2E_BASE_URL` で変えられる(既定 `http://localhost:4000`)(ソース: `storefront/playwright.config.ts:8`)。
- 在庫のある商品は `IN_STOCK` に並べてある。サンプルデータの種を変えたら確かめ直す(ソース: `storefront/e2e/support.ts:3-4`)。
- 失敗したときだけスクリーンショットと trace を残す(ソース: `storefront/playwright.config.ts:17-21`)。

## 使う API
段階 3 と 4 が OCC を呼ぶ。一覧は `docs/design/occ-api.md`。

## SSR での扱い
### 落とし穴: 動き出す前に押すと取りこぼす
- SSR の HTML が見えても、ブラウザの Angular がつながる(hydration)前に押した操作は、取りこぼされたり、古い値で再生されたりすることがある(ソース: `storefront/e2e/support.ts:6-9`)。
- **ページを開くときは `page.goto` でなく `open(page, url)` を使う。** 通信が落ち着く(`networkidle`)まで待つ(ソース: `storefront/e2e/support.ts:10-13`)。
- 画面の中でページが変わったあとも、操作の前に `page.waitForLoadState('networkidle')` で待つ(例: `storefront/e2e/checkout.spec.ts:12-13`)。
- SSR の HTML そのものを確かめるときは `request.get(url)` で取った文字列を見る。mock は SSR しないので real だけで確かめる(ソース: `storefront/e2e/product-details.spec.ts:12-16`)。

## 翻訳キーと CMS
- テストは画面の文字ではなく `data-testid` で部品を探す。ラベルで探すときは ja の文言を使う(ブラウザの言語は `ja-JP`)(ソース: `storefront/playwright.config.ts:18`、`storefront/e2e/checkout.spec.ts:16-30`)。

## テスト
この文書そのものがテストの設計。新しい機能では次を足す。
- バックエンド: Service と Populator の単体、DAO とインターセプターの結合、`occ-smoke.mjs` に呼び出しを 1 行。
- フロント: MSW のハンドラーとモックの JSON、`@mock` を付けた E2E を 1 本(モックで先に通し、real でも通す)。

## 根拠の確かさ
- (推論): JUnit の `@Tag` を Spock が読まないため、`spock.lang.Tag` も要る。
- (推論): `occ-smoke.mjs` を回すとデータが増える。計測の前には `ant initialize` で戻す(`claude.md` の「計測のしかた」)。
- (未確認): `ant alltests` は `integrationtests` と同じ中身(ソース: `platform/resources/ant/ant-impl.sh:39-40`)。本物の `alltests` との違いを気にするかは QA の担当に確かめる。
