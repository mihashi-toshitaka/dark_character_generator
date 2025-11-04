package com.example.darkchar.ui.controller;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessPreset;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.ProtagonistAlignment;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.service.AttributeQueryService;
import com.example.darkchar.service.CharacterGenerationService;
import com.example.darkchar.service.GenerationResult;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.ProviderType;
import com.example.darkchar.ui.AppStyleUtil;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.concurrent.Task;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.Cursor;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * メイン画面の操作をまとめるコントローラです。
 */
@Component
public class MainViewController {

    private final AttributeQueryService attributeQueryService;
    private final CharacterGenerationService characterGenerationService;
    private final AiProviderContextStore providerContextStore;

    @FXML
    private RadioButton autoModeButton;

    @FXML
    private RadioButton semiAutoModeButton;

    @FXML
    private ComboBox<WorldGenre> worldGenreComboBox;

    @FXML
    private FlowPane characterTraitContainer;

    @FXML
    private TextArea traitFreeTextArea;

    @FXML
    private VBox darknessCategoryContainer;

    @FXML
    private Slider protagonistBiasSlider;

    @FXML
    private Label protagonistPreviewLabel;

    @FXML
    private Slider darknessSlider;

    @FXML
    private Label darknessValueLabel;

    @FXML
    private Label darknessDescriptionLabel;

    @FXML
    private TextArea freeTextArea;

    @FXML
    private Button generateButton;

    @FXML
    private Button settingsButton;

    private final ToggleGroup modeToggleGroup = new ToggleGroup();
    private final Map<AttributeOption, CheckBox> traitCheckBoxes = new LinkedHashMap<>();
    private final Map<AttributeCategory, List<CheckBox>> darknessCheckBoxes = new EnumMap<>(AttributeCategory.class);
    private static final String DEFAULT_PROTAGONIST_PREVIEW = "例: 選択すると例文を表示します。";
    private final ApplicationContext applicationContext;
    private Stage resultStage;
    private CharacterResultController resultController;
    private Stage settingsStage;
    private SettingsController settingsController;

    /**
     * 画面で利用するサービスを注入します。
     *
     * @param attributeQueryService 属性取得サービス
     * @param characterGenerationService 生成サービス
     * @param providerContextStore プロバイダ設定ストア
     * @param applicationContext Spring アプリケーションコンテキスト
     */
    public MainViewController(AttributeQueryService attributeQueryService,
            CharacterGenerationService characterGenerationService,
            AiProviderContextStore providerContextStore,
            ApplicationContext applicationContext) {
        this.attributeQueryService = attributeQueryService;
        this.characterGenerationService = characterGenerationService;
        this.providerContextStore = providerContextStore;
        this.applicationContext = applicationContext;
    }

    /**
     * 画面初期化時に各コンポーネントをセットアップします。
     */
    @FXML
    public void initialize() {
        autoModeButton.setToggleGroup(modeToggleGroup);
        semiAutoModeButton.setToggleGroup(modeToggleGroup);
        autoModeButton.setSelected(true);
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateMode());

        setupProtagonistBiasSlider();

        setupDarknessSlider();

        setupWorldGenreComboBox();

