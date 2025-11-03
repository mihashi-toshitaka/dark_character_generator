package com.example.darkchar.ui.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.GenerationModelCatalog;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;

class SettingsControllerTest {

    @Test
    @DisplayName("APIキー未入力で保存すると設定がクリアされる")
    void handleSaveClearsSettingsWhenApiKeyBlank() throws Exception {
        AiProviderContextStore store = new AiProviderContextStore();
        store.setApiKey(ProviderType.OPENAI, "existing-key");
        store.setSelectedModel(ProviderType.OPENAI, "existing-model");

        SettingsController controller = new SettingsController(store, createRegistry(), new GenerationModelCatalog());

        PasswordField apiKeyField = mock(PasswordField.class);
        when(apiKeyField.getText()).thenReturn("   ");

        ComboBoxFixture fixture = comboBoxFixture("existing-model");
        Label statusLabel = mock(Label.class);

        setField(controller, "apiKeyField", apiKeyField);
        setField(controller, "modelComboBox", fixture.comboBox());
        setField(controller, "statusLabel", statusLabel);
        setField(controller, "currentProviderType", ProviderType.OPENAI);

        controller.handleSave(null);

        assertThat(store.hasApiKey(ProviderType.OPENAI)).isFalse();
        assertThat(store.getSelectedModel(ProviderType.OPENAI)).isEmpty();
        assertThat(fixture.items()).isEmpty();

        verify(fixture.comboBox()).setDisable(true);
        verify(fixture.selectionModel()).clearSelection();
        verify(fixture.editor()).clear();
        verify(statusLabel).setText("OpenAIの設定をクリアしました。");
    }

    @Test
    @DisplayName("モデル必須のプロバイダでモデルとAPIキーを保存する")
    void handleSavePersistsModelAndKey() throws Exception {
        AiProviderContextStore store = new AiProviderContextStore();
        SettingsController controller = new SettingsController(store, createRegistry(), new GenerationModelCatalog());

        PasswordField apiKeyField = mock(PasswordField.class);
        when(apiKeyField.getText()).thenReturn("  test-key  ");

        ComboBoxFixture fixture = comboBoxFixture("gpt-3.5-turbo");
        when(fixture.comboBox().getValue()).thenReturn("  gpt-4-turbo  ");
        when(fixture.editor().getText()).thenReturn("  gpt-4-turbo  ");

        Label statusLabel = mock(Label.class);

        setField(controller, "apiKeyField", apiKeyField);
        setField(controller, "modelComboBox", fixture.comboBox());
        setField(controller, "statusLabel", statusLabel);
        setField(controller, "currentProviderType", ProviderType.OPENAI);

        controller.handleSave(null);

        assertThat(store.getApiKey(ProviderType.OPENAI)).hasValue("test-key");
        assertThat(store.getSelectedModel(ProviderType.OPENAI)).hasValue("gpt-4-turbo");
        assertThat(store.getActiveProviderType()).isEqualTo(ProviderType.OPENAI);
        assertThat(fixture.items()).contains("gpt-4-turbo");

        verify(statusLabel).setText("OpenAIの設定を保存しました。");
    }

    @Test
    @DisplayName("モデル未選択の場合は保存せず警告を表示する")
    void handleSaveRequiresModelSelectionWhenMissing() throws Exception {
        AiProviderContextStore store = new AiProviderContextStore();
        SettingsController controller = new SettingsController(store, createRegistry(), new GenerationModelCatalog());

        PasswordField apiKeyField = mock(PasswordField.class);
        when(apiKeyField.getText()).thenReturn("  test-key  ");

        ComboBoxFixture fixture = comboBoxFixture("gpt-4o");
        when(fixture.comboBox().getValue()).thenReturn(null);
        when(fixture.editor().getText()).thenReturn("   ");

        Label statusLabel = mock(Label.class);

        setField(controller, "apiKeyField", apiKeyField);
        setField(controller, "modelComboBox", fixture.comboBox());
        setField(controller, "statusLabel", statusLabel);
        setField(controller, "currentProviderType", ProviderType.OPENAI);

        controller.handleSave(null);

        assertThat(store.hasApiKey(ProviderType.OPENAI)).isFalse();
        assertThat(store.getSelectedModel(ProviderType.OPENAI)).isEmpty();
        assertThat(fixture.items()).containsExactly("gpt-4o");

        verify(statusLabel).setText("使用するモデルを選択してください。");
        verifyNoInteractions(fixture.selectionModel());
    }

    private CharacterGenerationStrategyRegistry createRegistry() {
        return new CharacterGenerationStrategyRegistry(List.of(new StubProvider()));
    }

    private ComboBoxFixture comboBoxFixture(String initialItem) {
        ObservableList<String> items = FXCollections.observableArrayList();
        if (initialItem != null) {
            items.add(initialItem);
        }

        ComboBox<String> comboBox = mock(ComboBox.class);
        when(comboBox.getItems()).thenReturn(items);

        @SuppressWarnings("unchecked")
        SingleSelectionModel<String> selectionModel = mock(SingleSelectionModel.class);
        when(comboBox.getSelectionModel()).thenReturn(selectionModel);

        TextField editor = mock(TextField.class);
        when(comboBox.getEditor()).thenReturn(editor);
        when(comboBox.isEditable()).thenReturn(true);

        return new ComboBoxFixture(comboBox, items, selectionModel, editor);
    }

    private void setField(SettingsController controller, String fieldName, Object value) throws Exception {
        Field field = SettingsController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private record ComboBoxFixture(
            ComboBox<String> comboBox,
            ObservableList<String> items,
            SingleSelectionModel<String> selectionModel,
            TextField editor) {
    }

    private static class StubProvider implements CharacterGenerationProvider {

        @Override
        public ProviderType getProviderType() {
            return ProviderType.OPENAI;
        }

        @Override
        public String getDisplayName() {
            return "OpenAI";
        }

        @Override
        public ProviderConfigurationStatus assessConfiguration(com.example.darkchar.service.ai.AiProviderContext context) {
            return ProviderConfigurationStatus.onReady();
        }

        @Override
        public com.example.darkchar.service.ai.ProviderGenerationResult generate(
                com.example.darkchar.service.ai.AiProviderContext context,
                com.example.darkchar.domain.CharacterInput input,
                com.example.darkchar.domain.DarknessSelection selection) {
            throw new UnsupportedOperationException("generate should not be called in tests");
        }
    }
}

