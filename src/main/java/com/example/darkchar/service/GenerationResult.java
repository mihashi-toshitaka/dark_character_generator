package com.example.darkchar.service;

import java.util.Optional;

import com.example.darkchar.domain.GeneratedCharacter;

/**
 * キャラクター生成結果と付随情報を保持するレコード。
 */
public record GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, Optional<String> warningMessage) {

    public GenerationResult {
        if (generatedCharacter == null) {
            throw new IllegalArgumentException("generatedCharacter must not be null");
        }
        warningMessage = warningMessage == null ? Optional.empty() : warningMessage;
    }

    public GenerationResult(GeneratedCharacter generatedCharacter, boolean usedProvider, String warningMessage) {
        this(generatedCharacter, usedProvider, Optional.ofNullable(warningMessage));
    }
}
