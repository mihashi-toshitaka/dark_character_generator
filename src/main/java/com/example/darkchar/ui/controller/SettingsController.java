package com.example.darkchar.ui.controller;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.darkchar.service.OpenAiApiKeyStore;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * OpenAI APIキーの設定を行うダイアログのコントローラ。
 */
@Component
public class SettingsController {

    private final OpenAiApiKeyStore apiKeyStore;

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private Label statusLabel;

    private Stage stage;

    public SettingsController(OpenAiApiKeyStore apiKeyStore) {
        this.apiKeyStore = apiKeyStore;
    }

    @FXML
    public void initialize() {
        refreshFromStore();
    }

    @FXML
    void handleSave(ActionEvent event) {
        String key = apiKeyField.getText();
        if (key != null) {
            key = key.trim();
        }
        if (key == null || key.isEmpty()) {
            apiKeyStore.clear();
            updateStatus("APIキーをクリアしました。");
        } else {
            apiKeyStore.setApiKey(key);
            updateStatus("APIキーを保存しました。");
        }
        closeStage();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage();
    }

    @FXML
    void handleClear(ActionEvent event) {
        apiKeyField.clear();
        apiKeyStore.clear();
        updateStatus("APIキーをクリアしました。");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void refreshFromStore() {
        if (apiKeyField != null) {
            Optional<String> key = apiKeyStore.getApiKey();
            apiKeyField.setText(key.orElse(""));
        }
        updateStatus(apiKeyStore.hasApiKey() ? "APIキーが設定されています。" : "APIキーは未設定です。");
    }

    private void closeStage() {
        if (stage != null) {
            stage.close();
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
