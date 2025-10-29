package com.example.darkchar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CharacterGenerationServiceTest {

    private final CharacterGenerationService service = new CharacterGenerationService();

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

        GeneratedCharacter result = service.generate(input, selection);

        assertThat(result.narrative()).contains("中世ダークファンタジー");
        assertThat(result.narrative()).contains("復讐心");
        assertThat(result.narrative()).contains("白髪化");
        assertThat(result.narrative()).contains("闇堕ち度: 4/5");
        assertThat(result.narrative()).contains("■キャラクター属性メモ");
        assertThat(result.narrative()).contains("盾となって仲間を守る");
        assertThat(result.narrative()).contains("■闇堕ちメモ");
        assertThat(result.narrative()).contains("親友を救いたい");
    }
}
