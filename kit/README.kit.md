# Composable Storefront 向け Claude Code エージェントとスキル一式

SAP Commerce Cloud の Composable Storefront(Spartacus / Angular)プロジェクトで使う、
Claude Code のサブエージェント(.claude/agents)、スキル(.claude/skills)、hooks(.claude/hooks と .claude/settings.json)のセットです。

## 入れ方
1. この zip をストアフロントのリポジトリの直下で展開する(`.claude/` が作られます)
2. `CLAUDE.md.example` を参考に、リポジトリの CLAUDE.md にルールとコマンドを書く
3. 新しいセッションで「cs-frontend に商品詳細のバナー追加を頼んで」のように委任して試す

## 中身
- agents/: 13 人(フロントエンド、設計、運用、SRE、監査、PdM、テスト、性能、更新、文言と CMS、OCC の契約、バックエンド、ドキュメント)
- hooks/ と settings.json: 4 本の hooks(危険なコマンドを止める、秘密・標準ソース・dist への書き込みを止める、編集後の検査、会話の最初にリポジトリの種類を伝える)。jq が必要。既存の .claude/settings.json があるなら hooks と permissions の部分を手で足す
- hooks は bash 版(*.sh。jq が必要)と PowerShell 版(*.ps1。Windows に最初から入っている PowerShell 5.1 で動き、jq は不要)の 2 組。Windows で Git Bash が無いなら、settings.powershell.json を settings.json として置く(両方を登録しない)
- settings.json の permissions: 権限ルール(Claude Code 自身が止める禁止リスト)。秘密のファイルを読むこと、標準ソース・node_modules・dist への書き込み、ant initialize、パッケージの公開は禁止(deny)。git push / reset / clean と外へのダウンロードは毎回確認(ask)。git の閲覧、npm の build・test・lint、コストの集計スクリプトは確認なし(allow。フォルダを信頼した後に効く)。既存の settings.json があるなら、deny / ask / allow のリストに項目を足す。サンドボックスは各自の ~/.claude/settings.json に人が入れる(解説: https://learn-sap-commerce.vercel.app/learn/ai-driven-development/14-permission-rules-and-sandbox)
- skills/: 20 本の手順書(コンポーネント差し替え、OCC 追加とモック、SSR 確認、CCv2 リリース、障害切り分け、
  アクセシビリティ監査、セキュリティ監査、性能予算、バージョンアップ、ユーザーストーリー、ADR、E2E、リリースノート、SLO、レビュー観点、Commerce 側の OCC 拡張、型の追加と ImpEx、文書の型(templates/)、PowerPoint と Excel の文字起こし(scripts/。python3 か PowerShell で動き、追加のインストールは不要)、コストの集計(cs-cost))
- cs-cost: 依頼を 1 件終えるたびに、本会話と担当が使ったトークン数と API 料金に換算した金額を表にして報告に付け、docs/cost-log.csv に 1 行足す。node が必要。単価は skills/cs-cost/rates.json(公式の料金表を 2026-10-20 に写したもの)。CLAUDE.md.example と、Bash を持つ 10 人の担当に「終わるときに実行する」と書いてある

説明と使い方: https://learn-sap-commerce.vercel.app/learn/ai-driven-development/13-composable-storefront-agents-and-skills-kit

値や手順は各案件に合わせて書き換えてください。ツールの許可(allowed-tools / tools)は最小にしてあります。
