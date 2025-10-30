package com.example.darkchar.ui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.ProviderType;
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
import javafx.util.StringConverter;

/**
 * AIプロバイダ設定を行うダイアログのコントローラ。
 */
@Component
public class SettingsController {

    private final AiProviderContextStore providerContextStore;
    private final CharacterGenerationStrategyRegistry strategyRegistry;

    private List<String> currentModels = new ArrayList<>();
    private ProviderType currentProviderType;
    private boolean suppressProviderSelectionEvents;

    @FXML
    private ComboBox<ProviderType> providerComboBox;

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> modelComboBox;

    @FXML
    private Button loadModelsButton;

    private Stage stage;

    public SettingsController(AiProviderContextStore providerContextStore,
            CharacterGenerationStrategyRegistry strategyRegistry) {
        this.providerContextStore = providerContextStore;
        this.strategyRegistry = strategyRegistry;
    }

    @FXML
    public void initialize() {
        if (providerComboBox != null) {
            providerComboBox.setItems(FXCollections.observableArrayList(strategyRegistry.getRegisteredProviderTypes()));
            providerComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(ProviderType object) {
                    return object == null ? "" : object.getDisplayName();
                }

                @Override
                public ProviderType fromString(String string) {
                    return providerComboBox.getItems().stream()
                            .filter(type -> type.getDisplayName().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });
            providerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (suppressProviderSelectionEvents) {
                    return;
                }
                if (newVal != null) {
                    refreshForProvider(newVal);
                }
            });
        }

        if (modelComboBox != null) {
            modelComboBox.setItems(FXCollections.observableArrayList());
            modelComboBox.setDisable(true);
            modelComboBox.setPromptText("モデルを取得してください");
        }

        if (loadModelsButton != null) {
            loadModelsButton.setDisable(true);
        }

        refreshFromStore();
    }

    @FXML
    void handleSave(ActionEvent event) {
        ProviderType providerType = getCurrentProviderType();
        CharacterGenerationProvider provider = getCurrentProvider();

        String key = apiKeyField.getText();
        if (key != null) {
            key = key.trim();
        }
        if (key == null || key.isEmpty()) {
            providerContextStore.clear(providerType);
            currentModels = new ArrayList<>();
            if (modelComboBox != null) {
                modelComboBox.getItems().clear();
                modelComboBox.setDisable(true);
            }
            updateStatus(providerDisplayName(providerType) + "の設定をクリアしました。");
            closeStage();
            return;
        }

        if (provider != null && provider.supportsModelListing()) {
            if (modelComboBox == null || modelComboBox.isDisabled() || modelComboBox.getItems().isEmpty()) {
                updateStatus("モデル一覧を取得してから保存してください。");
                return;
            }
            String selected = modelComboBox.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isBlank()) {
                updateStatus("使用するモデルを選択してください。");
                return;
            }
            providerContextStore.setAvailableModels(providerType, currentModels);
            providerContextStore.setSelectedModel(providerType, selected);
        } else {
            providerContextStore.setAvailableModels(providerType, List.of());
            providerContextStore.setSelectedModel(providerType, null);
        }

        providerContextStore.setApiKey(providerType, key);
        providerContextStore.setActiveProviderType(providerType);
        updateStatus(providerDisplayName(providerType) + "の設定を保存しました。");
        closeStage();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage();
    }

    @FXML
    void handleClear(ActionEvent event) {
        ProviderType providerType = getCurrentProviderType();
        providerContextStore.clear(providerType);
        currentModels = new ArrayList<>();
        if (apiKeyField != null) {
            apiKeyField.clear();
        }
        if (modelComboBox != null) {
            modelComboBox.getItems().clear();
            modelComboBox.setDisable(true);
        }
        updateStatus(providerDisplayName(providerType) + "の設定をクリアしました。");
    }

    @FXML
    void handleLoadModels(ActionEvent event) {
        ProviderType providerType = getCurrentProviderType();
        CharacterGenerationProvider provider = getCurrentProvider();
        if (provider == null || !provider.supportsModelListing()) {
            showWarning("このプロバイダではモデル一覧を取得できません。");
            return;
        }

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
                return provider.listAvailableModels(key);
            }
        };

        task.setOnSucceeded(e -> {
            currentModels = new ArrayList<>(task.getValue());
            providerContextStore.setAvailableModels(providerType, currentModels);
            if (modelComboBox != null) {
                modelComboBox.getItems().setAll(currentModels);
                boolean hasModels = !currentModels.isEmpty();
                modelComboBox.setDisable(!hasModels);
                Optional<String> selected = providerContextStore.getSelectedModel(providerType);
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
            String message = (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank())
                    ? "モデル一覧の取得に失敗しました。"
                    : throwable.getMessage();
            showWarning(message);
            updateStatus("モデル一覧の取得に失敗しました。");
            if (loadModelsButton != null) {
                loadModelsButton.setDisable(false);
            }
            if (modelComboBox != null) {
                modelComboBox.getItems().setAll(currentModels);
                modelComboBox.setDisable(currentModels.isEmpty());
            }
        });

        Thread thread = new Thread(task, providerType.name().toLowerCase() + "-model-fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void refreshFromStore() {
        ProviderType providerType = providerContextStore.getActiveProviderType();
        if (providerComboBox != null) {
            suppressProviderSelectionEvents = true;
            if (!providerComboBox.getItems().contains(providerType)) {
                providerComboBox.getItems().add(providerType);
            }
            providerComboBox.getSelectionModel().select(providerType);
            suppressProviderSelectionEvents = false;
        }
        refreshForProvider(providerType);
    }

    private void refreshForProvider(ProviderType providerType) {
        currentProviderType = providerType;
        AiProviderContext context = providerContextStore.getContext(providerType);
        if (apiKeyField != null) {
            apiKeyField.setText(context.apiKey().orElse(""));
        }
        currentModels = new ArrayList<>(context.availableModels());

        CharacterGenerationProvider provider = getCurrentProvider();
        boolean supportsModels = provider != null && provider.supportsModelListing();

        if (modelComboBox != null) {
            modelComboBox.getItems().setAll(currentModels);
            if (supportsModels) {
                boolean hasModels = !currentModels.isEmpty();
                modelComboBox.setDisable(!hasModels);
                Optional<String> selected = context.selectedModel();
                if (selected.isPresent() && currentModels.contains(selected.get())) {
                    modelComboBox.getSelectionModel().select(selected.get());
                } else {
                    modelComboBox.getSelectionModel().clearSelection();
                }
            } else {
                modelComboBox.getSelectionModel().clearSelection();
                modelComboBox.setDisable(true);
            }
        }

        if (loadModelsButton != null) {
            loadModelsButton.setDisable(!supportsModels);
        }

        boolean hasKey = providerContextStore.hasApiKey(providerType);
        updateStatus(hasKey ? providerDisplayName(providerType) + "のAPIキーが設定されています。"
                : providerDisplayName(providerType) + "のAPIキーは未設定です。");
    }

    private ProviderType getCurrentProviderType() {
        return currentProviderType == null ? providerContextStore.getActiveProviderType() : currentProviderType;
    }

    private CharacterGenerationProvider getCurrentProvider() {
        ProviderType providerType = getCurrentProviderType();
        return strategyRegistry.findProvider(providerType).orElse(null);
    }

    private String providerDisplayName(ProviderType providerType) {
        return strategyRegistry.findProvider(providerType)
                .map(CharacterGenerationProvider::getDisplayName)
                .orElse(providerType.getDisplayName());
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
        alert.setHeaderText(null);
        String content = (message == null || message.isBlank()) ? "モデル一覧の取得に失敗しました。" : message;
        alert.setContentText(content);
        AppStyleUtil.applyToAlert(alert);
        alert.showAndWait();
    }
}
