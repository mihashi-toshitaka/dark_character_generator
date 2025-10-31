package com.example.darkchar.service.ai;

import java.util.Optional;

/**
 * プロバイダによる生成結果を保持するDTO。
 */
public record ProviderGenerationResult(String narrative, Optional<String> prompt) {

    public ProviderGenerationResult {
        if (narrative == null) {
            throw new IllegalArgumentException("narrative must not be null");
        }
        prompt = prompt == null ? Optional.empty() : prompt;
    }

    public ProviderGenerationResult(String narrative) {
        this(narrative, Optional.empty());
    }

    public ProviderGenerationResult(String narrative, String prompt) {
        this(narrative, Optional.ofNullable(prompt));
    }
}
