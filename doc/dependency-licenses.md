# 依存ライブラリおよび取得アセットのライセンス一覧

本プロジェクトで利用している主要な依存ライブラリと、ビルド時に取得するアセットの代表的なライセンス情報をまとめています。実際に配布・提供を行う際は各プロジェクトの最新ライセンス条文および NOTICE の確認、ならびに二次依存関係の追加調査を行ってください。

| 区分 | コンポーネント | 取得元 / バージョン等 | ライセンス | 備考 |
| --- | --- | --- | --- | --- |
| ランタイム | Spring Boot スターター群<br>`spring-boot-starter`<br>`spring-boot-starter-jdbc`<br>`spring-boot-starter-json`<br>`spring-boot-starter-web` | Maven Central（Spring Boot 3.2.5 系） | Apache License 2.0 | Spring プロジェクト共通ライセンス。再配布時は NOTICE への記載が推奨されます。 |
| ランタイム | H2 Database<br>`com.h2database:h2` | Maven Central | EPL 1.0 または MPL 2.0（二重ライセンス） | いずれか一方の条件を満たすことで利用可能です。 |
| ランタイム | JavaFX モジュール<br>`org.openjfx:javafx-controls`<br>`org.openjfx:javafx-fxml` | Maven Central（JavaFX 21.0.5 系） | GPLv2 with Classpath Exception | Classpath 例外によりアプリケーションは GPL を継承せずに配布可能です。 |
| ランタイム | OpenAI Java SDK<br>`com.openai:openai-java:4.6.1` | Maven Central | MIT License | 商用利用可。再配布時は著作権表示とライセンス文の保持が必要です。 |
| テスト | Spring Boot Test Starter<br>`spring-boot-starter-test` | Maven Central | Apache License 2.0 | 上記スターターと同様に Apache 2.0。JUnit など二次依存のライセンスにも留意してください。 |
| バンドル資産 | Noto Sans CJK JP フォント<br>`NotoSansCJKjp-Regular.otf`（ビルド時ダウンロード） | Google Fonts | SIL Open Font License 1.1 | 再配布時は OFL の要件（フォント名の変更不可など）を遵守してください。 |

> **補足:** ここに記載した情報は公式配布情報に基づきます。バージョンアップ等によりライセンス条件が変更される可能性があるため、定期的な確認を推奨します。
