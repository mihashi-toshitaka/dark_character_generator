package com.example.darkchar.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.darkchar.service.openai.OpenAiGenerationModel;

class GenerationModelCatalogTest {

    private final GenerationModelCatalog catalog = new GenerationModelCatalog();

    @Nested
    @DisplayName("listModels")
    class ListModels {

        @Test
        @DisplayName("OpenAI プロバイダのモデル一覧を返す")
        void returnsOpenAiModels() {
            List<String> models = catalog.listModels(ProviderType.OPENAI);

            assertThat(models)
                    .containsExactlyElementsOf(OpenAiGenerationModel.ids());
        }

        @Test
        @DisplayName("返却リストを書き換えてもカタログ内部に影響しない")
        void returnsDefensiveCopy() {
            List<String> models = catalog.listModels(ProviderType.OPENAI);

            models.add("custom");

            List<String> secondCall = catalog.listModels(ProviderType.OPENAI);
            assertThat(secondCall)
                    .containsExactlyElementsOf(OpenAiGenerationModel.ids());
        }

        @Test
        @DisplayName("未対応プロバイダは空のリストを返す")
        void returnsEmptyListForUnsupportedProviders() {
            List<String> models = catalog.listModels(ProviderType.LOCAL);

            assertThat(models).isEmpty();
        }

        @Test
        @DisplayName("null を渡すと例外がスローされる")
        void throwsForNullProvider() {
            assertThatThrownBy(() -> catalog.listModels(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("requiresModelSelection")
    class RequiresModelSelection {

        @Test
        @DisplayName("OpenAI はモデル選択が必須")
        void openAiRequiresSelection() {
            assertThat(catalog.requiresModelSelection(ProviderType.OPENAI)).isTrue();
        }

        @Test
        @DisplayName("ローカルはモデル選択不要")
        void localDoesNotRequireSelection() {
            assertThat(catalog.requiresModelSelection(ProviderType.LOCAL)).isFalse();
        }
    }
}
