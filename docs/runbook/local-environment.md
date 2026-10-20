# 手順書: ローカル環境の起動・停止・ビルド・データの作り直し

使う場面: 開発を始めるとき、items.xml や ImpEx を変えたとき、データを最初の状態に戻したいとき、画面が出ないとき。開発者が自分のパソコン(Windows / macOS)で行う。
所要時間: 初回 約 15 分(Docker のイメージと依存の取得を含む。推論)/ 2 回目から 約 3 分 / 実行する人: 開発者(本番の操作は含まない)

パスの書き方: リポジトリの最上位からの相対。`platform/` は `core-customize/hybris/bin/platform/`。行番号は 2026-10-20 時点。

## 始める前に(すべて YES になるまで進まない)
- [ ] Docker Desktop が動いている(`docker compose version` が版を返す)。ant も Commerce も Docker の中で動くので、Java や Gradle をパソコンに入れる必要は無い(ソース: `platform/ant:2-5`、ADR 0007)
- [ ] `storefront/` で `node -v` が `v22.23.3`、`npm -v` が `11.8.0`。Volta を入れていれば `storefront/package.json` の `volta` で自動で揃う(ソース: `storefront/package.json` の `volta`、ADR 0004)
- [ ] ポート 4000、4200、5432、9002 を、ほかのアプリが使っていない(ソース: `docker-compose.yml:12-13,40-41,59-60`)
- [ ] 計測の最中ではない。計測中の会話では `ant initialize` が仕組みで止められる(ソース: `CLAUDE.project.md:58`)

## 手順
コマンドは、断りが無ければ `platform/` で実行する。Windows の PowerShell と cmd では `./ant` の代わりに `.\ant.cmd`、`./hybrisserver.sh` の代わりに `.\hybrisserver.bat` を使う(ソース: `platform/ant.cmd:1-5`、`platform/hybrisserver.bat:1-6`)。

| # | やること | 画面 / コマンド | 期待する結果 | 違ったら |
|---|---|---|---|---|
| 1 | ビルド(items.xml → Model、Data / WsDTO の生成、jar を作る) | `./ant clean all` | 拡張ごとに `[echo] building extension '...'` が出て、最後に `BUILD SUCCESSFUL`。`platform/build/libs/commerce.jar` ができる | T1 へ |
| 2 | DB を作り、データを全部入れる(初回と、データを最初に戻すとき) | `./ant initialize` | `**** initializing system ...` のあと、ImpEx ごとに `imported N lines from ...`。最後に `BUILD SUCCESSFUL`。2026-10-20 の構築作業では約 83 秒(実測) | T2 へ |
| 2' | 型だけ反映する(items.xml に属性や型を足したとき。データは消さない) | `./ant updatesystem` | `added N tables / columns` か `type system is up to date` | T2 へ |
| 3 | 疑似 Commerce を起動する | `./hybrisserver.sh start`(ログを見続けるときは `./hybrisserver.sh`) | `docker compose ps` で `commerce` が running。`http://localhost:9002/occ/v2/basesites` が JSON を返す | T3 へ |
| 4 | OCC を確かめる(最上位で) | `node tools/occ-smoke.mjs` | 「N 回呼んで N 回が期待どおり、0 回が違った」 | NG の行を見て T3 へ |
| 5 | ストアフロントの依存を入れる(初回だけ。`storefront/` で) | `npm ci` | エラー無く終わる | T4 へ |
| 6 | SSR のビルド(`storefront/` で) | `npm run build:ssr` | `storefront/dist/storefront/server/server.mjs` ができる | — |
| 7 | 3 つを起動する(最上位で) | `docker compose --profile storefront up -d` | `http://localhost:4000/` にホームが出る | T5、T6 へ |
| 8 | (バックエンド無しで画面を作るとき)モックで起動(`storefront/` で) | `npm run start:mock` | `http://localhost:4200/` にホームが出る(SSR なし、MSW) | T6 へ |
| 9 | 止める | `./hybrisserver.sh stop`(Commerce だけ)/ 最上位で `docker compose --profile storefront stop`(全部) | `docker compose ps` が空か stopped | — |

### コマンドの補足
- ant のターゲットと中身(ソース: `platform/resources/ant/ant-impl.sh:33-50`):

| ターゲット | 中身 |
|---|---|
| `clean` | `gradle clean`(`bootstrap/gensrc` も消す。`platform/build.gradle:91-94`) |
| `all` / `build` / `compile` | `gradle bootJar`(先に items.xml と beans.xml から gensrc を作る。`platform/build.gradle:78-89`) |
| `initialize` | `commerce.jar --hybris.mode=initialize`(表を全部消して作り直し、common と sampledata の ImpEx を入れる) |
| `updatesystem` | `commerce.jar --hybris.mode=update`(足りない表と列を足し、common の ImpEx を入れる) |
| `unittests` / `integrationtests` / `alltests` | `gradle test`(`docs/design/testing.md`) |
| `importImpex -Dresource=<パス>` | 1 つの ImpEx を入れる。パスは `hybris/bin` からの相対か、クラスパス上の場所 |

