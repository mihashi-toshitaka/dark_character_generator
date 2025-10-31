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

    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, Optional<String> warningMessage) {
        this(generatedCharacter, usedProvider, warningMessage, Optional.empty());
    }

    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, String warningMessage) {
        this(generatedCharacter, usedProvider, Optional.ofNullable(warningMessage), Optional.empty());
    }

    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, String warningMessage, String prompt) {
        this(generatedCharacter, usedProvider, Optional.ofNullable(warningMessage), Optional.ofNullable(prompt));
    }
}
