package com.example.darkchar.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderGenerationResult;
import com.example.darkchar.service.ai.ProviderType;
import com.example.darkchar.service.openai.OpenAiIntegrationException;

@Service
public class CharacterGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterGenerationService.class);

    private final AiProviderContextStore providerContextStore;
    private final CharacterGenerationStrategyRegistry strategyRegistry;

    public CharacterGenerationService(AiProviderContextStore providerContextStore,
            CharacterGenerationStrategyRegistry strategyRegistry) {
        this.providerContextStore = providerContextStore;
        this.strategyRegistry = strategyRegistry;
    }

    public GenerationResult generate(CharacterInput input, DarknessSelection darknessSelection) {
        return generate(input, darknessSelection, providerContextStore.getActiveProviderType());
    }

    public GenerationResult generate(CharacterInput input, DarknessSelection darknessSelection,
            ProviderType providerType) {
        validate(input, darknessSelection);

        ProviderType effectiveType = providerType == null ? providerContextStore.getActiveProviderType() : providerType;
        Optional<CharacterGenerationProvider> providerOptional = strategyRegistry.findProvider(effectiveType);
        AiProviderContext context = providerContextStore.getContext(effectiveType);

        String narrative;
        boolean usedProvider = false;
        Optional<String> warning = Optional.empty();
        Optional<String> prompt = Optional.empty();

        if (providerOptional.isPresent()) {
            CharacterGenerationProvider provider = providerOptional.get();
            ProviderConfigurationStatus status = provider.assessConfiguration(context);
            if (!status.ready()) {
                logger.warn("{}が未設定のためローカル生成へフォールバックします。", provider.getDisplayName());
                if (status.warningMessage().isPresent()) {
                    warning = status.warningMessage();
                }
                narrative = buildNarrative(input, darknessSelection);
            } else {
                try {
                    ProviderGenerationResult providerResult = provider.generate(context, input, darknessSelection);
                    if (providerResult == null || providerResult.narrative() == null) {
                        throw new OpenAiIntegrationException("プロバイダから有効な結果を取得できませんでした。");
                    }
                    narrative = providerResult.narrative();
                    prompt = providerResult.prompt();
                    usedProvider = true;
                } catch (OpenAiIntegrationException ex) {
                    logger.warn("{}連携に失敗したためローカル生成へフォールバックします: {}", provider.getDisplayName(),
                            ex.getMessage());
                    narrative = buildNarrative(input, darknessSelection);
                    warning = Optional.of(provider.buildFailureWarning(ex));
                } catch (RuntimeException ex) {
                    logger.warn("{}連携に失敗したためローカル生成へフォールバックします: {}", provider.getDisplayName(),
                            ex.getMessage());
                    narrative = buildNarrative(input, darknessSelection);
                    warning = Optional.of(provider.buildFailureWarning(ex));
                }
            }
        } else {
            narrative = buildNarrative(input, darknessSelection);
        }

        return buildResult(input, darknessSelection, narrative, usedProvider, warning, prompt);
    }

    private GenerationResult buildResult(CharacterInput input, DarknessSelection darknessSelection, String narrative,
            boolean usedProvider, Optional<String> warning, Optional<String> prompt) {
        GeneratedCharacter character = new GeneratedCharacter(input, darknessSelection, narrative, Instant.now());
        return new GenerationResult(character, usedProvider, warning, prompt);
    }

    private void validate(CharacterInput input, DarknessSelection darknessSelection) {
        if (input.worldGenre() == null) {
            throw new IllegalArgumentException("世界観ジャンルを選択してください。");
        }
        if (input.mode() == InputMode.SEMI_AUTO
                && (input.characterTraits() == null || input.characterTraits().isEmpty())) {
            throw new IllegalArgumentException("セミオートモードではキャラクター属性を1つ以上選択してください。");
        }
        if (darknessSelection.selections().values().stream().allMatch(List::isEmpty)) {
            throw new IllegalArgumentException("闇堕ちカテゴリから少なくとも1つは選択してください。");
        }
        int darknessLevel = darknessSelection.darknessLevel();
        if (darknessLevel < 10 || darknessLevel > 300 || darknessLevel % 10 != 0) {
            throw new IllegalArgumentException("闇堕ち度は10%から300%の間で10%刻みで選択してください。");
        }
    }

    private String buildNarrative(CharacterInput input, DarknessSelection darknessSelection) {
        StringBuilder builder = new StringBuilder();
        builder.append("【闇堕ちキャラクター概要】\n");
        builder.append("世界観ジャンル: ").append(input.worldGenre().name()).append('\n');
        builder.append("モード: ").append(input.mode() == InputMode.AUTO ? "オート" : "セミオート").append('\n');
        builder.append("主人公度: ").append(input.protagonistScore()).append("/5\n\n");

        if (input.mode() == InputMode.SEMI_AUTO) {
            builder.append("■キャラクター属性\n");
            for (AttributeOption option : input.characterTraits()) {
                builder.append("・").append(option.name()).append(" - ").append(option.description()).append('\n');
            }
            builder.append('\n');
        }

        if (input.traitFreeText() != null && !input.traitFreeText().isBlank()) {
            builder.append("■キャラクター属性メモ\n");
            builder.append(input.traitFreeText().trim()).append("\n\n");
        }

        builder.append("■闇堕ちカテゴリ\n");
        for (Map.Entry<AttributeCategory, List<AttributeOption>> entry : darknessSelection.selections().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            builder.append("【").append(entry.getKey().getDisplayName()).append("】\n");
            for (AttributeOption option : entry.getValue()) {
                builder.append("・").append(option.name()).append(" - ").append(option.description()).append('\n');
            }
        }
        builder.append('\n');
        builder.append("闇堕ち度: ").append(formatPercent(darknessSelection.darknessLevel())).append("\n\n");

        if (input.darknessFreeText() != null && !input.darknessFreeText().isBlank()) {
            builder.append("■闇堕ちメモ\n");
            builder.append(input.darknessFreeText().trim()).append("\n\n");
        }

        builder.append("■生成ストーリー\n");
        builder.append(buildStoryParagraph(input, darknessSelection));
        builder.append('\n');
        return builder.toString();
    }

    private String buildStoryParagraph(CharacterInput input, DarknessSelection darknessSelection) {
        List<String> highlights = new ArrayList<>();
        for (Map.Entry<AttributeCategory, List<AttributeOption>> entry : darknessSelection.selections().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                StringJoiner joiner = new StringJoiner("、");
                entry.getValue().forEach(option -> joiner.add(option.name()));
                highlights.add(entry.getKey().getDisplayName() + "は" + joiner);
            }
        }
        String highlightText = String.join("。", highlights);
        String base = """
                元のキャラクターは%sの世界で活躍していましたが、主人公度%sの揺らぎが闇への扉を開きました。
                %s。
                闇堕ち度%sの現在、彼/彼女はかつての姿を忘れ、独自の正義で世界を塗り替えようとしています。
                """;
        return base.formatted(
                input.worldGenre().name(),
                input.protagonistScore(),
                highlightText.isEmpty() ? "闇の属性はまだ不明瞭です" : highlightText,
                formatPercent(darknessSelection.darknessLevel()));
    }

    private String formatPercent(int value) {
        return value + "%";
    }
}
