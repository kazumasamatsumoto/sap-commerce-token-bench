# 0005. バックエンド無しの画面開発とモックの E2E に MSW 3 を使う
日付: 2026-10-20
状態: 承認

## 背景
- フロントの担当は「モックを先に作り、そのあと本物の OCC につなぐ」順で作る(ソース: `claude.md` の「決まっていること」、シナリオ S05)。
- E2E の一部を、バックエンド無しで回したい(ソース: 同)。
- 前提: ストアフロントは OCC だけを呼ぶ。モックのために別のサーバー(BFF など)を置かない(ソース: `CLAUDE.project.md:8-9`)。

## 選択肢
1. MSW 3.0.3(ブラウザの Service Worker で通信を横取りする): 利点 = アプリのコードを変えずに、同じ OCC の URL のまま応答を差し替えられる。Playwright からも同じ動き。欠点 = SSR(サーバー側の通信)は横取りしない。2 系と書き方が違う。コスト = 小。
2. モック用の HTTP サーバー(json-server など)を立てて、OCC の baseUrl を向け直す: 利点 = SSR でも使える。欠点 = 設定の切り替えが要り、サーバーが 1 つ増える。
3. 何もしない(いつも本物の OCC で開発する): 欠点 = バックエンドの作業が終わるまで画面を作れない。

## 決定
案 1。
1. 画面の部品も Adapter も変えずに、起動のしかた(`npm run start:mock`)だけで切り替えられる(ソース: `storefront/src/main.ts:5-12`、`storefront/angular.json:75-87`)。
2. 応答は本物の OCC から録る(`npm run mocks:capture`)ので、形がずれにくい(ソース: `storefront/mocks/capture-fixtures.mjs:1-3`)。
3. モックの構成は SSR を切る(`ssr: false`)。SSR は本物の環境の E2E で確かめる(ソース: `storefront/angular.json:79`、`storefront/e2e/product-details.spec.ts:12-16`)。

## 根拠
- MSW 3 では、2 系の `onUnhandledRequest` が `onUnhandledFrame` に変わった(実測。`claude.md` の 4 段目)。使い方: `worker.start({ onUnhandledFrame: 'bypass', quiet: true })`(ソース: `storefront/mocks/browser.ts:4-12`)。
- 版: msw 3.0.3(実測: npm view。`claude.md` の版の表)。
- Service Worker のファイルは `public/` に置く(ソース: `storefront/package.json` の `msw.workerDirectory`)。

## 影響
- 新しい OCC の呼び出しを足すときは、`storefront/mocks/handlers.ts` にハンドラーを足し、録り直す対象を `mocks/capture-fixtures.mjs` に足す。
- カートと注文はハンドラーの中で覚え、sessionStorage に置く(ソース: `storefront/mocks/handlers.ts:42-53`)。本物と計算が違わないよう、税は本物と同じ式にする(ソース: 同 60-77 行)。
- E2E は名前に `@mock` を付けたものだけがモックで回る(ソース: `storefront/playwright.config.ts:27-31`)。
- 戻し方: `environment.mock.ts` の `useMocks` を false にするか、`start:mock` を使わない。アプリのコードは変わらない。

## 未決事項
- MSW 2 系の記事やコード片をそのまま使うと動かないことがある。担当への注意として `CLAUDE.project.md` に書くかを決める。
- `storefront/src/server.ts:28-29` の説明は「MSW がここより先に応答する」と読めるが、MSW はブラウザの中だけで動く。説明を直す(cs-frontend)。
