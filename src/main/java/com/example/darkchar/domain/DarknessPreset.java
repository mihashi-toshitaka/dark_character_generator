package com.example.darkchar.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 闇堕ち度スライダーのプリセット値を表します。
 */
public enum DarknessPreset {

    MILD(50, "軽度", "闇の誘惑は芽生えたばかりで、まだ自身を取り戻せる余地が残っています。"),
    STANDARD(100, "通常", "闇への傾倒が進み、迷いながらも暗い選択を受け入れ始めています。"),
    HEAVY(150, "重め", "闇が心身を侵食し、かつての価値観が大きく揺らぎ始めています。"),
    RADICAL(200, "過激", "闇が支配的となり、目的のためならば犠牲を厭わない行動を取ります。"),
    EXTREME(250, "極端", "闇と完全に同化し、世界を書き換えるために破滅すら手段とする狂信状態です。");

    private static final Map<Integer, DarknessPreset> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toMap(DarknessPreset::getValue, Function.identity()));

    private final int value;
    private final String label;
    private final String description;

    DarknessPreset(int value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    /**
     * プリセットの値を取得します。
     *
     * @return プリセット値
     */
    public int getValue() {
        return value;
    }

    /**
     * プリセットの短いラベルを取得します。
     *
     * @return ラベル
     */
    public String getLabel() {
        return label;
    }

    /**
     * プリセットの説明文を取得します。
     *
     * @return 説明文
     */
    public String getDescription() {
        return description;
    }

    /**
     * 指定した値に一致するプリセットを取得します。
     *
     * @param value プリセット値
     * @return 対応するプリセット
     */
    public static Optional<DarknessPreset> fromValue(int value) {
        return Optional.ofNullable(BY_VALUE.get(value));
    }

    /**
     * 指定値に最も近いプリセットを取得します。
     *
     * @param value 判定値
     * @return 最も近いプリセット
     */
    public static DarknessPreset closestTo(double value) {
        return Arrays.stream(values())
                .min(Comparator.comparingDouble(preset -> Math.abs(value - preset.value)))
                .orElse(STANDARD);
    }

    /**
     * プリセット値が有効か判定します。
     *
     * @param value 判定値
     * @return 有効なプリセット値か
     */
    public static boolean isValidValue(int value) {
        return BY_VALUE.containsKey(value);
    }

    /**
     * 最小プリセット値を取得します。
     *
     * @return 最小値
     */
    public static int minValue() {
        return Arrays.stream(values())
                .map(DarknessPreset::getValue)
                .min(Integer::compareTo)
                .orElse(STANDARD.value);
    }

    /**
     * 最大プリセット値を取得します。
     *
     * @return 最大値
     */
    public static int maxValue() {
        return Arrays.stream(values())
                .map(DarknessPreset::getValue)
                .max(Integer::compareTo)
                .orElse(STANDARD.value);
    }

    /**
     * デフォルトプリセットを取得します。
     *
     * @return デフォルトプリセット
     */
    public static DarknessPreset getDefault() {
        return STANDARD;
    }

    /**
     * 値とラベルを組み合わせた表示文字列を返します。
     *
     * @return 表示文字列
     */
    public String formatValueWithLabel() {
        return String.format(Locale.JAPAN, "%d%%（%s）", value, label);
    }

    /**
     * プリセットを null 安全に扱います。
     *
     * @param preset プリセット
     * @return 文字列
     */
    public static String safeFormatValueWithLabel(DarknessPreset preset) {
        return preset == null ? "" : preset.formatValueWithLabel();
    }

    /**
     * プリセットが必須であることを検証します。
     *
     * @param preset プリセット
     * @return 検証済みプリセット
     */
    public static DarknessPreset requireNonNull(DarknessPreset preset) {
        return Objects.requireNonNullElse(preset, getDefault());
    }
}
