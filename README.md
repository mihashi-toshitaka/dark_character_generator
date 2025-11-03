# 闇堕ちキャラクタージェネレーター

## アプリの概要
- スプリングブートとJavaFXで構築された、闇の雰囲気をまとったキャラクター設定を作成するデスクトップアプリです。
- 世界観ジャンル、キャラクター特性、闇堕ち要素などをGUI上で選択し、AIに頼らず手元でダークヒーロー像を生成できます。
- オート／セミオートモードの切り替え、立ち位置セグメント・闇堕ち度（50%-250% 100%で完全敵化）、フリーテキスト欄などを備え、物語づくりやゲーム企画のインスピレーションを得る用途に向いています。

## アプリの起動方法
1. **前提条件**
   - Java 21 が利用できる環境であること。
   - プロジェクトルートで `./gradlew` が実行できること（Gradle Wrapper を同梱しています）。

2. **開発モードで起動する**
   ```bash
   ./gradlew bootRun
   ```
   上記コマンドでSpring Bootアプリケーションが起動し、JavaFXウィンドウが表示されます。

3. **実行可能Jarを作成して起動する**
   ```bash
   ./gradlew bootJar
   java -jar build/libs/dark-character-generator.jar
   ```
   生成されたJarを実行すると、同様にデスクトップアプリが立ち上がります。

> **ヒント:** JavaFXはGUIを描画するため、ヘッドレス環境では正しく起動できません。GUI環境またはX11転送可能な環境で実行してください。

## Windows向けインストーラの作成（jpackage）

1. **追加の前提条件**
   - Windows 10/11 上で動作するマシンに JDK 21 がインストールされていること（`jpackage` は JDK 21 に同梱されています）。
   - `org.openjfx:javafx-<module>:21.0.5:win` のような JavaFX の Windows 向け分類子（classifier）付きアーティファクトへアクセスできること。Gradle は初回実行時にこれらを自動取得します。

2. **アプリイメージ（インストール不要のフォルダ）を生成する**
   ```powershell
   .\gradlew jpackageImage
   ```
   正常に完了すると `build/jpackage/image/` 以下に "Dark Character Generator" フォルダが生成され、`Dark Character Generator.exe` をそのまま実行できます。

3. **インストーラ（.exe）を生成する**
   ```powershell
   .\gradlew jpackageExe
   ```
   `build/jpackage/` 直下に `Dark Character Generator-<version>.exe` が作成されます。これを実行すると Windows の標準インストーラが起動し、スタートメニューおよびアンインストール情報が登録されます。

4. **インストール後の操作**
   - インストールしたアプリはスタートメニューまたはインストール先フォルダから起動できます。
   - アンインストールは Windows の「設定 > アプリ > インストールされているアプリ」から、またはインストール先に生成されるアンインストールショートカットから行えます。

5. **オプションのブランドリソース**
   - アプリやインストーラ用のアイコン（`.ico`）やライセンスファイルを用意する場合は、`src/main/jpackage/` ディレクトリを作成して格納してください。
   - 例としてアイコンを利用する場合、`src/main/jpackage/dcg.ico` を配置し、`gradlew` 実行時に `-PappIcon=src/main/jpackage/dcg.ico` を指定します。その他のリソースは `build.gradle` 内の `configureJpackageCommand` に対応する `jpackage` オプション（`--license-file` など）を追加することで組み込めます。
