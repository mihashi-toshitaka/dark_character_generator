package com.example.darkchar.domain;

import java.util.List;

/**
 * キャラクター生成に必要な入力情報をまとめます。
 */
public record CharacterInput(
        InputMode mode,
        WorldGenre worldGenre,
        List<AttributeOption> characterTraits,
        String traitFreeText,
        int protagonistScore,
        String darknessFreeText) {
}
