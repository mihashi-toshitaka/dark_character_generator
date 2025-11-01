package com.example.darkchar.service.openai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.ProtagonistAlignment;

/**
 * テンプレートを用いて OpenAI へのプロンプトを生成します。
 */
@Component
public class PromptTemplateRenderer {

    private static final String TEMPLATE_LOCATION = "classpath:prompts/dark_character_prompt.txt";

    private final String template;

    /**
     * テンプレートファイルを読み込みます。
     *
     * @param resourceLoader リソースローダ
     */
    public PromptTemplateRenderer(ResourceLoader resourceLoader) {
        this.template = loadTemplate(resourceLoader);
    }

    /**
     * 入力内容をテンプレートに埋め込みます。
     *
     * @param input     キャラクター入力
     * @param selection 闇堕ち選択
     * @return レンダリング済みプロンプト
     */
    public String render(CharacterInput input, DarknessSelection selection) {
        Map<String, String> placeholders = Map.of(
                "worldGenre", getWorldGenreName(input),
                "characterAttributesSection", buildCharacterAttributesSection(input),
                "traitFreeTextSection", buildTraitFreeTextSection(input),
                "protagonistAlignmentSection", buildProtagonistAlignmentSection(input),
                "darknessSelections", buildDarknessSelections(selection),
                "darknessLevel", selection != null ? formatPercent(selection.darknessLevel()) : "",
                "darknessFreeTextSection", buildDarknessFreeTextSection(input));

        return renderTemplate(placeholders);
    }

    /**
     * キャラクター属性セクションを組み立てます。
     *
     * @param input キャラクター入力
     * @return セクション文字列
     */
    private String buildCharacterAttributesSection(CharacterInput input) {
        if (input.mode() != InputMode.SEMI_AUTO) {
            return "";
        }
        List<AttributeOption> traits = input.characterTraits();
        if (traits == null || traits.isEmpty()) {
            return "";
        }
        StringJoiner lines = new StringJoiner("\n");
        for (AttributeOption option : traits) {
            String formatted = formatCharacterTrait(option);
            if (!formatted.isEmpty()) {
                lines.add(formatted);
            }
        }
        if (lines.length() == 0) {
            return "";
        }
        return "[キャラクター属性]\n" + lines + "\n\n";
    }

    /**
     * 属性メモのセクションを組み立てます。
     *
     * @param input キャラクター入力
     * @return セクション文字列
     */
    private String buildTraitFreeTextSection(CharacterInput input) {
        return formatFreeTextSection("[キャラクター属性メモ]", input.traitFreeText());
    }

    /**
     * 主人公度セクションを組み立てます。
     *
     * @param input キャラクター入力
     * @return セクション文字列
     */
    private String buildProtagonistAlignmentSection(CharacterInput input) {
        return ProtagonistAlignment.fromScore(input.protagonistScore())
                .map(alignment -> alignment.formatPromptLine() + "\n\n")
                .orElse("\n");
    }

    /**
     * 闇堕ちカテゴリの行を構築します。
     *
     * @param selection 闇堕ち選択
     * @return セクション文字列
     */
    private String buildDarknessSelections(DarknessSelection selection) {
        if (selection == null || selection.selections() == null || selection.selections().isEmpty()) {
            return "";
        }
        StringJoiner lines = new StringJoiner("\n");
        for (var entry : selection.selections().entrySet()) {
            var category = entry.getKey();
            String optionNames = formatOptionNames(entry.getValue());
            if (category == null || optionNames.isEmpty()) {
                continue;
            }
            lines.add(category.getDisplayName() + ": " + optionNames);
        }
        if (lines.length() == 0) {
            return "";
        }
        return lines + "\n\n";
    }

    /**
     * 闇堕ちメモのセクションを組み立てます。
     *
     * @param input キャラクター入力
     * @return セクション文字列
     */
    private String buildDarknessFreeTextSection(CharacterInput input) {
        return formatFreeTextSection("[闇堕ちメモ]", input.darknessFreeText());
    }

    /**
     * テンプレートファイルを読み込みます。
     *
     * @param resourceLoader リソースローダ
     * @return テンプレート文字列
     */
    private String loadTemplate(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(TEMPLATE_LOCATION);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt template from " + TEMPLATE_LOCATION, ex);
        }
    }

    /**
     * 数値をパーセント表記にします。
     *
     * @param value 数値
     * @return パーセント文字列
     */
    private String formatPercent(int value) {
        return value + "%";
    }

    /**
     * 世界観名を取得します。
     *
     * @param input キャラクター入力
     * @return 世界観名
     */
    private String getWorldGenreName(CharacterInput input) {
        return input.worldGenre() != null ? input.worldGenre().name() : "";
    }

    /**
     * テンプレート中のプレースホルダを置き換えます。
     *
     * @param placeholders 差し込み値
     * @return 置換後の文字列
     */
    private String renderTemplate(Map<String, String> placeholders) {
        String rendered = template;
        for (var entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return collapseConsecutiveBlankLines(rendered);
    }

    /**
     * 連続する空行を1行にまとめます。
     *
     * @param text 対象文字列
     * @return 整形後の文字列
     */
    private String collapseConsecutiveBlankLines(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int length = text.length();
        int index = 0;
        boolean previousBlankLine = false;

        while (index < length) {
            int newlineIndex = text.indexOf('\n', index);
            String line;
            int nextIndex;
            if (newlineIndex >= 0) {
                line = text.substring(index, newlineIndex);
                nextIndex = newlineIndex + 1;
            } else {
                line = text.substring(index);
                nextIndex = length;
            }

            boolean isBlankLine = line.isBlank();
            if (isBlankLine && previousBlankLine) {
                index = nextIndex;
                continue;
            }

            result.append(line);
            if (newlineIndex >= 0) {
                result.append('\n');
            }

            previousBlankLine = isBlankLine;
            index = nextIndex;
        }

        return result.toString();
    }

    /**
     * 属性1件をフォーマットします。
     *
     * @param option 属性
     * @return 表示文字列
     */
    private String formatCharacterTrait(AttributeOption option) {
        if (option == null || !hasText(option.name())) {
            return "";
        }
        StringBuilder line = new StringBuilder("・").append(option.name().trim());
        if (hasText(option.description())) {
            line.append(": ").append(option.description().trim());
        }
        return line.toString();
    }

    /**
     * 属性名だけを連結します。
     *
     * @param options 属性一覧
     * @return カンマ区切り文字列
     */
    private String formatOptionNames(List<AttributeOption> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("、");
        for (AttributeOption option : options) {
            String name = extractOptionName(option);
            if (!name.isEmpty()) {
                joiner.add(name);
            }
        }
        return joiner.length() == 0 ? "" : joiner.toString();
    }

    /**
     * 属性から名称を抽出します。
     *
     * @param option 属性
     * @return 属性名
     */
    private String extractOptionName(AttributeOption option) {
        if (option == null || !hasText(option.name())) {
            return "";
        }
        return option.name().trim();
    }

    /**
     * 任意テキストセクションをフォーマットします。
     *
     * @param heading 見出し
     * @param freeText 本文
     * @return セクション文字列
     */
    private String formatFreeTextSection(String heading, String freeText) {
        if (!hasText(freeText)) {
            return "";
        }
        return heading + "\n" + freeText.trim() + "\n\n";
    }

    /**
     * テキストが存在するか判定します。
     *
     * @param value 文字列
     * @return 有効なら true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
