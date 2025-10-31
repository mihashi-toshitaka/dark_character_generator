package com.example.darkchar.service.ai;

import java.util.List;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;

/**
 * 各種AIプロバイダが実装するキャラクター生成用インターフェース。
 */
public interface CharacterGenerationProvider {

    ProviderType getProviderType();

    String getDisplayName();

    ProviderConfigurationStatus assessConfiguration(AiProviderContext context);

    ProviderGenerationResult generate(AiProviderContext context, CharacterInput input, DarknessSelection selection);

    default boolean supportsModelListing() {
        return false;
    }

    default List<String> listAvailableModels(String apiKey) {
        return List.of();
    }

    default String buildFailureWarning(Throwable throwable) {
        String detail = throwable == null ? null : throwable.getMessage();
        String base = getDisplayName() + "連携に失敗したため、サンプル結果を表示しています。";
        if (detail == null || detail.isBlank()) {
            return base;
        }
        return base + "詳細: " + detail;
    }
}
