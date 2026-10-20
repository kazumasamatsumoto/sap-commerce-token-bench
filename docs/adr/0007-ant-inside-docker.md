# 0007. ant のコマンドは本物と同じ名前で呼び、中身は Docker の Gradle で動かす
日付: 2026-10-20
状態: 承認

## 背景
- 担当が本物と同じコマンド(`ant clean all`、`ant build`、`ant initialize`、`ant updatesystem`、`ant unittests`、`./hybrisserver.sh`)を使うことが要る(ソース: `claude.md` の「コマンドは本物と同じ名前で呼べるようにする」)。
- 開発のパソコンには Gradle も Maven も無い。Java 21 はあるが、版を人ごとに揃えるのは難しい(ソース: `claude.md` の版の表)。
- 全体は Docker Compose で起動する(ソース: 同)。

## 選択肢
1. `hybris/bin/platform/ant`(と `ant.cmd`)を入口にし、`docker compose run builder` の中の Gradle で動かす: 利点 = パソコンに Java も Gradle も要らない。コマンドの名前と場所が本物と同じ。欠点 = 毎回コンテナを起動するので、少し遅い。Windows の PowerShell では `-D` の引数の書き方に注意が要る。コスト = 小。
2. パソコンに Gradle を入れ、`ant` を Gradle の別名にする: 利点 = 速い。欠点 = 版が人ごとにずれる。Windows と macOS で手順が分かれる。
3. 何もしない(Gradle のタスク名で直接呼ぶ): 欠点 = 担当の作業が本物と変わり、計測がずれる。

## 決定
案 1。
1. ant のターゲットを Gradle と `commerce.jar` の起動に読み替える(ソース: `core-customize/hybris/bin/platform/resources/ant/ant-impl.sh:1-53`)。
2. 出力を本物の ant に近づける(`Buildfile: ...`、`<ターゲット>:`、`[echo] building extension '...'`、`BUILD SUCCESSFUL` / `BUILD FAILED`)(ソース: 同 29 行、`core-customize/hybris/bin/platform/build.gradle:96-99`)。
3. Gradle の依存はボリューム `gradle-home` に置き、2 回目からの取得を省く(ソース: `docker-compose.yml:22-31`)。

## 根拠
- builder のイメージは `gradle:8.14.6-jdk21-noble`、`--profile tools` のときだけ使う(ソース: `docker-compose.yml:22-34`)。
- 入口: `core-customize/hybris/bin/platform/ant:5`、`ant.cmd:5`。Windows の注意は `ant.cmd:3`(PowerShell では `'"-Dresource=..."'` と囲む)。
- `-D` の引数は Gradle の `-P` に読み替える(ソース: `ant-impl.sh:10`)。

## 影響
- 開発者: Docker Desktop が要る。Windows では `ant.cmd` と `hybrisserver.bat` を使う(`docs/runbook/local-environment.md`)。
- Commerce のコンテナ(`commerce`)は builder が作った `build/libs/commerce.jar` を動かす。先に `ant all` が要る(ソース: `docker-compose.yml:36-39`、`ant-impl.sh:18-21`)。
- 計測: コンテナの起動の分だけ時間が延びる。トークンには影響しない(推論)。
- 戻し方: パソコンに Gradle を入れ、`ant` の中身を `bash resources/ant/ant-impl.sh "$@"` に変える(DB の接続先は `HYBRIS_DB_URL` か `local.properties` の `db.url` で `localhost:5432`)(ソース: `core-customize/hybris/config/local.properties:1-5`)。

## 未決事項
- 本物の ant にあるターゲット(`ant customize`、`ant production` など)を足すか(必要になったら cs-backend が決める)。
