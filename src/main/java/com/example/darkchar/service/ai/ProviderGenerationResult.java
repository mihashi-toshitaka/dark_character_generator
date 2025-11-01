package com.example.darkchar.service.ai;

import java.util.Optional;

/**
 * プロバイダによる生成結果を保持するDTO。
 */
public record ProviderGenerationResult(String narrative, Optional<String> prompt) {

    /**
     * 値を検証して正規化します。
     */
    public ProviderGenerationResult {
        if (narrative == null) {
            throw new IllegalArgumentException("narrative must not be null");
        }
        prompt = prompt == null ? Optional.empty() : prompt;
    }

    /**
     * プロンプトなしの結果を生成します。
     *
     * @param narrative 生成テキスト
     */
    public ProviderGenerationResult(String narrative) {
        this(narrative, Optional.empty());
    }

    /**
     * 文字列プロンプト付きで結果を生成します。
     *
     * @param narrative 生成テキスト
     * @param prompt    使用プロンプト
     */
    public ProviderGenerationResult(String narrative, String prompt) {
        this(narrative, Optional.ofNullable(prompt));
    }
}
