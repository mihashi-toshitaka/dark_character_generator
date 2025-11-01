package com.example.darkchar.domain;

/**
 * 属性カテゴリを表す列挙です。
 */
public enum AttributeCategory {
    CHARACTER_TRAIT("キャラクター属性"),
    MOTIVE("動機・欲求"),
    TRANSFORMATION_PROCESS("変質プロセス"),
    MINDSET("性向の変質"),
    APPEARANCE("外見・象徴表現");

    private final String displayName;

    /**
     * 表示名を指定して列挙を初期化します。
     *
     * @param displayName 表示名
     */
    AttributeCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 表示名を返します。
     *
     * @return 日本語表示名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * コードからカテゴリを取得します。
     *
     * @param code 列挙名
     * @return 対応するカテゴリ
     * @throws IllegalArgumentException 未知のコードの場合
     */
    public static AttributeCategory fromCode(String code) {
        for (AttributeCategory category : values()) {
            if (category.name().equalsIgnoreCase(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category code: " + code);
    }
}
