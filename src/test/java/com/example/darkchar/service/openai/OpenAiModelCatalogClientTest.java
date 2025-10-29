package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiModelCatalogClientTest {

    private final OpenAiModelCatalogClient client = new OpenAiModelCatalogClient(RestClient.builder());

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

    @Test
    void filterAllowedModelsRemovesDateSuffixedEntries() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o-2024-05-13",
                "gpt-4o-mini-2024-05-13",
                "o1-preview-2024-06-18",
                "o3-mini"));

        assertThat(filtered).containsExactly("o3-mini");
    }

    @Test
    void filterAllowedModelsRemovesShortDateSuffixEntries() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o-0601",
                "o1-mini-1231",
                "gpt-4o"));

        assertThat(filtered).containsExactly("gpt-4o");
    }

    @Test
    void filterAllowedModelsRemovesExcludedKeywords() {
        List<String> filtered = client.filterAllowedModels(List.of(
                "gpt-4o-vision-preview",
                "gpt-4o-audio",
                "o1-realtime",
                "gpt-4o-mini"));

        assertThat(filtered).containsExactly("gpt-4o-mini");
    }
}
