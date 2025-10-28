package com.example.darkchar.domain;

public record AttributeOption(
        Long id,
        AttributeCategory category,
        String name,
        String description) {
}
