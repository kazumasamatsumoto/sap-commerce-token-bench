# 0002. 疑似プラットフォームを Spring Boot と PostgreSQL で作り、拡張の形は Commerce に合わせる
日付: 2026-10-20
状態: 承認

## 背景
- ADR 0001 で疑似環境にすると決めた。バックエンドの中身を何で作るかを決める必要がある。
- 要ること: items.xml から Model と表を作る(`ant build`、`ant updatesystem` に当たる)、ImpEx を読む、FlexibleSearch を SQL にする、ModelService・Converter / Populator・`@Secured` が使える(ソース: `claude.md` の「バックエンド」)。
- 制約: 本物の SAP のソースを使わない。パソコンに Java や Gradle を入れなくても動く(ADR 0007)。

## 選択肢
1. Spring Boot + JdbcTemplate + PostgreSQL で、Commerce と同じ名前のクラスと bean を自作する: 利点 = 本物も Spring なので、bean の XML、alias、`@Resource`、`@Secured` の書き方がそのまま使える。欠点 = 型の仕組み・ImpEx・FlexibleSearch を自分で書く手間。コスト = 中。
2. Spring Boot + JPA(Hibernate)で、Entity を手で書く: 利点 = 作る手間が少ない。欠点 = items.xml から作る流れにならず、担当の作業(items.xml → ant all → ant updatesystem)が本物と変わる。コスト = 小。
3. 何もしない(OCC の応答を固定の JSON で返すだけ): 利点 = 最も安い。欠点 = Service や Populator を直しても応答が変わらず、開発の作業が起きない。

## 決定
案 1。
1. items.xml → Model の生成 → 表の作成、という本物と同じ手順になる(ソース: `core-customize/hybris/bin/platform/build.gradle:78-89`、`core-customize/hybris/bin/platform-lite/src/de/hybris/platform/lite/persistence/SchemaManager.java:15-55`)。
2. bean の名前を本物に合わせられるので、拡張の `*-spring.xml` が本物と同じ書き方になる(ソース: `core-customize/hybris/bin/platform-lite/resources/platformlite-spring.xml:4-5`)。
3. DB は PostgreSQL 16 にする。本物の HANA との違いは、開発の手順とトークンの量にほぼ関係しない(推論)。

## 根拠
- Spring Boot 3.5.16、Java 21、PostgreSQL ドライバ 42.7.14、Spock 2.4(ソース: `core-customize/hybris/bin/platform/build.gradle:7,34,70,73-74`)。
- ImpEx は INSERT / INSERT_UPDATE / UPDATE / REMOVE、マクロ、`[unique=true]` などの修飾、参照の解決に対応。BeanShell・translator・processor は対応しない(ソース: `core-customize/hybris/bin/platform-lite/src/de/hybris/platform/lite/impex/ImpExImporter.java:30-40`)。
- FlexibleSearch は `{Product AS p}`、`{p.name[ja]}`、関連の型、`?param`、`{{ 副問い合わせ }}` に対応(ソース: `core-customize/hybris/bin/platform-lite/src/de/hybris/platform/lite/search/FlexibleSearchTranslator.java:23-31`)。
- 本物の HANA との違いが影響しないことは、測っていない(推論)。

## 影響
- コード: 型は親の表に子の型の行も入れ、`typecode` の列で見分ける。言語別の値は JSONB に入れる(ソース: `SchemaManager.java:57-125`)。
- 運用: `ant initialize` はスキーマを丸ごと消す(`DROP SCHEMA public CASCADE`)(ソース: `SchemaManager.java:31-36`)。
- 他チーム: 本物にある機能(Backoffice、HAC、キャッシュ、クラスタ、Solr)は無い。必要になったら別の ADR にする。
- 戻し方: 本物に移すとき、`platform-lite` を外して本物の platform を入れる。自社の拡張は書き方が同じなので、そのまま動くことを目指す(推論)。

## 未決事項
- 本物との書き方の違い(ImpEx の未対応の書き方など)が見つかったら、どちらに合わせるか(platform-lite の担当)。
