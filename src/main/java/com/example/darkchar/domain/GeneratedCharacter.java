package com.example.darkchar.domain;

import java.time.Instant;

/**
 * 生成済みキャラクターの結果を表現します。
 */
public record GeneratedCharacter(
        CharacterInput characterInput,
        DarknessSelection darknessSelection,
        String narrative,
        Instant generatedAt) {
}
