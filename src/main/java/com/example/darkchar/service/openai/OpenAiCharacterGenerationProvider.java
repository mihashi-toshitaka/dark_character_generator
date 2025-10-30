package com.example.darkchar.service.openai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderType;

@Component
public class OpenAiCharacterGenerationProvider implements CharacterGenerationProvider {

    private final OpenAiCharacterGenerationClient generationClient;
    private final OpenAiModelCatalogClient modelCatalogClient;

    public OpenAiCharacterGenerationProvider(OpenAiCharacterGenerationClient generationClient,
            OpenAiModelCatalogClient modelCatalogClient) {
        this.generationClient = generationClient;
        this.modelCatalogClient = modelCatalogClient;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI;
    }

    @Override
    public String getDisplayName() {
        return ProviderType.OPENAI.getDisplayName();
    }

    @Override
    public ProviderConfigurationStatus assessConfiguration(AiProviderContext context) {
        if (context.apiKey().isEmpty()) {
            return ProviderConfigurationStatus.notReady();
        }
        if (context.selectedModel().isEmpty()) {
            return ProviderConfigurationStatus
                    .notReadyWithWarning("OpenAIモデルが選択されていないため、サンプル結果を表示しています。");
        }
        return ProviderConfigurationStatus.ready();
    }

    @Override
    public String generateNarrative(AiProviderContext context, CharacterInput input, DarknessSelection selection) {
        String apiKey = context.apiKey()
                .orElseThrow(() -> new OpenAiIntegrationException("OpenAI APIキーが設定されていません。"));
        String modelId = context.selectedModel()
                .orElseThrow(() -> new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。"));
        return generationClient.generateNarrative(apiKey, modelId, input, selection);
    }

    @Override
    public boolean supportsModelListing() {
        return true;
    }

    @Override
    public List<String> listAvailableModels(String apiKey) {
        return modelCatalogClient.listModels(apiKey);
    }
}
