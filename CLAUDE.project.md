# CLAUDE.md — XYZ Store(SAP Commerce Cloud + Composable Storefront)

このリポジトリは、XYZ Store の EC サイトです。バックエンドは SAP Commerce Cloud(`core-customize/`)、フロントは Composable Storefront(Spartacus / Angular、`storefront/`)です。
ローカルでは Docker Compose で PostgreSQL、Commerce、SSR のストアフロントを動かします。

## この案件の前提(決定済み。変えるときは人が決める)
- SSR あり(`npm run build:ssr`)。すべての画面はサーバーでも描かれる。
- フロントは Composable Storefront の標準構成。バックエンドは SAP Commerce の標準構成。ストアフロントは OCC だけを呼ぶ。
- BFF、Next.js などの別フロント、別の Spring Boot サービスは置かない。
- サイトは `electronics-spa`、言語は ja / en、通貨は JPY / USD。価格は税込み(消費税 10% は内税)。

## リポジトリの構成
| 場所 | 中身 |
|---|---|
| `core-customize/hybris/bin/custom/xyzcore` | items.xml、ImpEx、Service、DAO、インターセプター、`xyzcore-spring.xml` |
| `core-customize/hybris/bin/custom/xyzfacades` | Facade、Populator、`xyzfacades-beans.xml`(Data) |
| `core-customize/hybris/bin/custom/xyzocc` | OCC の Controller(`web/src`)、`xyzocc-beans.xml`(WsDTO)、`xyzocc-web-spring.xml` |
| `core-customize/hybris/bin/platform-lite` | 標準のプラットフォームと標準の拡張(commerceservices、commercefacades、commercewebservices など)。**変更しない** |
| `core-customize/hybris/config` | `localextensions.xml`、`local.properties` |
| `storefront/src/app/xyz-storefront-lib` | Composable Storefront の標準ライブラリ(`@xyz/storefront`)。**変更しない** |
| `storefront/src/app/custom` | 自社の部品 |
| `storefront/mocks` | MSW のモック(OCC と同じ形の JSON) |
| `storefront/e2e` | Playwright |
| `api/occ-openapi.yaml` | OCC の仕様 |
| `docs/` | 設計書(`docs/design`)、ADR(`docs/adr`)、手順書(`docs/runbook`)。一覧は `docs/README.md` |

## ルール
- 標準のソース(`platform-lite`、`xyz-storefront-lib`)をコピーして改変しない。バックエンドは自社の拡張で足す・差し替える(bean の alias、modifyPopulatorList、自分の beans.xml と fieldSetLevelMapping)。フロントの差し替えは cmsComponents、追加は Outlet。
- コンポーネントから直接 HTTP を呼ばない。Adapter → Connector → Facade 経由。
- OCC の fields は必要な項目だけ。FULL を既定にしない。
- 画面文言は翻訳キー(`storefront/src/assets/i18n`)。ハードコードしない。
- window / document / localStorage を直接触らない(isPlatformBrowser)。
- OCC の URL はハードコードしない(`provideConfig` の `backend.occ`)。
- items.xml を変えたら `ant all` のあと `ant updatesystem`。Data / WsDTO は `*-beans.xml` から作られる(生成物の `bootstrap/gensrc` は直接書き換えない)。
- dist と gensrc をコミットしない。
- 本番の操作(デプロイ、ロールバック、設定変更)は提案までにとどめ、人が実行する。
- 依頼を 1 件終えて報告する直前に、毎回 cs-cost の手順で `node .claude/skills/cs-cost/scripts/cost.mjs --label "<依頼>"` を実行し、出た表を報告の最後に付ける(担当に任せた分も含めて数える)。

## コマンド
バックエンド(`core-customize/hybris/bin/platform` で実行。中は Docker で動く。Windows の PowerShell では `-D` の引数を `'"-Dresource=..."'` のように引用符で囲む):
- ビルド: `./ant clean all`(Windows は `ant.cmd`)
- 型を DB に反映: `./ant updatesystem`
- ImpEx を入れる: `./ant importImpex -Dresource=<ファイル>`
- 単体テスト: `./ant unittests` / 結合テスト(DB を使う): `./ant integrationtests`
- 起動と停止: `./hybrisserver.sh start` / `./hybrisserver.sh stop`(OCC は http://localhost:9002/occ/v2/electronics-spa)
- OCC の動作確認: リポジトリの最上位で `node tools/occ-smoke.mjs`

ストアフロント(`storefront` で実行):
- 開発(モック): `npm run start:mock`(http://localhost:4200、バックエンド不要)
- SSR ビルド: `npm run build:ssr`、起動: `docker compose --profile storefront up -d storefront`(http://localhost:4000)
- 単体テスト: `npx ng test --watch=false`
- E2E: `npm run e2e -- --project=real`(Docker の環境に対して)
- モックの応答を録り直す: `npm run mocks:capture`

テスト用の利用者: `taro@example.com`(B2C)、`hanako@example.com`(B2B の承認者)。パスワードは `Test1234!`。OAuth のクライアントは `mobile_android` / `secret`。

## 仕組みで止めていること(.claude/hooks と settings)
- ant initialize、rm -rf、git push --force、DROP TABLE、パッケージの公開は止まる。止まったら手順を書いて人に依頼する。
- .env と鍵は、Read と、よくある読み出しコマンドを止める。標準のソース(`hybris/bin/platform-lite`、`hybris/bin/platform`)、node_modules、dist への書き込みは止まる。
- 編集後に OCC の URL の直書き、fields=FULL、window / document の直接利用、typecode、ImpEx の unique=true を検査する。

## 担当の分け方(.claude/agents)
- フロントエンド(ストアフロント側の実装): cs-frontend / 設計: cs-architect / 運用: cs-operator / 信頼性: cs-sre
- 監査: cs-auditor / 要望の整理: cs-pdm / テスト: cs-qa / 性能: cs-performance
- 更新: cs-upgrader / 文言と CMS: cs-content-i18n / OCC の契約: cs-occ-liaison
- バックエンド(SAP Commerce 側の OCC 拡張、型、ImpEx): cs-backend
- 文書(引き継ぎ、設計書、手順書、API 仕様、説明資料): cs-docs
