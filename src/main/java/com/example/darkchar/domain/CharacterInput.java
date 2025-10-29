package com.example.darkchar.domain;

import java.util.List;

public record CharacterInput(
        InputMode mode,
        WorldGenre worldGenre,
        List<AttributeOption> characterTraits,
        String traitFreeText,
        int protagonistScore,
        String darknessFreeText) {
}
