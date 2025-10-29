package com.example.darkchar.ui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.darkchar.service.OpenAiApiKeyStore;
import com.example.darkchar.service.OpenAiModelStore;
import com.example.darkchar.service.openai.OpenAiIntegrationException;
import com.example.darkchar.service.openai.OpenAiModelCatalogClient;
import com.example.darkchar.ui.AppStyleUtil;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * OpenAI APIキーとモデル設定を行うダイアログのコントローラ。
 */
@Component
public class SettingsController {

    private final OpenAiApiKeyStore apiKeyStore;
    private final OpenAiModelStore modelStore;
    private final OpenAiModelCatalogClient modelCatalogClient;

    private List<String> currentModels = new ArrayList<>();

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> modelComboBox;

    @FXML
    private Button loadModelsButton;

    private Stage stage;

    public SettingsController(OpenAiApiKeyStore apiKeyStore,
            OpenAiModelStore modelStore,
            OpenAiModelCatalogClient modelCatalogClient) {
        this.apiKeyStore = apiKeyStore;
        this.modelStore = modelStore;
        this.modelCatalogClient = modelCatalogClient;
    }

    @FXML
    public void initialize() {
        if (modelComboBox != null) {
            modelComboBox.setItems(FXCollections.observableArrayList());
            modelComboBox.setDisable(true);
            modelComboBox.setPromptText("モデルを取得してください");
        }
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
            modelStore.clear();
            currentModels = new ArrayList<>();
            updateStatus("APIキーをクリアしました。");
            closeStage();
            return;
        }

        if (modelComboBox == null || modelComboBox.isDisabled() || modelComboBox.getItems().isEmpty()) {
            updateStatus("モデル一覧を取得してから保存してください。");
            return;
        }
        String selected = modelComboBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank()) {
            updateStatus("使用するモデルを選択してください。");
            return;
        }

        apiKeyStore.setApiKey(key);
        modelStore.setAvailableModels(currentModels);
        modelStore.setSelectedModel(selected);
        updateStatus("APIキーとモデルを保存しました。");
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
        modelStore.clear();
        currentModels = new ArrayList<>();
        if (modelComboBox != null) {
            modelComboBox.getItems().clear();
            modelComboBox.setDisable(true);
        }
        updateStatus("APIキーをクリアしました。");
    }

    @FXML
    void handleLoadModels(ActionEvent event) {
        String inputKey = apiKeyField.getText();
        if (inputKey != null) {
            inputKey = inputKey.trim();
        }
        if (inputKey == null || inputKey.isEmpty()) {
            showWarning("APIキーを入力してください。");
            return;
        }
        final String key = inputKey;

        if (loadModelsButton != null) {
            loadModelsButton.setDisable(true);
        }
        if (modelComboBox != null) {
            modelComboBox.setDisable(true);
        }
        updateStatus("モデル一覧を取得しています...");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return modelCatalogClient.listModels(key);
            }
        };

        task.setOnSucceeded(e -> {
            currentModels = new ArrayList<>(task.getValue());
            modelStore.setAvailableModels(currentModels);
            if (modelComboBox != null) {
                modelComboBox.getItems().setAll(currentModels);
                modelComboBox.setDisable(currentModels.isEmpty());
                Optional<String> selected = modelStore.getSelectedModel();
                if (selected.isPresent() && currentModels.contains(selected.get())) {
                    modelComboBox.getSelectionModel().select(selected.get());
                } else {
                    modelComboBox.getSelectionModel().clearSelection();
                }
            }
            updateStatus(currentModels.isEmpty() ? "利用可能なモデルが見つかりませんでした。" : "モデルを選択してください。");
            if (loadModelsButton != null) {
                loadModelsButton.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            Throwable throwable = task.getException();
            if (throwable instanceof OpenAiIntegrationException) {
                showWarning(throwable.getMessage());
            } else {
                showWarning("モデル一覧の取得に失敗しました。");
            }
            updateStatus("モデル一覧の取得に失敗しました。");
            if (loadModelsButton != null) {
                loadModelsButton.setDisable(false);
            }
            if (modelComboBox != null) {
                modelComboBox.getItems().setAll(currentModels);
                modelComboBox.setDisable(currentModels.isEmpty());
            }
        });

        Thread thread = new Thread(task, "openai-model-fetcher");
        thread.setDaemon(true);
        thread.start();
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
        currentModels = new ArrayList<>(modelStore.getAvailableModels());
        if (modelComboBox != null) {
            modelComboBox.getItems().setAll(currentModels);
            boolean hasModels = !currentModels.isEmpty();
            modelComboBox.setDisable(!hasModels);
            Optional<String> selected = modelStore.getSelectedModel();
            if (selected.isPresent() && currentModels.contains(selected.get())) {
                modelComboBox.getSelectionModel().select(selected.get());
            } else {
                modelComboBox.getSelectionModel().clearSelection();
            }
        }
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

    private void showWarning(String message) {
        Alert alert = new Alert(AlertType.WARNING);
        // alert.setTitle("OpenAIモデル取得エラー");
        alert.setHeaderText(null);
        String content = (message == null || message.isBlank()) ? "モデル一覧の取得に失敗しました。" : message;
        alert.setContentText(content);
        // アプリ共通の stylesheet / フォント設定をAlertに適用
        AppStyleUtil.applyToAlert(alert);
        alert.showAndWait();
    }
}
