# S08 商品詳細の表示の性能の分析(cs-performance)

- 担当: cs-performance
- 種類: 開発(性能)
- 前準備: なし
- 時間の上限: 40 分

## 依頼文
<!-- prompt -->
「商品詳細の表示が遅い」という声が出ています。cs-performance に頼んで、商品詳細(/product/XYZ-000001)の表示の性能を分析してください。
SSR の応答時間、ブラウザに送る JavaScript の大きさ、OCC の呼び出しの数と fields、画像の扱いを計測し、
効果の大きい順に改善案を docs/perf/pdp-analysis.md にまとめてください。計測した値には、計った方法を書いてください。コードはまだ直さないでください。
<!-- /prompt -->

## やり直しの文(判定が通らなかったときに、この順で送る)
<!-- retry -->
docs/perf/pdp-analysis.md ができていません。分析の結果をこのファイルに書いてください。
<!-- /retry -->

## 判定(bench/run.sh が作業ツリーの最上位で実行する。0 で終われば通過)
<!-- check -->
test -s docs/perf/pdp-analysis.md
<!-- /check -->
