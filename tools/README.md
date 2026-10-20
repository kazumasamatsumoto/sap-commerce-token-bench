# tools

## generate-sampledata.mjs

サンプルデータの ImpEx を作るスクリプトです。Node 22 だけで動きます(npm のパッケージは使いません)。

```
node tools/generate-sampledata.mjs
```

出力先: `core-customize/hybris/bin/custom/xyzcore/resources/xyzcore/import/sampledata/`

| ファイル | 中身 |
| --- | --- |
| 01-catalog.impex | カタログ 2 件、サイト、配送方法 2 件、ストア |
| 02-media.impex | 画像 1,000 件(商品、ブランド、カテゴリ、バナー) |
| 03-categories.impex | カテゴリ 100 件(上位 10 + 下位 90) |
| 04-brands.impex | ブランド 30 件 |
| 05-products.impex | 商品 1,000 件 |
| 06-prices-stock.impex | 価格(JPY と USD)、在庫 |
| 07-b2b-customers.impex | B2B ユニット 100 件、顧客 1,000 人 + テスト用 2 人、住所 |
| 08-orders.impex | 注文 5,000 件と明細 |
| 09-reviews.impex | レビュー 2,000 件 |
| 10-cms.impex | CMS のテンプレート、部品 500 件、スロット、ページ 100 件 |

- 乱数の種と基準日(2026-10-01)を固定しているので、何度動かしても同じファイルができます。
- 出力したファイルはコミットしてください。`ant initialize` はこのフォルダのファイルを名前の順に読み込みます。
- ImpEx を手で直さず、スクリプトを直してから作り直してください。
- テスト用ユーザー: `taro@example.com`(B2C)、`hanako@example.com`(B2B、承認者)。パスワードはどちらも `Test1234!`。
