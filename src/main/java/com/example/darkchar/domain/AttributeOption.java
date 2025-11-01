package com.example.darkchar.domain;

/**
 * キャラクター属性の選択肢を保持します。
 */
public record AttributeOption(
        Long id,
        AttributeCategory category,
        String name,
        String description) {
}
