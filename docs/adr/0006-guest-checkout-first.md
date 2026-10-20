# 0006. 画面の購入の流れは、ゲストのチェックアウトから作る
日付: 2026-10-20
状態: 承認

## 背景
- E2E の対象は、商品検索、商品詳細、カート、チェックアウトの 4 画面(ソース: `claude.md` の「リポジトリの構成」)。
- 購入には、ゲスト(ログインしない人)とログインした人の 2 つの流れがある。ログインには、トークンの取得、トークンの保存と更新、ログインの画面、カートの引き継ぎ(ゲストのカートをログイン後のカートにまとめる)が要る(推論。本物の Spartacus の構成から)。
- OCC 側は両方の流れに対応している(ソース: `tools/occ-smoke.mjs:68-111`)。

## 選択肢
1. ゲストから作る: 利点 = ログインの画面とトークンの扱いが要らず、4 画面を早く通せる。欠点 = マイアカウント、住所録、注文履歴の画面が無い。コスト = 小。
2. ログインした人から作る: 利点 = 本物の多くの案件の主な流れに近い。欠点 = トークンの仕組みを先に作る手間。コスト = 中。
3. 何もしない(購入の流れを作らない): 欠点 = 計測のシナリオ(配送予定日、バグ B04 など)の土台が無い。

## 決定
案 1。
1. 4 画面の E2E を最短で通せる(ソース: `storefront/e2e/checkout.spec.ts:5-37`)。
2. ストアフロントの利用者は `anonymous` に固定し、カートは guid で見分ける(ソース: `storefront/src/app/xyz-storefront-lib/cart/facade/active-cart.service.ts:9-19`)。
3. 1 ページで「メールと届け先 → 配送方法 → 支払い → 確定」を行う(ソース: `storefront/src/app/xyz-storefront-lib/cms-components/checkout/checkout.component.ts:9-12`)。

## 根拠
- 流れの詳細は `docs/design/checkout-flow.md`。
- ゲストの顧客は uid `<カートの guid>|<メール>` で作る。本物と同じ uid の形にしてある(ソース: `core-customize/hybris/bin/platform-lite/src/de/hybris/platform/commercewebservices/core/v2/helper/GuestCustomerHelper.java:12,33`)。本物と同じかは確かめていない(未確認)。

## 影響
- コード: `ActiveCartService.getUserId()` は常に `anonymous`。ログインを足すときは、ここを `current` に切り替える仕組みが要る(ソース: `active-cart.service.ts:19,48-50`)。カートの Effects は `current` のとき code を使うように書いてある(ソース: `storefront/src/app/xyz-storefront-lib/cart/store/cart.effects.ts:33`)。
- テスト: ログインした人の流れは `tools/occ-smoke.mjs` だけで確かめる。
- 戻し方: この決定は機能を足す順の決定なので、戻す作業は無い。ログインを足すときは、この ADR を置き換える新しい ADR を書く。

## 未決事項
- ログインと、ゲストのカートの引き継ぎをいつ作るか(PO が決める)。
- B2B(承認の流れ)はシナリオ S01 で設計する(ソース: `claude.md` の「計測のシナリオ」)。
