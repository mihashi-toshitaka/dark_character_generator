package com.example.darkchar.domain;

import java.util.List;
import java.util.Map;

/**
 * 闇堕ちカテゴリの選択内容を保持します。
 */
public record DarknessSelection(
        Map<AttributeCategory, List<AttributeOption>> selections,
        int darknessLevel) {
}
