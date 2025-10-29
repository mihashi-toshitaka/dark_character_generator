package com.example.darkchar.service;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.GeneratedCharacter;
import com.example.darkchar.domain.InputMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;

@Service
public class CharacterGenerationService {

    public GeneratedCharacter generate(CharacterInput input, DarknessSelection darknessSelection) {
        validate(input, darknessSelection);
        String narrative = buildNarrative(input, darknessSelection);
        return new GeneratedCharacter(input, darknessSelection, narrative, Instant.now());
    }

    private void validate(CharacterInput input, DarknessSelection darknessSelection) {
        if (input.worldGenre() == null) {
            throw new IllegalArgumentException("世界観ジャンルを選択してください。");
        }
        if (input.mode() == InputMode.SEMI_AUTO && (input.characterTraits() == null || input.characterTraits().isEmpty())) {
            throw new IllegalArgumentException("セミオートモードではキャラクター属性を1つ以上選択してください。");
        }
        if (darknessSelection.selections().values().stream().allMatch(List::isEmpty)) {
            throw new IllegalArgumentException("闇堕ちカテゴリから少なくとも1つは選択してください。");
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
        builder.append("闇堕ち度: ").append(darknessSelection.darknessLevel()).append("/5\n\n");

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
                darknessSelection.darknessLevel());
    }
}
