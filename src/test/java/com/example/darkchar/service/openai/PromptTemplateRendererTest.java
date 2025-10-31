package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;

class PromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer(new DefaultResourceLoader());

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
                3);

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
                
                [主人公度]
                4/5
                
                [闇堕ちカテゴリと選択肢]
                性向の変質: 復讐心
                
                [闇堕ち度]
                3/5
                
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

    @Test
    void renderOmitsOptionalSectionsWhenAbsent() {
        CharacterInput input = new CharacterInput(
                InputMode.AUTO,
                new WorldGenre(2L, "サイバーパンク"),
                List.of(),
                null,
                2,
                null);
        DarknessSelection selection = new DarknessSelection(Map.of(), 1);

        String actual = renderer.render(input, selection);

        String expected = """
                あなたは闇堕ちキャラクターの設定と短いストーリーを作成する日本語のライターです。
                以下の入力情報に基づき、世界観の説明、キャラクターの転落のきっかけ、闇堕ち後の姿を含む物語を400〜600文字程度で作成してください。
                最後に箇条書きは使わず、段落構成でまとめてください。
                
                [世界観ジャンル]
                サイバーパンク
                
                [モード]
                オート
                
                [主人公度]
                2/5
                
                [闇堕ちカテゴリと選択肢]
                
                [闇堕ち度]
                1/5
                
                条件:
                1. キャラクターが闇堕ちに至った心理的な揺らぎや事件を描写する。
                2. 闇堕ち後のビジュアルや能力、価値観の変化を盛り込む。
                3. 最後は読者が次の展開を想像できる余韻を残す。
                4. 出力は日本語で行う。
                """;

        assertThat(actual).isEqualTo(expected);
    }
}
