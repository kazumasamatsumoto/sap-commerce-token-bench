# CLAUDE.md — SAP Commerce Cloud 開発のトークン計測用の疑似案件

このリポジトリは、Claude Opus 5.5 を Amazon Bedrock で使うときの予算を見積もるための**計測台**です。
SAP Commerce Cloud + Composable Storefront の案件に似せた、**実際にビルドして動かせる**疑似環境を作り、
composable-storefront-claude-code-kit の担当(サブエージェント)に決まった開発とバグ修正をさせて、トークン数を測ります。

## 目的(最終的に出す数字)
- SAP Commerce Cloud の案件で、**開発とバグ修正をしたときに使うトークン数**の推計
- 1 ストーリーあたりの **入力・キャッシュ書き込み・キャッシュ読み込み・出力** の 4 つのトークン数(担当ごと、シナリオごと)と、かかった時間
- それを 1 スプリント、1 人月に積み上げた量
- Bedrock の単価を掛けた月額
- **4 つは最後まで分けて持つ。** 入力の 9 割以上はキャッシュ読み込みで、単価は通常の入力の 1/20(Opus 5.5)。まとめると入力の費用を十倍以上に見積もる

## 決まっていること(変えるときは人が決める)
- 本物の案件の前提: SSR あり。フロントは Angular(Composable Storefront の標準構成)、バックエンドは SAP Commerce の標準構成。ストアフロントは OCC だけを呼ぶ。BFF、Next.js は置かない。
- 本物の SAP のソース、顧客のデータ、資格情報はリポジトリに置かない。本物の Spartacus(@spartacus/*)も SAP Commerce も入れない。
- **疑似環境にする(2026-10-20 に人が決めた)。** 目的はトークン量の推計なので、担当が「読む → 書く → ビルドする → エラーやログを読む → 直す」の繰り返しが本物と同じように起きることを優先する。本物と同じ機能は求めない。
- **フロント**: Spartacus と同じ構成(CMS コンポーネントの対応表、Outlet、OCC の Adapter → Connector → Facade、NgRx、i18n、SSR)をまねた Angular のアプリを自分で書く。疑似ライブラリは `storefront/src/app/xyz-storefront-lib/`(`@xyz/storefront` で import)。
- **バックエンド**: ファイルの形は SAP Commerce と同じにする(拡張 = extension、items.xml、ImpEx、`*-spring.xml`、Model、DAO、Service、Facade、Populator、WsDTO、OCC の Controller)。中で動かす仕組みは Spring Boot と PostgreSQL で自作する(疑似プラットフォーム `platform-lite`)。
  - items.xml から Model のクラスと DB の表を作る(本物の `ant build` と `ant updatesystem` に当たる)
  - ImpEx(INSERT / INSERT_UPDATE / UPDATE / REMOVE の基本の書き方)を読み込む
  - FlexibleSearch(`SELECT {pk} FROM {Product} WHERE {code} = ?code` の基本の形)を SQL に直して流す
  - ModelService、Converter / Populator、`@Secured` を使えるようにする
- **コマンドは本物と同じ名前で呼べるようにする**: `ant clean all`、`ant build`、`ant initialize`、`ant updatesystem`、`ant unittests`、`./hybrisserver.sh`。中では Docker の Gradle が動く。
- **全体は Docker Compose で起動する**: PostgreSQL、疑似 Commerce、SSR のストアフロントの 3 つ。
- MSW のモックは、バックエンド無しで画面を作る作業(cs-frontend の「モックを先に作る」)と、E2E の一部に使う。

## バージョン(2026-10-20 に確かめたもの)

| もの | 版 | 根拠 |
|---|---|---|
| Composable Storefront(Spartacus) | まねる対象は release/221121.22.x の系列(アプリの version 2611.0.0) | ソース: github.com/SAP/spartacus の package.json(2026-10-16 のコミット)。入れはしない |
| Angular / @angular/ssr | 21.2 系(入ったのは 21.2.25) | ソース: 同上。Angular の最新は 22.2.3 だが、Spartacus に合わせて 21 |
| NgRx / RxJS / TypeScript | 21.0 系 / 7.8 系 / 5.9 系 | ソース: 同上 |
| Node.js / npm | 22.23.3 / 11.8.0(Volta で storefront に固定) | 実測。npm 10.9.9 は依存の解決で落ちた |
| Playwright | @playwright/test 1.64.1 | 実測: npm view |
| MSW | 3.0.3 | 実測: npm view。2 系と書き方が違う可能性がある(未確認) |
| Java | 21 | 実測: このマシンの openjdk 21.0.10。ビルドは Docker の中で行う |
| Gradle | Docker の gradle イメージ(jdk21) | このマシンには Gradle も Maven も無い(実測) |
| PostgreSQL | 16(Docker) | 本物の Commerce は HANA。DB の違いは計測に関係しない(推論) |
| Claude Code | 2.1.304(計測中は固定する) | 実測 |
| モデル | Claude Opus 5.5(Bedrock の ID は `global.anthropic.claude-opus-5-5` など) | 二次情報。Bedrock のコンソールで確かめる(未確認) |

## リポジトリの構成

```text
.
├── CLAUDE.md                  … この文書(作るときの指示)
├── CLAUDE.project.md          … 計測のときに CLAUDE.md と置き換える「案件の CLAUDE.md」(キットの CLAUDE.md.example をもとにする)
├── docker-compose.yml         … postgres / commerce / storefront
├── kit/.claude/               … キット。作る間はここに退避し、6 段目で ./.claude に戻す
├── storefront/                … Angular CLI で作った SSR のアプリ。Spartacus と同じ構成をまねる
│   ├── src/app/xyz-storefront-lib/ … Spartacus の代わりの疑似ライブラリ
│   ├── src/app/spartacus/     … Spartacus のアプリと同じ置き方の設定
│   ├── src/app/custom/        … 自社の部品(30〜50 個)。cmsComponents の差し替えと Outlet の追加を混ぜる
│   ├── src/assets/i18n/       … ja / en の翻訳
│   ├── mocks/                 … MSW のハンドラと、OCC と同じ形の JSON
│   └── e2e/                   … Playwright(商品検索、商品詳細、カート、チェックアウトの 4 画面)
├── core-customize/
│   ├── manifest.json          … CCv2 と同じ形(extensions の一覧など)
│   └── hybris/
│       ├── config/localextensions.xml, local.properties
│       └── bin/
│           ├── platform/      … ant / hybrisserver.sh の入口(中身は Docker の Gradle を呼ぶ)
│           ├── platform-lite/ … 疑似プラットフォーム(型、ImpEx、FlexibleSearch、ModelService、OCC の土台)
│           └── custom/
│               ├── xyzcore/     … items.xml、ImpEx、Service、DAO、xyzcore-spring.xml
│               ├── xyzfacades/  … Facade、Populator、Data
│               └── xyzocc/      … OCC の Controller、WsDTO、fieldSetLevelMapping
├── api/occ-openapi.yaml       … OCC と同じ形の OpenAPI(50 本)
├── docs/                      … 設計書、ADR、手順書(シナリオの材料)
├── bench/
│   ├── scenarios/             … シナリオ 1 本 1 ファイル(依頼文とやり直しの文を固定する)
│   ├── bugs/                  … バグ修正のシナリオ用に仕込むバグ(1 本 1 パッチ)
│   ├── run.sh                 … シナリオを回して記録する
│   └── results/               … cost-log.csv と集計の表
├── tools/                     … データを作るスクリプト(人が読める量のコードにする)
└── .build-log/                … 計測台を作る作業そのものの時間とトークン(計測の結果とは混ぜない)
```

### データの量(ImpEx で入れる)
- 商品 1,000 / カテゴリ 100 / 顧客 1,000 / 注文 5,000 / B2B の組織 100
- CMS: ページ 100 / コンポーネント 500 / メディア 1,000(画像そのものは置かず、URL だけ)
- OCC の端点 50 本(/products、/categories、/carts、/orders、/users、/cms など。標準の形に合わせる)
- 大量のデータは `tools/` のスクリプトで作り、手で書かない。乱数は種を固定して、何度作っても同じ中身にする

## 作る順番
1. `storefront/` を Angular CLI(21)で SSR 付きで作り、疑似ライブラリの骨組みを置いて、SSR のビルドと起動が通るところまで。(**済み**: 2026-10-20)
2. Docker Compose と疑似プラットフォーム: `ant build` で items.xml から Model と表ができ、`ant initialize` で ImpEx が入り、`./hybrisserver.sh` で OCC が応答するところまで。(**済み**: 2026-10-20)
3. 拡張(xyzcore / xyzfacades / xyzocc)と ImpEx のデータ、OCC 50 本、OpenAPI。`ant unittests` が通るところまで。(**済み**: 2026-10-20)
   - データは `node tools/generate-sampledata.mjs` で作る(種を固定。出力の ImpEx はコミットする)。
   - OCC の 50 本は `node tools/occ-smoke.mjs` で確かめる(サーバーが動いている必要がある)。
   - `*-beans.xml` から Data / WsDTO が作られる(本物と同じ)。項目を足すときは自分の拡張の beans.xml に同じ class の bean を書く。
4. ストアフロントを本物の OCC につなぎ、商品一覧・詳細・カート・チェックアウトが画面に出るところまで。MSW のモックも用意する。(**済み**: 2026-10-20)
   - 標準の CMS 部品(Spartacus の機能ライブラリに当たる)は `xyz-storefront-lib/cms-components/`。対応表は `cms-components.module.ts`。
   - モックは `storefront/mocks/`(`npm run start:mock` で :4200)。応答は `npm run mocks:capture` で本物の OCC から録り直す。MSW 3 では `onUnhandledRequest` が `onUnhandledFrame` に変わった(実測)。
5. Playwright の 4 本が、Docker Compose で起動した環境で通るところまで。(**済み**: 2026-10-20)
   - 本物の環境: `docker compose --profile storefront up -d` のあと `npm run e2e -- --project=real`(8 本)。
   - モック: `E2E_MOCK=1 npm run e2e -- --project=mock`(@mock の 4 本)。
   - SSR の HTML のまま押した操作は取りこぼされることがある。テストは `e2e/support.ts` の `open()` で、画面が動き出すのを待ってから操作する。
6. 自社の部品と docs(設計書 5〜10 本、ADR 数本)。`.claude/` にキットを戻し、`CLAUDE.project.md` を作る。
7. `bench/` のシナリオ、仕込むバグ、実行スクリプト。
- 各段の終わりに、動くことを確かめたコマンドと結果を報告する。動かなかったことも書く。
- 各段の終わりに、作業の時間とトークン(入力・キャッシュ書き込み・キャッシュ読み込み・出力)を `.build-log/cost-log.csv` に記録して報告する:
  `node kit/.claude/skills/cs-cost/scripts/cost.mjs --label "<段>" --log .build-log/cost-log.csv`

## 計測のシナリオ(bench/scenarios に 1 本 1 ファイル)

### 開発
| 番号 | 担当 | 依頼 |
|---|---|---|
| S01 | cs-architect | B2B の承認機能の設計と ADR |
| S02 | cs-docs | 設計書のレビュー |
| S03 | cs-occ-liaison | 配送予定日を出すための OCC の依頼書 |
| S04 | cs-backend | 配送予定日の OCC 端点を足す(items.xml、ImpEx、WsDTO、Populator、Controller、テスト。`ant build` と `ant unittests` まで) |
| S05 | cs-frontend | 商品詳細に配送予定日を出す(モックを先に作り、そのあと本物の OCC につなぐ) |
| S06 | cs-qa | 配送予定日の Playwright のテスト |
| S07 | cs-auditor | S04〜S06 の差分のレビュー |
| S08 | cs-performance | 商品詳細の Lighthouse の分析 |
| S09 | cs-sre | 用意した障害のログから原因を切り分ける |
| S10 | cs-auditor | リポジトリ全体の監査(**別枠**。ストーリーの集計に入れない) |

### バグ修正(bench/bugs のパッチを当ててから始める)
| 番号 | 担当 | 仕込むバグ |
|---|---|---|
| B01 | cs-backend | Populator で項目を写し忘れ、OCC の応答から値が消える |
| B02 | cs-backend | ImpEx の書き間違いで `ant initialize` が途中で落ちる |
| B03 | cs-frontend | SSR のときだけ `window` を触って落ちる画面 |
| B04 | cs-backend | `@Secured` の付け間違いで、ログインしていない人がカートを見られない |
| B05 | cs-frontend | OCC の fields の指定漏れで、画面の値が出ない |

- 各ファイルに、最初の依頼文と、やり直しの文(「テストが落ちたので直して」「レビューの指摘を直して」)を 1〜2 本、**一字一句固定**して書く。バグ修正の依頼文は、利用者からの報告の形(症状だけ)にし、原因は書かない。
- 1 ストーリー = S01〜S07 のうち、そのストーリーで実際に通る担当の組。どの組をストーリーとするかを `bench/scenarios/README.md` に書く。
- バグ修正は、1 スプリントに何件あるかを人が決めて、ストーリーとは別に積み上げる。

## 計測のしかた
- 計測の前に `CLAUDE.md` を `CLAUDE.project.md` と置き換える。この文書は計測の間に読ませない(読む量が変わるため)。
- 各シナリオを **3 回以上**回す。毎回、作業ツリーと DB を計測前の状態に戻してから始める(`git` と `ant initialize`。`bench/run.sh` が手順を用意する)。
- 1 回ごとに新しい会話で始める。前の回の会話を続けない。
- 固定するもの: Claude Code の版、モデル、考える量(effort)、設定ファイル、リポジトリの中身、Docker のイメージ。どれかが変わったら、それまでの結果と混ぜない。
- 記録は、キットの cs-cost で取る。ラベルは `S05/cs-frontend/2回目` の形にする。
- 結果は、真ん中の値と最大値の両方を残す。予算は最大値の側で見る。
- 同じ条件で入力が桁違いに跳ねる回があった(2026-10-20 の試しで 51 回中 10 回、原因は未確認)。外すときは、外した回と理由を表に残す。

### 本物との差(報告に必ず書く)
- 疑似プラットフォームは本物より小さい。本物では、プラットフォームのソースや jar を読みに行く、ビルドのログがずっと長い、起動に時間がかかる、の分だけトークンと時間が増える(推論)。
- この差は、計測の数字には足さず、「補正の目安」として別の行に書く。

### Bedrock で動かすとき
- Claude Code を Bedrock につなぐ設定(`CLAUDE_CODE_USE_BEDROCK=1`、`AWS_REGION`、使うモデル)は、人が各自のシェルか `~/.claude/settings.json` に入れる。AWS の資格情報は Claude が読まない・表示しない。
- Bedrock でも会話の記録(`~/.claude/projects/`)に使用量が残るかを、最初の 1 回で確かめる(未確認)。残らなければ止めて人に相談する。

## cs-cost に足すこと(キットのリポジトリ側の作業)
- CSV に担当ごとの行を足す(今は会話の合計が 1 行)。
- かかった時間(最初と最後の応答の時刻の差)の列を足す。
- `rates.json` を差し替えられるようにする(`--rates`)。Bedrock 用の単価表を `rates.bedrock.json` として置く。
  - Opus 5.5 の Bedrock の単価: 二次情報(typingmind)では、Global が入力 $4・出力 $20、地域指定(US / EU / JP / AU)が 1.1 倍の入力 $4.40・出力 $22・キャッシュ読み込み $0.22・キャッシュ書き込み $5.50(いずれも 100 万トークンあたり)。**AWS の料金ページで確かめてから使う(未確認)。**
  - Anthropic の公式料金表にも「地域を限定すると 1.1 倍」とある(公式 Pricing)。

## 集計(bench/results)
- 列: シナリオ、担当、回、入力、キャッシュ書き込み、キャッシュ読み込み、出力、時間、金額。
- 1 ストーリー = その組の担当の真ん中の値の合計(最大値の合計も並べる)。
- 1 スプリント = 1 ストーリー × スプリントあたりのストーリー数 + バグ修正 1 件 × スプリントあたりの件数。1 人月 = 人が月に回すストーリー数とバグ修正の件数で掛ける。件数は人が決めて表に書く(Claude は決めない)。
- 月額 = 4 つのトークン数 × Bedrock の単価(4 つ別々に掛けてから足す)。

## 書き方の決まり
- 事実の文には根拠の印を付ける: (実測)(ソース)(公式)(推論)(未確認)。推論と未確認は必ず印を付ける。
- 説明は日常の言葉で書き、専門用語は最初に一言で言い換える。
- 報告の最後に「会議で言える 3 文」を付ける。
- 選択肢を並べて終わらず、1 つ推奨する。

## やらないこと
- 資格情報(.npmrc、AWS、.env)を読む・書く・表示する。
- 本物の SAP のソースや顧客のデータを持ち込む。
- 計測の途中でリポジトリやバージョンを変える(変えるなら、それまでの結果と分ける)。
- push、プルリクエスト、コミットは人がする。
