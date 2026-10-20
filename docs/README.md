# docs の一覧

XYZ Store(SAP Commerce Cloud + Composable Storefront)の設計書、ADR、手順書。最初に読むのは `design/architecture.md` と `runbook/local-environment.md`。

- 書き方の決まり: 事実の文には根拠の印 (実測)(ソース)(公式)(推論)(未確認) を付ける。(ソース)にはファイルと行を書く。
- パスの書き方: リポジトリの最上位からの相対。`core-customize/hybris/bin/` の下は頭を省いて `platform/`、`platform-lite/`、`custom/` から書く。行番号は 2026-10-20 時点。

## 設計書(`design/`)
| ファイル | 中身 |
|---|---|
| [design/architecture.md](design/architecture.md) | 全体の構成(SSR のストアフロント ↔ OCC ↔ 疑似 Commerce ↔ PostgreSQL、Docker Compose、本物との違い) |
| [design/backend-extensions.md](design/backend-extensions.md) | xyzcore / xyzfacades / xyzocc の層、ブランドの見本、項目を items.xml から OCC まで足す手順、インターセプター、動的属性 |
| [design/data-model.md](design/data-model.md) | 主な型と関連(表と ER 図)、表の作られ方、サンプルデータの件数 |
| [design/occ-api.md](design/occ-api.md) | OCC の約束ごと(base site、userId、fields、エラー、OAuth)と 50 本の一覧 |
| [design/storefront.md](design/storefront.md) | ストアフロントの構成(Config、CMS の対応表、Outlet、Adapter → Connector → Facade、NgRx、i18n、SSR の決まり、テンプレートとスロット、MSW) |
| [design/checkout-flow.md](design/checkout-flow.md) | ゲストのチェックアウトの流れ(シーケンス図)、カートの見分け方(guid と code)、値段と税の出し方 |
| [design/testing.md](design/testing.md) | テストの段階(ant unittests / integrationtests、occ-smoke、Playwright real / mock)と回し方、落とし穴 |
| [design/custom-components.md](design/custom-components.md) | 自社の部品(`storefront/src/app/custom/`)の設計。別の担当が書く |

## ADR(`adr/`)
| ファイル | 決めたこと |
|---|---|
| [adr/0001-pseudo-environment.md](adr/0001-pseudo-environment.md) | 本物の SAP Commerce / Spartacus を入れず、同じ形の疑似環境で開発する |
| [adr/0002-spring-boot-postgresql-platform-lite.md](adr/0002-spring-boot-postgresql-platform-lite.md) | 疑似プラットフォームを Spring Boot と PostgreSQL で作り、拡張の形は Commerce に合わせる |
| [adr/0003-flexiblesearch-product-search.md](adr/0003-flexiblesearch-product-search.md) | 商品検索は Solr を使わず FlexibleSearch(SQL)で行う |
| [adr/0004-node22-npm11-pin.md](adr/0004-node22-npm11-pin.md) | ストアフロントの Node.js を 22.23.3、npm を 11.8.0 に固定する |
| [adr/0005-msw3-mocks.md](adr/0005-msw3-mocks.md) | バックエンド無しの画面開発とモックの E2E に MSW 3 を使う |
| [adr/0006-guest-checkout-first.md](adr/0006-guest-checkout-first.md) | 画面の購入の流れはゲストのチェックアウトから作る |
| [adr/0007-ant-inside-docker.md](adr/0007-ant-inside-docker.md) | ant のコマンドは本物と同じ名前で呼び、中身は Docker の Gradle で動かす |

## 手順書(`runbook/`)
| ファイル | 中身 |
|---|---|
| [runbook/local-environment.md](runbook/local-environment.md) | ローカル環境の起動・停止・ビルド(ant のコマンド、Windows の `-D` の書き方)、データの作り直し、困ったときの対処 |

## ほかの資料
- OCC の正式な仕様: `api/occ-openapi.yaml`
- データを作るスクリプト: `tools/README.md`
- 文書の型: `.claude/skills/cs-doc-writing/`(設計書・手順書・API)、`.claude/skills/cs-adr/`(ADR)
