# 0001. 本物の SAP Commerce / Spartacus を入れず、同じ形の疑似環境で開発する
日付: 2026-10-20
状態: 承認

## 背景
- この案件の目的は、SAP Commerce Cloud + Composable Storefront の開発とバグ修正で、AI の担当(サブエージェント)が使うトークン数を見積もることである(ソース: `claude.md` の「目的」)。
- 担当が「読む → 書く → ビルドする → エラーやログを読む → 直す」を本物と同じように繰り返すことが要る。同じ機能は要らない(ソース: `claude.md` の「決まっていること」)。
- 制約: 本物の SAP のソース、顧客のデータ、資格情報、`@spartacus/*` をリポジトリに置けない(ソース: 同)。今の Spartacus は公開の npm では手に入らず、SAP の配布元の資格情報が要る(未確認。構築のときの調べ)。
- 前提(SSR あり、Angular と SAP Commerce の標準構成、OCC だけを呼ぶ、BFF なし)は変えない(ソース: `CLAUDE.project.md:6-10`)。

## 選択肢
1. 本物の SAP Commerce と Spartacus を入れる: 利点 = 作業量が本物と同じ。欠点 = ライセンスと資格情報が要り、リポジトリを公開・共有できない。起動とビルドが重い。コスト = 大。
2. 同じ形の疑似環境を自作する: 利点 = 資格情報なしで誰でも動かせる。ファイルの置き場所、名前、コマンドが本物と同じなので、担当の作業の流れは本物に近い。欠点 = プラットフォームが小さく、読む量・ログの量が本物より少ない。コスト = 中(作る手間)。
3. 何もしない(ファイルの形だけの見本を置き、動かさない): 利点 = 安い。欠点 = ビルドやテストの失敗が起きず、「エラーを読んで直す」の量を測れない。

## 決定
案 2。バックエンドは `platform-lite`(Spring Boot + PostgreSQL で自作)と自社の拡張 `xyzcore` / `xyzfacades` / `xyzocc`、フロントは自作の疑似ライブラリ `@xyz/storefront`(`storefront/src/app/xyz-storefront-lib/`)にする。
1. 資格情報なしで、誰でも同じ環境を作れる。
2. 担当が触るファイルと手順(items.xml、beans.xml、Populator、fieldSetLevelMapping、cmsComponents、Outlet、`ant` のコマンド)が本物と同じになる。
3. 2026-10-20 に人が決めた(ソース: `claude.md` の「疑似環境にする」)。

## 根拠
- `claude.md` の「決まっていること」(人の決定)。
- 本物と同じ名前の bean・クラス・端点にしてある(ソース: `core-customize/hybris/bin/platform-lite/src/de/hybris/platform/lite/config/PlatformLiteConfiguration.java:25-27`、`api/occ-openapi.yaml:5-8`)。
- 本物との差が作業量をどれだけ減らすかは測っていない。「一般的な慣行」ではなく推論として扱う(推論)。

## 影響
- コード: `core-customize/hybris/bin/platform-lite/` と `storefront/src/app/xyz-storefront-lib/` は「標準のソース」とみなし、案件では直さない(ソース: `CLAUDE.project.md:18,20,28`)。
- 計測: 本物との差(プラットフォームのソースや jar を読みに行く、長いビルドのログ、遅い起動)は、計測の数字に足さず「補正の目安」として別に書く(ソース: `claude.md` の「本物との差」)。
- 戻し方: 本物に移すときは、自社の拡張(`custom/`)と `storefront/src/app/custom/` をそのまま本物の環境に載せ、`platform-lite` と `xyz-storefront-lib` の代わりに本物を入れる。import 先の `@xyz/storefront` を `@spartacus/*` に置き換える作業が出る(推論)。

## 未決事項
- 本物との差の「補正の目安」をどう出すか(計測の担当が決める)。
- 本物の Spartacus の取得元と資格情報の扱い(本物に移すときに人が決める)。
