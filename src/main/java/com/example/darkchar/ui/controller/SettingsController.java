package com.example.darkchar.ui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.GenerationModelCatalog;
import com.example.darkchar.service.ai.ProviderType;
import com.example.darkchar.ui.JapaneseTextInputSupport;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
    private final GenerationModelCatalog modelCatalog;

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

    private Stage stage;

    /**
     * 設定ストア・レジストリ・モデルカタログを注入します。
     *
     * @param providerContextStore プロバイダ設定ストア
     * @param strategyRegistry     プロバイダレジストリ
     * @param modelCatalog         モデルカタログ
     */
    public SettingsController(AiProviderContextStore providerContextStore,
            CharacterGenerationStrategyRegistry strategyRegistry, GenerationModelCatalog modelCatalog) {
        this.providerContextStore = providerContextStore;
        this.strategyRegistry = strategyRegistry;
        this.modelCatalog = modelCatalog;
    }

    /**
     * 画面初期化時のセットアップを行います。
     */
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
            modelComboBox.setEditable(true);
            modelComboBox.setDisable(true);
            modelComboBox.setPromptText("モデルIDを入力または選択");
            JapaneseTextInputSupport.enable(modelComboBox.getEditor());
        }

        refreshFromStore();
    }

    /**
     * 保存ボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleSave(ActionEvent event) {
        ProviderType providerType = getCurrentProviderType();

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
                modelComboBox.getSelectionModel().clearSelection();
                modelComboBox.getEditor().clear();
            }
            updateStatus(providerDisplayName(providerType) + "の設定をクリアしました。");
            closeStage();
            return;
        }

        if (modelCatalog.requiresModelSelection(providerType)) {
            String selected = extractModelInput();
            if (selected == null) {
                updateStatus("使用するモデルを選択してください。");
                return;
            }
            providerContextStore.setSelectedModel(providerType, selected);
            if (modelComboBox != null && !modelComboBox.getItems().contains(selected)) {
                modelComboBox.getItems().add(selected);
            }
        } else {
            providerContextStore.setSelectedModel(providerType, null);
        }

        providerContextStore.setApiKey(providerType, key);
        providerContextStore.setActiveProviderType(providerType);
        updateStatus(providerDisplayName(providerType) + "の設定を保存しました。");
        closeStage();
    }

    /**
     * キャンセルボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleCancel(ActionEvent event) {
        closeStage();
    }

    /**
     * クリアボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleClear(ActionEvent event) {
        ProviderType providerType = getCurrentProviderType();
        providerContextStore.clear(providerType);
        currentModels = new ArrayList<>();
        if (apiKeyField != null) {
            apiKeyField.clear();
        }
        if (modelComboBox != null) {
            if (modelCatalog.requiresModelSelection(providerType)) {
                modelComboBox.getItems().setAll(modelCatalog.listModels(providerType));
                modelComboBox.setDisable(false);
            } else {
                modelComboBox.getItems().clear();
                modelComboBox.setDisable(true);
            }
            modelComboBox.getSelectionModel().clearSelection();
            modelComboBox.getEditor().clear();
        }
        updateStatus(providerDisplayName(providerType) + "の設定をクリアしました。");
    }

    /**
     * 閉じ操作で利用するステージを設定します。
     *
     * @param stage 設定画面のステージ
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * ストアの内容で画面を更新します。
     */
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

    /**
     * 指定プロバイダの情報で画面を更新します。
     *
     * @param providerType プロバイダ種別
     */
    private void refreshForProvider(ProviderType providerType) {
        currentProviderType = providerType;
        AiProviderContext context = providerContextStore.getContext(providerType);
        if (apiKeyField != null) {
            apiKeyField.setText(context.apiKey().orElse(""));
        }
        boolean requiresModels = modelCatalog.requiresModelSelection(providerType);
        currentModels = requiresModels ? new ArrayList<>(modelCatalog.listModels(providerType)) : new ArrayList<>();
        if (!requiresModels) {
            providerContextStore.setSelectedModel(providerType, null);
        }

        if (modelComboBox != null) {
            modelComboBox.setEditable(true);
            if (requiresModels) {
                modelComboBox.getItems().setAll(currentModels);
                modelComboBox.setDisable(false);
                Optional<String> selected = context.selectedModel();
                if (selected.isPresent()) {
                    String selectedModel = selected.get();
                    if (!modelComboBox.getItems().contains(selectedModel)) {
                        modelComboBox.getItems().add(selectedModel);
                    }
                    modelComboBox.getSelectionModel().select(selectedModel);
                    modelComboBox.getEditor().setText(selectedModel);
                } else {
                    modelComboBox.getSelectionModel().clearSelection();
                    modelComboBox.getEditor().clear();
                }
            } else {
                modelComboBox.getItems().clear();
                modelComboBox.getSelectionModel().clearSelection();
                modelComboBox.getEditor().clear();
                modelComboBox.setDisable(true);
            }
        }

        boolean hasKey = providerContextStore.hasApiKey(providerType);
        updateStatus(hasKey ? providerDisplayName(providerType) + "のAPIキーが設定されています。"
                : providerDisplayName(providerType) + "のAPIキーは未設定です。");
    }

    /**
     * 現在選択中のプロバイダを返します。
     *
     * @return プロバイダ種別
     */
    private ProviderType getCurrentProviderType() {
        return currentProviderType == null ? providerContextStore.getActiveProviderType() : currentProviderType;
    }

    /**
     * プロバイダの表示名を取得します。
     *
     * @param providerType プロバイダ種別
     * @return 表示名
     */
    private String providerDisplayName(ProviderType providerType) {
        return strategyRegistry.findProvider(providerType)
                .map(CharacterGenerationProvider::getDisplayName)
                .orElse(providerType.getDisplayName());
    }

    /**
     * 設定画面を閉じます。
     */
    private void closeStage() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * ステータス表示を更新します。
     *
     * @param message メッセージ
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private String extractModelInput() {
        if (modelComboBox == null) {
            return null;
        }
        String value = modelComboBox.getValue();
        if (value == null || value.isBlank()) {
            value = modelComboBox.isEditable() ? modelComboBox.getEditor().getText() : null;
        }
        if (value != null) {
            value = value.trim();
        }
        return (value == null || value.isEmpty()) ? null : value;
    }
}
