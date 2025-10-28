package com.example.darkchar.domain;

public enum AttributeCategory {
    CHARACTER_TRAIT("キャラクター属性"),
    MOTIVE("動機・欲求"),
    TRANSFORMATION_PROCESS("変質プロセス"),
    MINDSET("性向の変質"),
    APPEARANCE("外見・象徴表現");

    private final String displayName;

    AttributeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttributeCategory fromCode(String code) {
        for (AttributeCategory category : values()) {
            if (category.name().equalsIgnoreCase(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category code: " + code);
    }
}
