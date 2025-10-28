package com.example.darkchar.domain;

import java.util.List;

public record CharacterInput(
        InputMode mode,
        WorldGenre worldGenre,
        List<AttributeOption> characterTraits,
        int protagonistScore,
        String freeText) {
}