        populateCharacterTraits();
        populateDarknessOptions();
        updateMode();
    }

    private void setupWorldGenreComboBox() {
        worldGenreComboBox.setItems(FXCollections.observableArrayList(attributeQueryService.loadWorldGenres()));
        worldGenreComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(WorldGenre genre, boolean empty) {
                super.updateItem(genre, empty);
                setText(empty || genre == null ? "" : genre.name());
            }
        });
        worldGenreComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WorldGenre genre, boolean empty) {
                super.updateItem(genre, empty);
                setText(empty || genre == null ? "" : genre.name());
            }
        });
        worldGenreComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WorldGenre object) {
                return object == null ? "" : object.name();
            }

            @Override
            public WorldGenre fromString(String string) {
                return worldGenreComboBox.getItems().stream()
                        .filter(genre -> genre.name().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        if (!worldGenreComboBox.getItems().isEmpty()) {
            worldGenreComboBox.getSelectionModel().selectFirst();
        } else {
            worldGenreComboBox.getSelectionModel().clearSelection();
        }
    }

    private void setupProtagonistBiasSlider() {
        protagonistBiasSlider.setMin(1);
        protagonistBiasSlider.setMax(5);
        protagonistBiasSlider.setMajorTickUnit(1);
        protagonistBiasSlider.setMinorTickCount(0);
        protagonistBiasSlider.setSnapToTicks(true);
        protagonistBiasSlider.setShowTickMarks(true);
        protagonistBiasSlider.setShowTickLabels(true);
        protagonistBiasSlider.setBlockIncrement(1);
        protagonistBiasSlider.setValue(3);

        updateProtagonistPreview(3);

        protagonistBiasSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            int rounded = clampProtagonistBias(newValue.doubleValue());
            if (Math.abs(newValue.doubleValue() - rounded) > 0.0001) {
                protagonistBiasSlider.setValue(rounded);
                return;
            }
            updateProtagonistPreview(rounded);
        });
    }

    private int getSelectedProtagonistBias() {
        return clampProtagonistBias(protagonistBiasSlider.getValue());
    }

    private int clampProtagonistBias(double value) {
        return (int) Math.max(1, Math.min(5, Math.round(value)));
    }

    private void updateProtagonistPreview(int value) {
        String preview = ProtagonistAlignment.fromScore(value)
                .map(ProtagonistAlignment::getPreviewText)
                .orElse(DEFAULT_PROTAGONIST_PREVIEW);
        protagonistPreviewLabel.setText(preview);
    }

    /**
     * キャラクター属性のチェックボックスを構築します。
     */
    private void populateCharacterTraits() {
        characterTraitContainer.getChildren().clear();
        List<AttributeOption> traits = attributeQueryService.loadCharacterTraits();
        for (AttributeOption trait : traits) {
            CheckBox checkBox = new CheckBox(trait.name());
            checkBox.setUserData(trait);
            checkBox.setTooltip(new Tooltip(trait.description()));
            traitCheckBoxes.put(trait, checkBox);
            characterTraitContainer.getChildren().add(checkBox);
        }
    }

    /**
     * 闇堕ちカテゴリのチェックボックスを構築します。
     */
    private void populateDarknessOptions() {
        darknessCategoryContainer.getChildren().clear();
        darknessCheckBoxes.clear();
        Map<AttributeCategory, List<AttributeOption>> options = attributeQueryService.loadDarknessOptions();
        for (Map.Entry<AttributeCategory, List<AttributeOption>> entry : options.entrySet()) {
            AttributeCategory category = entry.getKey();
            if (category == AttributeCategory.CHARACTER_TRAIT) {
                continue;
            }
            VBox box = new VBox(4);
            List<CheckBox> categoryBoxes = new ArrayList<>();
            for (AttributeOption option : entry.getValue()) {
                CheckBox checkBox = new CheckBox(option.name());
                checkBox.setUserData(option);
                checkBox.setTooltip(new Tooltip(option.description()));
                box.getChildren().add(checkBox);
                categoryBoxes.add(checkBox);
            }
            TitledPane pane = new TitledPane(category.getDisplayName(), box);
            pane.setCollapsible(false);
            darknessCategoryContainer.getChildren().add(pane);
            darknessCheckBoxes.put(category, categoryBoxes);
        }
    }

    /**
     * 選択モードに応じて入力可能項目を切り替えます。
     */
    private void updateMode() {
        boolean semiAuto = semiAutoModeButton.isSelected();
        characterTraitContainer.setDisable(!semiAuto);
        traitFreeTextArea.setDisable(!semiAuto);
        traitCheckBoxes.values().forEach(checkBox -> {
            checkBox.setDisable(!semiAuto);
            if (!semiAuto) {
                checkBox.setSelected(false);
            }
        });
        if (!semiAuto) {
            traitFreeTextArea.clear();
        }
    }

    /**
     * 生成ボタン押下時の処理を行います。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleGenerate(ActionEvent event) {
        try {
            InputMode mode = autoModeButton.isSelected() ? InputMode.AUTO : InputMode.SEMI_AUTO;
            WorldGenre worldGenre = worldGenreComboBox.getValue();
            List<AttributeOption> selectedTraits = traitCheckBoxes.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            Map<AttributeCategory, List<AttributeOption>> darknessSelections = darknessCheckBoxes.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .filter(CheckBox::isSelected)
                                    .map(checkBox -> (AttributeOption) checkBox.getUserData())
                                    .collect(Collectors.toList()),
                            (a, b) -> a,
                            () -> new EnumMap<>(AttributeCategory.class)));

            CharacterInput input = new CharacterInput(
                    mode,
                    worldGenre,
                    selectedTraits,
                    traitFreeTextArea.getText(),
                    getSelectedProtagonistBias(),
                    freeTextArea.getText());

            DarknessSelection selection = new DarknessSelection(
                    darknessSelections,
                    getDarknessPreset(darknessSlider.getValue()));

            runGenerationTask(input, selection);
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, ex.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "生成準備中にエラーが発生しました: " + ex.getMessage());
        }
    }

    /**
     * 結果表示ウィンドウを表示します。
     *
     * @param generatedCharacter 生成キャラクター
     * @param prompt 使用したプロンプト
     */
    private void showResultWindow(GeneratedCharacter generatedCharacter, Optional<String> prompt) {
        try {
            if (resultStage == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/darkchar/ui/character-result-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                resultController = loader.getController();

                Scene ownerScene = generateButton.getScene();
                Scene scene = new Scene(root);
                scene.getStylesheets().addAll(ownerScene.getStylesheets());
                String ownerStyle = ownerScene.getRoot().getStyle();
                if (ownerStyle != null && !ownerStyle.isBlank()) {
                    scene.getRoot().setStyle(ownerStyle);
                }

                resultStage = new Stage();
                resultStage.initOwner(ownerScene.getWindow());
                resultStage.initModality(Modality.WINDOW_MODAL);
                resultStage.setScene(scene);
                resultStage.setOnHidden(event -> {
                    resultStage = null;
                    resultController = null;
                });
                resultController.setStage(resultStage);
            }

            if (resultController != null) {
                resultController.setResult(generatedCharacter, prompt);
            }

            if (resultStage != null && !resultStage.isShowing()) {
                resultStage.show();
            } else if (resultStage != null) {
                resultStage.toFront();
                resultStage.requestFocus();
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "結果画面の表示中にエラーが発生しました: " + ex.getMessage());
        }
    }

    /**
     * 非同期でキャラクター生成処理を実行します。
     *
     * @param input 入力情報
     * @param selection 闇堕ち選択
     */
    private void runGenerationTask(CharacterInput input, DarknessSelection selection) {
        ProviderType providerType = providerContextStore.getActiveProviderType();
        Task<GenerationResult> task = new Task<>() {
            @Override
            protected GenerationResult call() {
                return characterGenerationService.generate(input, selection, providerType);
            }
        };

        task.setOnSucceeded(event -> {
            generateButton.setDisable(false);
            setSceneCursor(Cursor.DEFAULT);
            GenerationResult result = task.getValue();
            if (result != null) {
                result.warningMessage().ifPresent(message -> showAlert(Alert.AlertType.WARNING, message));
                showResultWindow(result.generatedCharacter(), result.prompt());
            }
        });

        task.setOnFailed(event -> {
            generateButton.setDisable(false);
            setSceneCursor(Cursor.DEFAULT);
            Throwable ex = task.getException();
            if (ex instanceof IllegalArgumentException iae) {
                showAlert(Alert.AlertType.WARNING, iae.getMessage());
            } else {
                String message = ex == null ? "不明なエラーが発生しました。" : ex.getMessage();
                showAlert(Alert.AlertType.ERROR, "生成中にエラーが発生しました: " + message);
            }
        });

        task.setOnCancelled(event -> {
            generateButton.setDisable(false);
            setSceneCursor(Cursor.DEFAULT);
        });

        generateButton.setDisable(true);
        setSceneCursor(Cursor.WAIT);

        Thread thread = new Thread(task, "character-generation-task");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * シーン全体のカーソルを変更します。
     *
     * @param cursor 設定するカーソル
     */
    private void setSceneCursor(Cursor cursor) {
        if (generateButton != null && generateButton.getScene() != null) {
            generateButton.getScene().setCursor(cursor);
        }
    }

    private void setupDarknessSlider() {
        DarknessPreset defaultPreset = DarknessPreset.getDefault();
        darknessSlider.setMin(DarknessPreset.minValue());
        darknessSlider.setMax(DarknessPreset.maxValue());
        darknessSlider.setMajorTickUnit(50);
        darknessSlider.setMinorTickCount(0);
        darknessSlider.setSnapToTicks(true);
        darknessSlider.setBlockIncrement(50);
        darknessSlider.setValue(defaultPreset.getValue());
        updateDarknessLabels(defaultPreset);

        darknessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            DarknessPreset preset = getDarknessPreset(newVal.doubleValue());
            if (Math.abs(newVal.doubleValue() - preset.getValue()) > 0.0001) {
                darknessSlider.setValue(preset.getValue());
                return;
            }
            updateDarknessLabels(preset);
        });
    }

    private void updateDarknessLabels(DarknessPreset preset) {
        darknessValueLabel.setText(preset.formatValueWithLabel());
        if (darknessDescriptionLabel != null) {
            darknessDescriptionLabel.setText(preset.getDescription());
        }
    }

    private DarknessPreset getDarknessPreset(double value) {
        return DarknessPreset.closestTo(value);
    }

    /**
     * 設定ボタン押下時に設定ダイアログを表示します。
     *
     * @param event 発生したイベント
     */
    @FXML
    void handleOpenSettings(ActionEvent event) {
        try {
            if (settingsStage == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/darkchar/ui/settings-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                settingsController = loader.getController();

                Scene ownerScene = settingsButton.getScene();
                Scene scene = new Scene(root);
                if (ownerScene != null) {
                    scene.getStylesheets().addAll(ownerScene.getStylesheets());
                    String ownerStyle = ownerScene.getRoot().getStyle();
                    if (ownerStyle != null && !ownerStyle.isBlank()) {
                        scene.getRoot().setStyle(ownerStyle);
                    }
                }

                settingsStage = new Stage();
                if (ownerScene != null) {
                    settingsStage.initOwner(ownerScene.getWindow());
                }
                settingsStage.initModality(Modality.WINDOW_MODAL);
                settingsStage.setResizable(false);
                settingsStage.setScene(scene);
                settingsStage.setOnHidden(e -> {
                    settingsStage = null;
                    settingsController = null;
                });
                if (settingsController != null) {
                    settingsController.setStage(settingsStage);
                }
            }

            if (settingsController != null) {
                settingsController.refreshFromStore();
            }

            if (settingsStage != null) {
                if (!settingsStage.isShowing()) {
                    settingsStage.show();
                } else {
                    settingsStage.toFront();
                    settingsStage.requestFocus();
                }
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "設定画面の表示中にエラーが発生しました: " + ex.getMessage());
        }
    }

    /**
     * 共通スタイルを適用したアラートを表示します。
     *
     * @param type    アラート種別
     * @param message 表示メッセージ
     */
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        // オーナーを設定してダイアログ表示を安定させる
        try {
            if (generateButton != null && generateButton.getScene() != null) {
                Stage owner = (Stage) generateButton.getScene().getWindow();
                alert.initOwner(owner);
                alert.initModality(Modality.WINDOW_MODAL);
            }
        } catch (Exception e) {
            // 何か取れなければ無視して続行
        }

        // アプリ共通の stylesheet / font-family を適用
        AppStyleUtil.applyToAlert(alert);
        // OSによっては文字化けするのでalert.setTitleは使わない
        // alert.setTitle("メッセージ");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
