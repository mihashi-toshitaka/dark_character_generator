package com.example.darkchar.service.ai;

import java.util.List;
import java.util.Optional;

/**
 * プロバイダ別の設定情報を読み取り専用で表現する値オブジェクト。
 */
public record AiProviderContext(
        ProviderType providerType,
        Optional<String> apiKey,
        Optional<String> selectedModel,
        List<String> availableModels) {

    /**
     * 入力値を検証し正規化します。
     */
    public AiProviderContext {
        if (providerType == null) {
            throw new IllegalArgumentException("providerType must not be null");
        }
        apiKey = apiKey == null ? Optional.empty() : apiKey;
        selectedModel = selectedModel == null ? Optional.empty() : selectedModel;
        availableModels = availableModels == null ? List.of() : List.copyOf(availableModels);
    }
}
