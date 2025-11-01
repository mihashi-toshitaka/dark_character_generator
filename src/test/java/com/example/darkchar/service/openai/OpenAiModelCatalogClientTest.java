package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link OpenAiModelCatalogClient} のフィルタリングを検証します。
 */
class OpenAiModelCatalogClientTest {

    private final OpenAiModelCatalogClient client = new OpenAiModelCatalogClient(new OpenAiClientFactory());

    /**
     * GPT と O シリーズのみ残すことを確認します。
     */
    @Test
    void filterAllowedModelsKeepsAllowedPrefixes() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4.1",
                "gpt-4.1-mini",
                "o1-mini",
                "o12-large",
                "text-davinci-003"));

        assertThat(filtered).containsExactly(
                "gpt-4.1",
                "gpt-4.1-mini",
                "gpt-4o",
                "gpt-4o-mini",
                "o1-mini",
                "o12-large");
    }

    /**
     * null 値が除外されることを確認します。
     */
    @Test
    void filterAllowedModelsIgnoresNullEntries() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o",
                null,
                "gpt-4.1"));

        assertThat(filtered).containsExactly(
                "gpt-4.1",
                "gpt-4o");
    }

    /**
     * 日付サフィックス付きモデルが除外されることを確認します。
     */
    @Test
    void filterAllowedModelsRemovesDateSuffixedEntries() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o-2024-05-13",
                "gpt-4o-mini-2024-05-13",
                "o1-preview-2024-06-18",
                "o3-mini"));

        assertThat(filtered).containsExactly("o3-mini");
    }
}
