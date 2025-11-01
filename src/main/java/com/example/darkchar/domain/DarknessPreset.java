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

    MILD(50, "軽度", "闇の力に傾倒し、それをかなり受け入れているが、かつての情や未練がわずかに残る。状況と相手次第では説得に耳を傾けることもあるが、闇への決意は容易には揺らがない。"),
    STANDARD(100, "通常", "闇に完全支配され、価値観も忠誠も暗黒側に固定。自分の行動理念を達成するためには手段を選ばない。正義側から見れば完全敵対化であり、理詰めの説得も情の訴えも通用しない。"),
    HEAVY(150, "重め", "単なる闇堕ちを越え、深く闇そのものへと同化した段階。闇堕ち前の自我の多くは剥落し、力の行使それ自体が行動目的へとなり替わってきている。"),
    RADICAL(200, "過激", "世界を闇に塗りつぶすために動く存在となった段階。禁忌や人道は抑止力を失い、反対勢力は体系的に排除され、都市・国家規模で世界が闇に堕ち始める。"),
    EXTREME(250, "極端", "絶対的な闇の権化。存在そのものが破局の引き金となり、正義は壊滅、世界は取り返しのつかない崩壊へ傾く——物語は完全なバッドエンドに収束する。");

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
