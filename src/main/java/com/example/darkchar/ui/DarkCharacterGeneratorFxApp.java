package com.example.darkchar.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.example.darkchar.DarkCharacterGeneratorApplication;

public class DarkCharacterGeneratorFxApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(DarkCharacterGeneratorFxApp.class);
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(DarkCharacterGeneratorApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/darkchar/ui/main-view.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        loadJapaneseFont(scene);
        stage.setTitle("闇堕ちキャラクターメーカー");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }

    private void loadJapaneseFont(Scene scene) {
        var stylesheetUrl = getClass().getResource("/com/example/darkchar/ui/application.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            logger.warn("Japanese UI stylesheet not found; falling back to default font rendering.");
        }

        try (var stream = getClass().getResourceAsStream("/fonts/NotoSansJP-Regular.otf")) {
            if (stream == null) {
                logger.warn("Japanese font resource not found; UI text may not render correctly.");
                return;
            }
            Font font = Font.loadFont(stream, 14);
            if (font == null) {
                logger.warn("Failed to load Japanese font resource; UI text may not render correctly.");
                return;
            }
            logger.info("Loaded Japanese UI font: {}", font.getName());
        } catch (Exception ex) {
            logger.warn("Error while loading Japanese font; UI text may not render correctly.", ex);
        }
    }
}
