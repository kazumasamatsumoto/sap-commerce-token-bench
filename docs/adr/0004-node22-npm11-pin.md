# 0004. ストアフロントの Node.js を 22.23.3、npm を 11.8.0 に固定する
日付: 2026-10-20
状態: 承認

## 背景
- ストアフロントは Angular 21.2 系で作る。まねる対象の Spartacus(release/221121.22.x の系列)が Angular 21 に合わせているため(ソース: `claude.md` の版の表)。
- Node.js の最新の LTS は 24 だが、Spartacus の指定は 22 系(ソース: `claude.md` の版の表。構築のときに Spartacus の package.json で確かめた)。
- npm 10.9.9 では依存の解決で落ちた(実測。`claude.md` の版の表)。
- 計測では、版が変わるとそれまでの結果と混ぜられない(ソース: `claude.md` の「計測のしかた」)。

## 選択肢
1. Volta で Node 22.23.3 と npm 11.8.0 を `storefront/package.json` に固定する: 利点 = `storefront/` に入るだけで揃う。Docker のイメージも同じ版にできる。欠点 = Volta を入れていない人は手で揃える。コスト = 小。
2. `.nvmrc` と `engines` で指定するだけ: 利点 = 道具を選ばない。欠点 = 自動では切り替わらず、違う版で動いてしまう。
3. 何もしない(各自のパソコンの版で動かす): 欠点 = npm 10 では `npm ci` が落ちる。結果がパソコンごとに違う。

## 決定
案 1。
1. npm 10.9.9 で落ちたので、11.8.0 が要る(実測)。
2. Volta なら、`storefront/` の外の作業(バックエンドの Docker)に影響しない。
3. SSR のコンテナも同じ版のイメージ `node:22.23.3-bookworm-slim` にして、パソコンとコンテナの違いを無くす(ソース: `docker-compose.yml:49-51`)。

## 根拠
- `storefront/package.json` の `volta`(node 22.23.3、npm 11.8.0)と `packageManager: npm@11.8.0`。
- 実測: npm 10.9.9 で依存の解決が落ちた(`claude.md` の版の表)。何の依存で落ちたかの記録は無い(未確認)。

## 影響
- 開発者: Volta を入れる。入れない人は `node -v` と `npm -v` を手で合わせる(`docs/runbook/local-environment.md` の「始める前に」)。
- Docker: storefront のイメージの版を変えるときは、`package.json` の `volta` と一緒に変える。
- 戻し方: `volta` の欄と `packageManager` を消し、`docker-compose.yml` のイメージの版を戻す。

## 未決事項
- Angular 22 / Node 24 に上げる時期(Spartacus の対応を待つ。cs-upgrader が確かめる)。
- npm 10 で落ちた原因(未確認。必要になったら cs-frontend が調べる)。