- 知らないターゲットは `BUILD FAILED` と使えるターゲットの一覧を出す(ソース: 同 45-49 行)。
- `initialize` と `updatesystem` は `build/libs/commerce.jar` を使う。無ければ「先に 'ant all' を実行してください」と出て止まる(ソース: 同 18-21 行)。
- `-D` の引数は Gradle の `-P` に読み替えられる(ソース: 同 10 行)。
- **Windows の PowerShell では、`-D` の引数を `'"..."'` で囲む。** 例: `.\ant.cmd importImpex '"-Dresource=custom/xyzcore/resources/xyzcore/import/sampledata/04-brands.impex"'`(ソース: `platform/ant.cmd:3`)。囲まないと、PowerShell が `=` や `.` のところで引数を分けてしまうことがある(推論)。
- Git Bash や macOS では `./ant importImpex -Dresource=...` のままでよい(推論)。

### データを最初の状態に戻す
1. `./hybrisserver.sh stop`(動いている Commerce が古いデータを持ったままにならないように。推論)
2. サンプルデータを作り直すときだけ、最上位で `node tools/generate-sampledata.mjs`(種が固定なので、スクリプトを変えていなければ同じファイルになる。ソース: `tools/README.md:26`)
3. `./ant initialize`(`DROP SCHEMA public CASCADE` から作り直す。ソース: `platform-lite/src/de/hybris/platform/lite/persistence/SchemaManager.java:31-36`)
4. `./hybrisserver.sh start`
5. 発行済みのトークンは Commerce を止めた時点で消える。ブラウザでログインし直す(ソース: `platform-lite/src/de/hybris/platform/lite/web/OAuthTokenStore.java:9-12`)。ゲストのカートの guid がブラウザの localStorage に残っていると、カートが見つからない状態になる。新しく「カートに入れる」と作り直される(推論。`storefront/src/app/xyz-storefront-lib/cart/facade/active-cart.service.ts:52-62`)

DB ごと消したいときは、最上位で `docker compose down -v`(ボリューム `pgdata` と `gradle-home` も消える。Gradle の依存の取り直しで次のビルドが遅くなる。推論)。

### 違ったときの対処
| 番号 | 症状 | 調べ方・直し方 |
|---|---|---|
| T1 | `ant all` が `拡張 'xxx' が見つかりません` で落ちる | `localextensions.xml` に書いた名前と、`extensioninfo.xml` の `name` が合っているか(ソース: `platform/build.gradle:18-26`)。items.xml の誤りは `xxx-items.xml: ...` の形で出る(ソース: `platform-lite/bootstrap/src/de/hybris/bootstrap/typesystem/TypeSystem.java:178-181,200,265`) |
| T2 | `ant initialize` が `Import has caused an error, see following unresolved lines` で落ちる | ログに出た行の ImpEx を直す。値の参照(`catalogVersion(catalog(id),version)` など)の先が無い、enum の値の誤り、列の数の違いが多い(ソース: `platform-lite/src/de/hybris/platform/lite/setup/SystemSetup.java:112-118`)。サンプルデータはスクリプトを直して作り直す |
| T2' | DB につながらない | `docker compose ps` で `postgres` が healthy か。builder と commerce は postgres が healthy になるまで待つ(ソース: `docker-compose.yml:15-20,32-34`) |
| T3 | OCC が 400 `InvalidResourceError` | URL のサイトが `electronics-spa` か(ソース: `core-customize/hybris/config/local.properties:13`)。データが入っていない(手順 2 をしていない)ときも出る(推論) |
| T3' | Commerce がすぐ止まる | `./hybrisserver.sh`(ログ表示)でエラーを見る。`commerce.jar` が無い、DB が空、が多い(推論) |
| T4 | `npm ci` が依存の解決で落ちる | `npm -v` が 11.8.0 か。npm 10.9.9 では依存の解決で落ちた(実測。`claude.md` の版の表)。Volta を使うか、npm 11.8.0 を入れる |
| T5 | ポート 4000 が使われていて storefront が起動しない | Windows: `netstat -ano | findstr :4000` で PID を見て、そのアプリを止める。macOS: `lsof -i :4000`。前に `npm run serve:ssr` をパソコンで直接動かしたままのことが多い(推論)。止めてから手順 7 をやり直す |
| T6 | 画面は出るが SSR されない、またはホスト名のエラーになる | ブラウザの URL が `localhost` か `127.0.0.1` か。SSR を受け付けるホスト名はこの 2 つだけ(ソース: `storefront/angular.json:44-49`)。別の名前(LAN の IP など)で開くときは `allowedHosts` に足す。足すと何が起きるかは cs-frontend に確かめる(未確認) |
| T7 | 画面に商品が出ない(SSR の HTML が空) | storefront のコンテナの `OCC_BASE_URL` が `http://commerce:9002` か(ソース: `docker-compose.yml:56-58`、`storefront/src/app/app.module.server.ts:16`)。Commerce が動いているか(手順 3) |

## 終わった後の確認
- [ ] `node tools/occ-smoke.mjs` が 0 回違った、で終わる
- [ ] `http://localhost:4000/product/XYZ-000001` に商品名と価格が出る
- [ ] (E2E を回すとき)`storefront/` で `npm run e2e -- --project=real` が通る(`docs/design/testing.md`)

## 戻し方
1. `docker compose --profile storefront stop` で止める。
2. データを壊したときは「データを最初の状態に戻す」をやる。
3. ビルドがおかしいときは `./ant clean all` からやり直す(`gensrc` と `build` を消して作り直す)。

## 履歴
| 日付 | 変えたこと | 確かめた方法((実測)など) |
|---|---|---|
| 2026-10-20 | 初版 | 手順 1〜7 と E2E は構築作業で通した(実測。`claude.md` の 2〜5 段目)。この文書を書いた時点では、コマンドを流し直していない |
