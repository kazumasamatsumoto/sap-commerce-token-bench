I'm checking the latest versions. Spartacus `develop` is now on 2612.0.0 with Angular 21; next I'm confirming the release line and Bedrock pricing.

Versions are confirmed. Next I'm writing the CLAUDE.md for the new repository.

計測用の新しいリポジトリに CLAUDE.md を作りました。場所は `/Users/kazu/sap-research/cs-token-benchmark/CLAUDE.md` です。別のセッションでこのフォルダを開いて起動すれば、そのまま作り始められます。

## バージョンを最新にした結果

| もの | 元の案 | 今回 | 根拠 |
|---|---|---|---|
| Composable Storefront | 書かれていない | release/221121.22.x の系列(アプリの版 2611.0.0、2026-10-16 のコミット) | ソース(SAP/spartacus の package.json) |
| Angular | 18 + Universal | **21.2 系**と `@angular/ssr` | ソース |
| Node.js | なし | **22 系**(`^22.22.0`) | ソース(Spartacus の指定) |
| NgRx / RxJS / TypeScript | なし | 21.0 / 7.8 / 5.9 | ソース |
| Playwright | なし | 1.64.1 | 実測(npm) |
| MSW | なし | 3.0.3 | 実測(npm) |
| Claude Code | なし | 2.1.304(計測の間は固定) | 実測(npm) |
| バックエンド | Spring Boot 3 + PostgreSQL 16 | SAP Commerce と同じ形のファイル(items.xml、ImpEx、OCC)。Java 21 の書き方 | 前提に合わせた。Java の版は推論 |

最新をそのまま使わなかったものがあります。
- **Angular は 22.2.3 が最新ですが、21 にしました。** Spartacus が対応しているのが 21 までだからです(ソース)。
- **Node は 24 が LTS の最新ですが、22 にしました。** 理由は Angular と同じです(ソース)。
- **TypeScript は 7.0 が最新ですが、5.9 にしました。** 理由は Angular と同じです(ソース)。

**Spartacus のライブラリは、公開の npm からは入りません。** 公開の npm の版は 4.3.8(2022 年)で止まっていて、今の版は SAP の配布元から入れます(実測)。配布元の資格情報は人が入れる、と CLAUDE.md に書きました。

## CLAUDE.md に書いたこと
- **目的**: 4 種類のトークン(入力、キャッシュ書き込み、キャッシュ読み込み、出力)を、ストーリー → スプリント → 人月 → 月額まで分けたまま積み上げます。
- **構成とデータ量**:
  - 商品 1,000、注文 5,000、CMS 500 件、OCC 50 本、E2E 4 画面です。
  - データはスクリプトで作り、種を固定して、何度作っても同じ中身になるようにします。
- **作る順番**: 7 段です。各段で、動いたことを報告させます。
- **シナリオ 10 本**:
  - 依頼文とやり直しの文を、一字一句固定します。
  - S10(全体監査)は量が桁違いなので、ストーリーの集計に入れず別枠にします。
- **計測のしかた**:
  - 計測の前に、CLAUDE.md を案件用の `CLAUDE.project.md` と置き換えます。この作業指示を読ませると、読む量が変わるためです。
  - 各シナリオを 3 回以上、毎回新しい会話で回します。
  - Claude Code の版、モデル、考える量(effort)は固定します。
- **cs-cost に足すこと**: 担当ごとの行、かかった時間の列、Bedrock 用の単価表です。
- **Bedrock の単価**: 二次情報では、Global が入力 $4・出力 $20、地域を指定すると 1.1 倍の入力 $4.40・出力 $22 です。AWS の料金ページは数字を取り出せなかったので、確かめてから使うように書きました(未確認)。地域指定で 1.1 倍になることは、Anthropic の料金表にも書かれています(公式)。
- **書き方の決まり**: 根拠の印、日常の言葉で書く、会議で言える 3 文、1 つ推奨すること、を入れました。
- **やらないこと**: 資格情報には触らないこと、push は人がすること、などを書きました。

**会議で言える 3 文**
1. 計測台は最新の Composable Storefront に合わせ、Angular 21、Node 22 で作ります。Angular 22 にしないのは、Spartacus がまだ対応していないからです。
2. バックエンドは Spring Boot ではなく、SAP Commerce と同じ形のファイルとモックで用意します。担当が実際に読むものと同じにするためです。
3. Bedrock の単価は地域を指定すると 1.1 倍になるので、AWS の料金ページで確かめてから月額に掛けます。

Sources:
- [SAP/spartacus(package.json、release/221121.22.x)](https://github.com/SAP/spartacus)
- [Claude Opus 5.5 on Bedrock の料金(typingmind、二次情報)](https://custom.typingmind.com/tools/estimate-llm-usage-costs/amazon-bedrock/global-anthropic-claude-opus-5-5)
- [LLM Reference: Claude Opus 5.5 on AWS Bedrock](https://www.llmreference.com/model/claude-opus-5-5/aws-bedrock)
- [Anthropic Pricing](https://platform.claude.com/docs/en/about-claude/pricing)
- [Composable Storefront Updates(SAP Learning)](https://learning.sap.com/courses/sap-commerce-cloud-developer/composable-storefront-updates)