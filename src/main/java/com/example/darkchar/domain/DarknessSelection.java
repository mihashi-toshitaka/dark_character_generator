package com.example.darkchar.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 闇堕ちカテゴリの選択内容とプリセット情報を保持します。
 */
public record DarknessSelection(
        Map<AttributeCategory, List<AttributeOption>> selections,
        DarknessPreset preset) {

    public DarknessSelection {
        Objects.requireNonNull(selections, "selections");
        preset = DarknessPreset.requireNonNull(preset);
    }

    /**
     * 選択した闇堕ち度（パーセント値）を返します。
     *
     * @return 闇堕ち度
     */
    public int darknessLevel() {
        return preset.getValue();
    }

    /**
     * プリセットラベルを返します。
     *
     * @return ラベル
     */
    public String darknessLabel() {
        return preset.getLabel();
    }

    /**
     * プリセット説明文を返します。
     *
     * @return 説明文
     */
    public String darknessDescription() {
        return preset.getDescription();
    }
}
