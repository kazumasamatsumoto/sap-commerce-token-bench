# E01 要望から通しで回す「お届け予定日」(cs-pdm から始めて、担当が順に引き継ぐ)

- 担当: cs-pdm(最初の担当。続けて cs-occ-liaison → cs-backend → cs-frontend → cs-qa → cs-auditor を本会話が順に呼ぶ)
- 種類: 開発(通し。S03〜S07 のストーリーと同じ機能を、1 つの会話で要望からレビューまで進める)
- 前準備: なし
- 時間の上限: 150 分

## このシナリオの考え方
- S03〜S07 は担当ごとに別の会話で回す。E01 は、本物の進め方に近い「1 つの要望を 1 つの会話に渡し、本会話がまとめ役として担当を順に呼ぶ」形で回す。
- Claude Code では担当(サブエージェント)が別の担当を呼べないので、まとめ役は本会話になる。本会話は担当の報告を受け取るたびに長くなるので、キャッシュ読み込みは S03〜S07 の合計より増える見込み(推論)。
- 計測では人が途中で答えられないので、依頼文で「確認を待たずに最後まで進める」ことを指示する。本物では途中で人の確認が入る(その分の会話は数えていない)。
- 集計では、S03〜S07 のストーリーとは別の行に出す(混ぜない)。担当ごとの内訳は bench/results/cost-by-agent.csv。

## 依頼文
<!-- prompt -->
営業から次の要望が来ました。この要望を、cs-pdm から始めて、各担当に順に引き継いで、実装とテストとレビューまで進めてください。
私は途中で答えられないので、確認を待たずに、各担当の判断で最後まで進めてください。決められないことは前提として書いて先へ進んでください。

要望: 「商品詳細ページに、いつ届くかを出してほしい。問い合わせの多くが『いつ届くか』なので、注文前に分かるようにしたい。」
営業との打ち合わせで決まった条件:
- 商品ごとに出荷までの日数(1〜5 日)を持たせ、サンプルデータの全商品に値を入れる。
- 届け先の郵便番号から配送にかかる日数を足す(北海道と沖縄は 3 日、それ以外は 1 日。郵便番号が無ければ 1 日)。郵便番号は、まだ画面で入れてもらわない。
- 画面には、価格の下に「お届け予定日: 10月23日(木)〜10月25日(土)」のように、最短の日〜最長の日(最長は最短の 2 日後)を出す。

進め方:
1. cs-pdm: ユーザーストーリーと受入条件を docs/stories/delivery-estimate.md に書く。
2. cs-occ-liaison: いまの OCC で足りるかを確かめ、足りなければバックエンドへの依頼書を docs/occ-requests/delivery-estimate.md に書く。
3. cs-backend: 依頼書のとおりに OCC を作る(items.xml、ImpEx、WsDTO、Populator、Controller、Spock の単体テスト)。ant all と ant unittests が通るところまで確かめる。
4. cs-frontend: まず MSW のモックで画面を仕上げ、そのあと本物の OCC につないで確かめる。SSR で壊れないこと、文言は翻訳キーにすること。npm run build:ssr と単体テストが通るところまで確かめる。
5. cs-qa: お届け予定日の E2E テストを、本物の環境(npm run e2e -- --project=real)とモックの環境(@mock)の両方で通るように作る。既存のテストも全部通ることを確かめる。
6. cs-auditor: まだコミットしていない差分全部を、案件のルール、セキュリティ、SSR、テストの抜けの観点でレビューし、指摘を重い順に docs/review/delivery-estimate-review.md にまとめる(コードは直さない)。
最後に、各担当が何をしたかと、残った課題を短く報告してください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
確かめたら、ビルドかテストが落ちるか、商品詳細にお届け予定日が出ていませんでした。ant all、ant unittests、npm run build:ssr、単体テスト、npm run e2e -- --project=real と、商品詳細の画面をもう一度確かめて、担当に直してもらってください。
<!-- /retry -->
<!-- retry -->
まだ通りません。docs/stories/delivery-estimate.md と docs/review/delivery-estimate-review.md があること、http://localhost:4000/product/XYZ-000001 の SSR の HTML に「お届け予定日」が出ること、E2E が全部通ることを確かめて直してください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
test -s docs/stories/delivery-estimate.md && test -s docs/occ-requests/delivery-estimate.md && test -s docs/review/delivery-estimate-review.md && bench_ant all unittests initialize && bench_restart_commerce && bench_storefront_build && bench_storefront_unit && bench_restart_storefront && bench_ssr_get /product/XYZ-000001 | grep -q 'お届け予定日' && git status --porcelain -- storefront/e2e | grep -q . && bench_e2e_real
<!-- /check -->
