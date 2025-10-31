package com.example.darkchar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderType;

class CharacterGenerationServiceTest {

    private CharacterGenerationService service;
    private AiProviderContextStore contextStore;
    private StubProvider openAiProvider;
    private StubProvider localProvider;

    @BeforeEach
    void setUp() {
        contextStore = new AiProviderContextStore();
        openAiProvider = new StubProvider(ProviderType.OPENAI, "OpenAI");
        localProvider = new StubProvider(ProviderType.LOCAL, "ローカル");
        CharacterGenerationStrategyRegistry registry = new CharacterGenerationStrategyRegistry(
                List.of(openAiProvider, localProvider));
        service = new CharacterGenerationService(contextStore, registry);
    }

    @Test
    void generateShouldBuildNarrativeWhenProviderUnavailable() {
        openAiProvider.configurationStatus = ProviderConfigurationStatus.notReady();

        GenerationResult result = service.generate(sampleInput(), sampleSelection(), ProviderType.OPENAI);
        GeneratedCharacter character = result.generatedCharacter();

        assertThat(result.usedProvider()).isFalse();
        assertThat(result.warningMessage()).isEmpty();
        assertThat(character.narrative()).contains("中世ダークファンタジー");
        assertThat(character.narrative()).contains("復讐心");
        assertThat(character.narrative()).contains("白髪化");
        assertThat(character.narrative()).contains("闇堕ち度: 4/5");
    }

    @Test
    void generateShouldUseProviderWhenConfigured() {
        openAiProvider.configurationStatus = ProviderConfigurationStatus.onReady();
        openAiProvider.generatedNarrative = "remote narrative";

        GenerationResult result = service.generate(sampleInput(), sampleSelection(), ProviderType.OPENAI);

        assertThat(result.usedProvider()).isTrue();
        assertThat(result.warningMessage()).isEmpty();
        assertThat(result.generatedCharacter().narrative()).isEqualTo("remote narrative");
    }

    @Test
    void generateShouldFallbackWithWarningWhenProviderFails() {
        openAiProvider.configurationStatus = ProviderConfigurationStatus.onReady();
        openAiProvider.exceptionToThrow = new RuntimeException("error-detail");

        GenerationResult result = service.generate(sampleInput(), sampleSelection(), ProviderType.OPENAI);

        assertThat(result.usedProvider()).isFalse();
        assertThat(result.warningMessage())
                .hasValue("OpenAI連携に失敗したため、サンプル結果を表示しています。詳細: error-detail");
    }

    @Test
    void generateShouldAllowSwitchingProviders() {
        localProvider.configurationStatus = ProviderConfigurationStatus.onReady();
        localProvider.generatedNarrative = "local narrative";

        GenerationResult result = service.generate(sampleInput(), sampleSelection(), ProviderType.LOCAL);

        assertThat(result.usedProvider()).isTrue();
        assertThat(result.generatedCharacter().narrative()).isEqualTo("local narrative");
    }

    private CharacterInput sampleInput() {
        return new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(1L, "中世ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "勇敢な守護者", "勇敢さ")),
                "盾となって仲間を守る",
                2,
                "親友を救いたい");
    }

    private DarknessSelection sampleSelection() {
        Map<AttributeCategory, List<AttributeOption>> darkness = Map.of(
                AttributeCategory.MOTIVE,
                List.of(new AttributeOption(10L, AttributeCategory.MOTIVE, "復讐心", "復讐")),
                AttributeCategory.APPEARANCE,
                List.of(new AttributeOption(20L, AttributeCategory.APPEARANCE, "白髪化", "白髪化")));
        return new DarknessSelection(darkness, 4);
    }

    private static class StubProvider implements CharacterGenerationProvider {

        private final ProviderType providerType;
        private final String displayName;
        private ProviderConfigurationStatus configurationStatus = ProviderConfigurationStatus.notReady();
        private String generatedNarrative = "";
        private RuntimeException exceptionToThrow;

        StubProvider(ProviderType providerType, String displayName) {
            this.providerType = providerType;
            this.displayName = displayName;
        }

        @Override
        public ProviderType getProviderType() {
            return providerType;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public ProviderConfigurationStatus assessConfiguration(AiProviderContext context) {
            return configurationStatus;
        }

        @Override
        public String generateNarrative(AiProviderContext context, CharacterInput input, DarknessSelection selection) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return generatedNarrative;
        }
    }
}
