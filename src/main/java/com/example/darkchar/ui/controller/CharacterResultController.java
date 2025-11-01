package com.example.darkchar.ui.controller;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.ui.AppStyleUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 生成結果表示ダイアログのコントローラです。
 */
@Component
public class CharacterResultController {

    @FXML
    private TextArea resultTextArea;

    @FXML
    private Button showPromptButton;

    private Stage stage;
    private String promptText;

    /**
     * ダイアログのステージを設定します。
     *
     * @param stage 表示ステージ
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 画面に表示する結果を設定します。
     *
     * @param generatedCharacter 生成キャラクター
     * @param prompt 使用プロンプト
     */
    public void setResult(GeneratedCharacter generatedCharacter, Optional<String> prompt) {
        if (generatedCharacter == null) {
            resultTextArea.clear();
        } else {
            resultTextArea.setText(generatedCharacter.narrative());
        }
        this.promptText = prompt.filter(p -> !p.isBlank()).orElse(null);
        if (showPromptButton != null) {
            showPromptButton.setDisable(this.promptText == null);
        }
    }

    /**
     * 閉じるボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleClose(ActionEvent event) {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * プロンプト表示ボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleShowPrompt(ActionEvent event) {
        if (promptText == null || promptText.isBlank()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("使用したプロンプト");
        alert.setTitle(null);
        if (stage != null) {
            try {
                alert.initOwner(stage);
                alert.initModality(Modality.WINDOW_MODAL);
            } catch (IllegalStateException ex) {
                // owner設定に失敗しても続行
            }
        }

        TextArea textArea = new TextArea(promptText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(50);
        textArea.setPrefRowCount(12);
        textArea.setMinHeight(Region.USE_PREF_SIZE);
        textArea.setMinWidth(Region.USE_PREF_SIZE);
        textArea.setFocusTraversable(false);

        alert.getDialogPane().setContent(textArea);
        AppStyleUtil.applyToAlert(alert);
        alert.showAndWait();
    }
}
