package com.example.darkchar.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.example.darkchar.DarkCharacterGeneratorApplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * JavaFX アプリケーションの起点となるクラスです。
 */
public class DarkCharacterGeneratorFxApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(DarkCharacterGeneratorFxApp.class);
    private ConfigurableApplicationContext context;

    /**
     * Spring コンテキストを初期化します。
     */
    @Override
    public void init() {
        context = new SpringApplicationBuilder(DarkCharacterGeneratorApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    /**
     * メインウィンドウを表示します。
     *
     * @param stage メインステージ
     * @throws Exception ロード失敗時
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/darkchar/ui/main-view.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        var bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(960, bounds.getWidth() * 0.9);
        double height = Math.min(720, bounds.getHeight() * 0.9);
        Scene scene = new Scene(root, width, height);
        loadJapaneseFont(scene);
        // OSによっては文字化けするのでstage.setTitleは使わない
        // stage.setTitle("闇堕ちキャラクターメーカー");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setMaxHeight(bounds.getHeight());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * アプリケーションを終了します。
     */
    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }

    /**
     * UI で日本語フォントが使えるよう設定します。
     *
     * @param scene 適用対象のシーン
     */
    private void loadJapaneseFont(Scene scene) {
        // 任意CSS（あるなら読み込む）
        var stylesheetUrl = getClass().getResource("/com/example/darkchar/ui/application.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            logger.warn("Japanese UI stylesheet not found; using code-based font setup.");
        }

        // リソース内の OTF を読み込む（※ build で /fonts に入っている前提）
        final String[] fontPaths = {
                "/fonts/NotoSansCJKjp-Regular.otf"
        };

        String chosenFamily = null;
        for (String cp : fontPaths) {
            String fam = loadFontAndGetFamily(cp, 14);
            if (fam != null && chosenFamily == null) {
                chosenFamily = fam; // 最初に成功したファミリ名を採用
            }
        }

        // 読み込めたファミリ名をそのまま Scene ルートに適用（←これが一番確実）
        if (chosenFamily != null) {
            String css = String.format(
                    "-fx-font-family: '%s','Noto Sans JP','Yu Gothic UI','Meiryo',sans-serif;", chosenFamily);
            scene.getRoot().setStyle(css);
            logger.info("Applied UI font-family: {}", css);
            // 一元化のため、AppStyleUtil に stylesheet とファミリ名を保存
            if (stylesheetUrl != null) {
                AppStyleUtil.setUiStylesheetUrl(stylesheetUrl.toExternalForm());
            }
            AppStyleUtil.setFontFamily(chosenFamily);
        } else {
            logger.warn("No Japanese font loaded from resources; UI text may show tofu glyphs.");
        }

        // デバッグ：利用可能ファミリを少しログ出し
        var sampleFamilies = javafx.scene.text.Font.getFamilies().stream()
                .filter(f -> f.toLowerCase().contains("noto") || f.contains("Gothic") || f.contains("Meiryo"))
                .limit(20).toList();
        logger.info("Font families (excerpt): {}", sampleFamilies);
    }

    /**
     * フォントを読み込みファミリ名を返します。
     *
     * @param cp   クラスパス
     * @param size 読み込むサイズ
     * @return ファミリ名
     */
    private String loadFontAndGetFamily(String cp, double size) {
        try (var in = getClass().getResourceAsStream(cp)) {
            if (in == null) {
                logger.debug("Font resource not found: {}", cp);
                return null;
            }
            var font = javafx.scene.text.Font.loadFont(in, size);
            if (font == null) {
                logger.warn("Failed to load font: {}", cp);
                return null;
            }
            // family と name をログで確認
            logger.info("Loaded font: name='{}', family='{}' (from {})", font.getName(), font.getFamily(), cp);
            return font.getFamily(); // ← “Noto Sans CJK JP” 等、実ファミリ名を返す
        } catch (Exception e) {
            logger.warn("Error while loading font: {}", cp, e);
            return null;
        }
    }

}
