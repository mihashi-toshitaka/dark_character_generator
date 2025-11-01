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
import com.example.darkchar.domain.ProtagonistAlignment;
import com.example.darkchar.service.ai.AiProviderContext;
import com.example.darkchar.service.ai.AiProviderContextStore;
import com.example.darkchar.service.ai.CharacterGenerationProvider;
import com.example.darkchar.service.ai.CharacterGenerationStrategyRegistry;
import com.example.darkchar.service.ai.ProviderConfigurationStatus;
import com.example.darkchar.service.ai.ProviderGenerationResult;
import com.example.darkchar.service.ai.ProviderType;
import com.example.darkchar.service.openai.OpenAiIntegrationException;

/**
 * キャラクター生成ロジックをまとめたサービスです。
 */
@Service
public class CharacterGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterGenerationService.class);

    private final AiProviderContextStore providerContextStore;
    private final CharacterGenerationStrategyRegistry strategyRegistry;

    /**
     * 依存サービスを注入します。
     *
     * @param providerContextStore プロバイダ設定ストア
     * @param strategyRegistry     プロバイダレジストリ
     */
    public CharacterGenerationService(AiProviderContextStore providerContextStore,
            CharacterGenerationStrategyRegistry strategyRegistry) {
        this.providerContextStore = providerContextStore;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * アクティブなプロバイダでキャラクターを生成します。
     *
     * @param input             ユーザー入力
     * @param darknessSelection 闇堕ち選択
     * @return 生成結果
     */
    public GenerationResult generate(CharacterInput input, DarknessSelection darknessSelection) {
        return generate(input, darknessSelection, providerContextStore.getActiveProviderType());
    }

    /**
     * 指定したプロバイダを用いてキャラクターを生成します。
     *
     * @param input             ユーザー入力
     * @param darknessSelection 闇堕ち選択
     * @param providerType      利用するプロバイダ
     * @return 生成結果
     */
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

    /**
     * 生成内容を {@link GenerationResult} にまとめます。
     *
     * @param input             入力情報
     * @param darknessSelection 闇堕ち選択
     * @param narrative         生成テキスト
     * @param usedProvider      プロバイダ利用有無
     * @param warning           警告メッセージ
     * @param prompt            使用プロンプト
     * @return 変換した結果
     */
    private GenerationResult buildResult(CharacterInput input, DarknessSelection darknessSelection, String narrative,
            boolean usedProvider, Optional<String> warning, Optional<String> prompt) {
        GeneratedCharacter character = new GeneratedCharacter(input, darknessSelection, narrative, Instant.now());
        return new GenerationResult(character, usedProvider, warning, prompt);
    }

    /**
     * 入力内容を検証します。
     *
     * @param input             入力情報
     * @param darknessSelection 闇堕ち選択
     */
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

    /**
     * ローカル生成用の文章を作成します。
     *
     * @param input             入力情報
     * @param darknessSelection 闇堕ち選択
     * @return 生成した文章
     */
    private String buildNarrative(CharacterInput input, DarknessSelection darknessSelection) {
        StringBuilder builder = new StringBuilder();
        builder.append("【闇堕ちキャラクター概要】\n");
        builder.append("世界観ジャンル: ").append(input.worldGenre().name()).append('\n');
        builder.append("モード: ").append(input.mode() == InputMode.AUTO ? "オート" : "セミオート").append('\n');
        ProtagonistAlignment alignment = ProtagonistAlignment.fromScore(input.protagonistScore()).orElse(null);
        builder.append("闇堕ち前の立ち位置: ");
        if (alignment != null) {
            builder.append(alignment.getPromptDescription())
                    .append(" (主人公度 ")
                    .append(alignment.getScore())
                    .append("/5)");
        } else {
            builder.append("不明 (主人公度 ")
                    .append(input.protagonistScore())
                    .append("/5)");
        }
        builder.append('\n').append('\n');

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

    /**
     * 物語部分の文章を構築します。
     *
     * @param input             入力情報
     * @param darknessSelection 闇堕ち選択
     * @return 生成した段落
     */
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
        ProtagonistAlignment alignment = ProtagonistAlignment.fromScore(input.protagonistScore()).orElse(null);
        String alignmentDescription = alignment != null
                ? alignment.getPromptDescription()
                : "主人公陣営との関係は不明";
        String alignmentScore = alignment != null
                ? alignment.getScore() + "/5"
                : input.protagonistScore() + "/5";
        String base = """
                元のキャラクターは%sの世界で活躍していましたが、%s（主人公度%s）が闇への扉を開きました。
                %s。
                闇堕ち度%sの現在、彼/彼女はかつての姿を忘れ、独自の正義で世界を塗り替えようとしています。
                """;
        return base.formatted(
                input.worldGenre().name(),
                alignmentDescription,
                alignmentScore,
                highlightText.isEmpty() ? "闇の属性はまだ不明瞭です" : highlightText,
                formatPercent(darknessSelection.darknessLevel()));
    }

    /**
     * パーセント表記へ変換します。
     *
     * @param value 数値
     * @return パーセント文字列
     */
    private String formatPercent(int value) {
        return value + "%";
    }
}
