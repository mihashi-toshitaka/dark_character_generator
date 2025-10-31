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

@Component
public class PromptTemplateRenderer {

    private static final String TEMPLATE_LOCATION = "classpath:prompts/dark_character_prompt.txt";

    private final String template;

    public PromptTemplateRenderer(ResourceLoader resourceLoader) {
        this.template = loadTemplate(resourceLoader);
    }

    public String render(CharacterInput input, DarknessSelection selection) {
        Map<String, String> placeholders = Map.of(
                "worldGenre", getWorldGenreName(input),
                "mode", toModeLabel(input.mode()),
                "characterAttributesSection", buildCharacterAttributesSection(input),
                "traitFreeTextSection", buildTraitFreeTextSection(input),
                "protagonistScore", Integer.toString(input.protagonistScore()),
                "darknessSelections", buildDarknessSelections(selection),
                "darknessLevel", selection != null ? formatPercent(selection.darknessLevel()) : "",
                "darknessFreeTextSection", buildDarknessFreeTextSection(input));

        return renderTemplate(placeholders);
    }

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

    private String buildTraitFreeTextSection(CharacterInput input) {
        return formatFreeTextSection("[キャラクター属性メモ]", input.traitFreeText());
    }

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

    private String buildDarknessFreeTextSection(CharacterInput input) {
        return formatFreeTextSection("[闇堕ちメモ]", input.darknessFreeText());
    }

    private String loadTemplate(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(TEMPLATE_LOCATION);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt template from " + TEMPLATE_LOCATION, ex);
        }
    }

    private String formatPercent(int value) {
        return value + "%";
    }

    private String getWorldGenreName(CharacterInput input) {
        return input.worldGenre() != null ? input.worldGenre().name() : "";
    }

    private String toModeLabel(InputMode mode) {
        if (mode == null) {
            return "";
        }
        return mode == InputMode.AUTO ? "オート" : "セミオート";
    }

    private String renderTemplate(Map<String, String> placeholders) {
        String rendered = template;
        for (var entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

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

    private String extractOptionName(AttributeOption option) {
        if (option == null || !hasText(option.name())) {
            return "";
        }
        return option.name().trim();
    }

    private String formatFreeTextSection(String heading, String freeText) {
        if (!hasText(freeText)) {
            return "";
        }
        return heading + "\n" + freeText.trim() + "\n\n";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
