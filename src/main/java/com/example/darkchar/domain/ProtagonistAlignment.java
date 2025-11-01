package com.example.darkchar.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 主人公度のスコアに応じた立ち位置を表現します。
 */
public enum ProtagonistAlignment {

    HEROIC_LEADER(1,
            "例: 正義側の中心人物として物語が始まる。",
            "正義側の中心人物として物語を牽引する英雄的な立場"),
    RELIABLE_ALLY(2,
            "例: 主人公陣営の頼れる仲間として登場する。",
            "主人公陣営を陰で支える頼れる仲間という立場"),
    INDEPENDENT_AGENT(3,
            "例: 利害で動く第三勢力、どちらにも肩入れしない。",
            "利害で動きどちらにも肩入れしない独立勢力の立場"),
    FALLEN_ANTI_HERO(4,
            "例: 敵側に傾いた反英雄として物語に関与する。",
            "敵側に傾き始めた反英雄として揺らぐ立場"),
    VILLAIN_CORE(5,
            "例: 開幕から敵組織の中核メンバーとして暗躍する。",
            "物語開始時点で敵組織の中核として暗躍する立場");

    private final int score;
    private final String previewText;
    private final String promptDescription;

    ProtagonistAlignment(int score, String previewText, String promptDescription) {
        this.score = score;
        this.previewText = previewText;
        this.promptDescription = promptDescription;
    }

    /**
     * スコアに対応する立ち位置を取得します。
     *
     * @param score 主人公度スコア
     * @return 対応する {@link ProtagonistAlignment}
     */
    public static Optional<ProtagonistAlignment> fromScore(int score) {
        return Arrays.stream(values())
                .filter(alignment -> alignment.score == score)
                .findFirst();
    }

    /**
     * UI で利用するプレビュー文を取得します。
     *
     * @return プレビュー文
     */
    public String getPreviewText() {
        return previewText;
    }

    /**
     * プロンプトに埋め込む説明を取得します。
     *
     * @return プロンプト用説明文
     */
    public String getPromptDescription() {
        return promptDescription;
    }

    /**
     * 主人公度スコアを取得します。
     *
     * @return スコア (1-5)
     */
    public int getScore() {
        return score;
    }

    /**
     * プロンプトに挿入する整形済みの行を返します。
     *
     * @return 整形済み文字列
     */
    public String formatPromptLine() {
        String trimmedDescription = promptDescription.trim();
        if (trimmedDescription.isEmpty()) {
            return trimmedDescription;
        }

        char lastChar = trimmedDescription.charAt(trimmedDescription.length() - 1);
        if (lastChar == '。' || lastChar == '！' || lastChar == '？' ||
                lastChar == '.' || lastChar == '!' || lastChar == '?') {
            return trimmedDescription;
        }

        return trimmedDescription + "。";
    }
}
