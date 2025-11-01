package com.example.darkchar.service;

import com.example.darkchar.domain.GeneratedCharacter;

import java.util.Optional;

/**
 * キャラクター生成結果と付随情報を保持するレコード。
 */
public record GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider,
        Optional<String> warningMessage, Optional<String> prompt) {

    public GenerationResult {
        if (generatedCharacter == null) {
            throw new IllegalArgumentException("generatedCharacter must not be null");
        }
        warningMessage = warningMessage == null ? Optional.empty() : warningMessage;
        prompt = prompt == null ? Optional.empty() : prompt;
    }

    /**
     * プロンプトなしで結果を生成します。
     *
     * @param generatedCharacter 生成キャラクター
     * @param usedProvider       プロバイダ利用有無
     * @param warningMessage     警告メッセージ
     */
    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, Optional<String> warningMessage) {
        this(generatedCharacter, usedProvider, warningMessage, Optional.empty());
    }

    /**
     * 文字列警告のみ指定して結果を生成します。
     *
     * @param generatedCharacter 生成キャラクター
     * @param usedProvider       プロバイダ利用有無
     * @param warningMessage     警告メッセージ
     */
    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, String warningMessage) {
        this(generatedCharacter, usedProvider, Optional.ofNullable(warningMessage), Optional.empty());
    }

    /**
     * 警告とプロンプトを指定して結果を生成します。
     *
     * @param generatedCharacter 生成キャラクター
     * @param usedProvider       プロバイダ利用有無
     * @param warningMessage     警告メッセージ
     * @param prompt             使用したプロンプト
     */
    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, String warningMessage, String prompt) {
        this(generatedCharacter, usedProvider, Optional.ofNullable(warningMessage), Optional.ofNullable(prompt));
    }
}
