package com.example.darkchar.service.openai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderType;
import com.example.darkchar.service.ai.ProviderGenerationResult;

/**
 * OpenAI ベースのキャラクター生成を担当するプロバイダです。
 */
@Component
public class OpenAiCharacterGenerationProvider implements CharacterGenerationProvider {

    private final OpenAiCharacterGenerationClient generationClient;
    private final OpenAiModelCatalogClient modelCatalogClient;

    /**
     * OpenAI 関連コンポーネントを注入します。
     *
     * @param generationClient SDK クライアント
     * @param modelCatalogClient モデル一覧クライアント
     */
    public OpenAiCharacterGenerationProvider(OpenAiCharacterGenerationClient generationClient,
            OpenAiModelCatalogClient modelCatalogClient) {
        this.generationClient = generationClient;
        this.modelCatalogClient = modelCatalogClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return ProviderType.OPENAI.getDisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderConfigurationStatus assessConfiguration(AiProviderContext context) {
        if (context.apiKey().isEmpty()) {
            return ProviderConfigurationStatus.notReady();
        }
        if (context.selectedModel().isEmpty()) {
            return ProviderConfigurationStatus
                    .notReadyWithWarning("OpenAIモデルが選択されていないため、サンプル結果を表示しています。");
        }
        return ProviderConfigurationStatus.onReady();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderGenerationResult generate(AiProviderContext context, CharacterInput input, DarknessSelection selection) {
        String apiKey = context.apiKey()
                .orElseThrow(() -> new OpenAiIntegrationException("OpenAI APIキーが設定されていません。"));
        String modelId = context.selectedModel()
                .orElseThrow(() -> new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。"));
        return generationClient.generate(apiKey, modelId, input, selection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsModelListing() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listAvailableModels(String apiKey) {
        return modelCatalogClient.listModels(apiKey);
    }
}
