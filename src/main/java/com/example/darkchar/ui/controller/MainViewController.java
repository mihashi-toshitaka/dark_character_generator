package com.example.darkchar.ui.controller;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.service.AttributeQueryService;
import com.example.darkchar.service.CharacterGenerationService;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@Component
public class MainViewController {

    private final AttributeQueryService attributeQueryService;
    private final CharacterGenerationService characterGenerationService;

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
    private Slider protagonistSlider;

    @FXML
    private Label protagonistValueLabel;

    @FXML
    private Slider darknessSlider;

    @FXML
    private Label darknessValueLabel;

    @FXML
    private TextArea freeTextArea;

    @FXML
    private Button generateButton;

    private final ToggleGroup modeToggleGroup = new ToggleGroup();
    private final Map<AttributeOption, CheckBox> traitCheckBoxes = new LinkedHashMap<>();
    private final Map<AttributeCategory, List<CheckBox>> darknessCheckBoxes = new EnumMap<>(AttributeCategory.class);
    private final ApplicationContext applicationContext;
    private Stage resultStage;
    private CharacterResultController resultController;

    public MainViewController(AttributeQueryService attributeQueryService,
            CharacterGenerationService characterGenerationService,
            ApplicationContext applicationContext) {
        this.attributeQueryService = attributeQueryService;
        this.characterGenerationService = characterGenerationService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        autoModeButton.setToggleGroup(modeToggleGroup);
        semiAutoModeButton.setToggleGroup(modeToggleGroup);
        autoModeButton.setSelected(true);
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateMode());

        protagonistSlider.setMin(1);
        protagonistSlider.setMax(5);
        protagonistSlider.setMajorTickUnit(1);
        protagonistSlider.setMinorTickCount(0);
        protagonistSlider.setSnapToTicks(true);
        protagonistSlider.setValue(3);
        protagonistValueLabel.setText(Integer.toString((int) protagonistSlider.getValue()));
        protagonistSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> protagonistValueLabel.setText(Integer.toString(newVal.intValue())));

        darknessSlider.setMin(1);
        darknessSlider.setMax(5);
        darknessSlider.setMajorTickUnit(1);
        darknessSlider.setMinorTickCount(0);
        darknessSlider.setSnapToTicks(true);
        darknessSlider.setValue(3);
        darknessValueLabel.setText(Integer.toString((int) darknessSlider.getValue()));
        darknessSlider.valueProperty()
                .addListener((obs, oldVal, newVal) -> darknessValueLabel.setText(Integer.toString(newVal.intValue())));

        worldGenreComboBox.setItems(FXCollections.observableArrayList(attributeQueryService.loadWorldGenres()));
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
        }

        populateCharacterTraits();
        populateDarknessOptions();
        updateMode();
    }

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
                    (int) Math.round(protagonistSlider.getValue()),
                    freeTextArea.getText());

            DarknessSelection selection = new DarknessSelection(
                    darknessSelections,
                    (int) Math.round(darknessSlider.getValue()));

            GeneratedCharacter generatedCharacter = characterGenerationService.generate(input, selection);
            showResultWindow(generatedCharacter);
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, ex.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "生成中にエラーが発生しました: " + ex.getMessage());
        }
    }

    private void showResultWindow(GeneratedCharacter generatedCharacter) {
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
                resultController.setGeneratedCharacter(generatedCharacter);
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
