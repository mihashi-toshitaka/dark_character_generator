package com.example.darkchar.domain;

import java.time.Instant;

public record GeneratedCharacter(
        CharacterInput characterInput,
        DarknessSelection darknessSelection,
        String narrative,
        Instant generatedAt) {
}
