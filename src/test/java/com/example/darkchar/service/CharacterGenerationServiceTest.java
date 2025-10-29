package com.example.darkchar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.service.GenerationResult;
import com.example.darkchar.service.OpenAiApiKeyStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CharacterGenerationServiceTest {

    private final OpenAiApiKeyStore keyStore = new OpenAiApiKeyStore();
    private final CharacterGenerationService service = new CharacterGenerationService(keyStore,
            (apiKey, input, selection) -> {
                throw new AssertionError("OpenAI client should not be invoked in this test");
            });

    @Test
    void generateShouldBuildNarrative() {
        CharacterInput input = new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(1L, "中世ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "勇敢な守護者", "勇敢さ")),
                "盾となって仲間を守る", 
                2,
                "親友を救いたい");

        Map<AttributeCategory, List<AttributeOption>> darkness = Map.of(
                AttributeCategory.MOTIVE,
                List.of(new AttributeOption(10L, AttributeCategory.MOTIVE, "復讐心", "復讐")),
                AttributeCategory.APPEARANCE,
                List.of(new AttributeOption(20L, AttributeCategory.APPEARANCE, "白髪化", "白髪化")));

        DarknessSelection selection = new DarknessSelection(darkness, 4);

        GenerationResult result = service.generate(input, selection);
        GeneratedCharacter character = result.generatedCharacter();

        assertThat(result.usedOpenAi()).isFalse();
        assertThat(result.warningMessage()).isEmpty();
        assertThat(character.narrative()).contains("中世ダークファンタジー");
        assertThat(character.narrative()).contains("復讐心");
        assertThat(character.narrative()).contains("白髪化");
        assertThat(character.narrative()).contains("闇堕ち度: 4/5");
        assertThat(character.narrative()).contains("■キャラクター属性メモ");
        assertThat(character.narrative()).contains("盾となって仲間を守る");
        assertThat(character.narrative()).contains("■闇堕ちメモ");
        assertThat(character.narrative()).contains("親友を救いたい");
    }
}
