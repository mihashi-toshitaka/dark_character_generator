package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessPreset;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;

/**
 * {@link PromptTemplateRenderer} が期待通りにテンプレートを展開することを検証します。
 */
class PromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer(new DefaultResourceLoader());

    /**
     * 任意セクションがすべて出力されるケースを確認します。
     */
    @Test
    void renderIncludesOptionalSections() {
        CharacterInput input = new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(1L, "ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "堕ちた騎士", "名誉を失った騎士")),
                "秘密の弱み",
                4,
                "影に魅入られた");
        DarknessSelection selection = new DarknessSelection(
                Map.of(AttributeCategory.MINDSET,
                        List.of(new AttributeOption(2L, AttributeCategory.MINDSET, "復讐心", "復讐に燃える"))),
                DarknessPreset.MILD);

        String actual = renderer.render(input, selection);

        String expected = """
                あなたは闇堕ちキャラクターの設定と短いストーリーを作成する日本語のライターです。
                以下の入力情報に基づき、世界観の説明、キャラクターの転落のきっかけ、闇堕ち後の姿を含む物語を400〜600文字程度で作成してください。
                最後に箇条書きは使わず、段落構成でまとめてください。

                [世界観ジャンル]
                ダークファンタジー

                [モード]
                セミオート

                [キャラクター属性]
                ・堕ちた騎士: 名誉を失った騎士

                [キャラクター属性メモ]
                秘密の弱み

                [闇堕ち前の立ち位置]
                敵側に傾き始めた反英雄として揺らぐ立場。

                [闇堕ちカテゴリと選択肢]
                性向の変質: 復讐心

                [闇堕ち度(100%で標準の闇堕ち状態)]
                50%（軽度）: 闇の誘惑は芽生えたばかりで、まだ自身を取り戻せる余地が残っています。

                [闇堕ちメモ]
                影に魅入られた

                条件:
                1. キャラクターが闇堕ちに至った心理的な揺らぎや事件を描写する。
                2. 闇堕ち後のビジュアルや能力、価値観の変化を盛り込む。
                3. 最後は読者が次の展開を想像できる余韻を残す。
                4. 出力は日本語で行う。
                """;

        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 任意セクションが空の場合に省略されることを確認します。
     */
    @Test
    void renderOmitsOptionalSectionsWhenAbsent() {
        CharacterInput input = new CharacterInput(
                InputMode.AUTO,
                new WorldGenre(2L, "サイバーパンク"),
                List.of(),
                null,
                2,
                null);
        DarknessSelection selection = new DarknessSelection(Map.of(), DarknessPreset.STANDARD);

        String actual = renderer.render(input, selection);

        String expected = """
                あなたは闇堕ちキャラクターの設定と短いストーリーを作成する日本語のライターです。
                以下の入力情報に基づき、世界観の説明、キャラクターの転落のきっかけ、闇堕ち後の姿を含む物語を400〜600文字程度で作成してください。
                最後に箇条書きは使わず、段落構成でまとめてください。

                [世界観ジャンル]
                サイバーパンク

                [モード]
                オート

                [闇堕ち前の立ち位置]
                主人公陣営を陰で支える頼れる仲間という立場。

                [闇堕ちカテゴリと選択肢]

                [闇堕ち度(100%で標準の闇堕ち状態)]
                100%（通常）: 闇への傾倒が進み、迷いながらも暗い選択を受け入れ始めています。

                条件:
                1. キャラクターが闇堕ちに至った心理的な揺らぎや事件を描写する。
                2. 闇堕ち後のビジュアルや能力、価値観の変化を盛り込む。
                3. 最後は読者が次の展開を想像できる余韻を残す。
                4. 出力は日本語で行う。
                """;

        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 連続する空行が1行に圧縮される一方、意図的な空行が保持されることを確認します。
     */
    @Test
    void renderCollapsesConsecutiveBlankLines() {
        CharacterInput input = new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(3L, "ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "堕ちた騎士", "堕落した守護者")),
                "段落A\n\n段落B",
                3,
                "闇の囁き\n\n\n深淵の叫び");
        DarknessSelection selection = new DarknessSelection(Map.of(), DarknessPreset.HEAVY);

        String actual = renderer.render(input, selection);

        assertThat(actual).contains("段落A\n\n段落B");
        assertThat(actual).contains("闇の囁き\n\n深淵の叫び");
        assertThat(actual).doesNotContain("\n\n\n");
        assertThat(actual).endsWith("\n");
    }
}
