package com.example.darkchar;

import com.example.darkchar.ui.DarkCharacterGeneratorFxApp;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションエントリポイントを提供します。
 */
@SpringBootApplication
public class DarkCharacterGeneratorApplication {

    /**
     * Spring Boot と JavaFX を起動します。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        Application.launch(DarkCharacterGeneratorFxApp.class, args);
    }
}
