package com.example.darkchar.service.openai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("worldGenre", input.worldGenre() != null ? input.worldGenre().name() : "");
        placeholders.put("mode", input.mode() == InputMode.AUTO ? "オート" : "セミオート");
        placeholders.put("characterAttributesSection", buildCharacterAttributesSection(input));
        placeholders.put("traitFreeTextSection", buildTraitFreeTextSection(input));
        placeholders.put("protagonistScore", String.valueOf(input.protagonistScore()));
        placeholders.put("darknessSelections", buildDarknessSelections(selection));
        placeholders.put("darknessLevel", String.valueOf(selection.darknessLevel()));
        placeholders.put("darknessFreeTextSection", buildDarknessFreeTextSection(input));

        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    private String buildCharacterAttributesSection(CharacterInput input) {
        if (input.mode() != InputMode.SEMI_AUTO) {
            return "";
        }
        List<AttributeOption> traits = input.characterTraits();
        if (traits == null || traits.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[キャラクター属性]\n");
        for (AttributeOption option : traits) {
            if (option == null) {
                continue;
            }
            builder.append("・").append(option.name());
            if (option.description() != null && !option.description().isBlank()) {
                builder.append(": ").append(option.description());
            }
            builder.append("\n");
        }
        builder.append("\n");
        return builder.toString();
    }

    private String buildTraitFreeTextSection(CharacterInput input) {
        String freeText = input.traitFreeText();
        if (freeText == null || freeText.isBlank()) {
            return "";
        }
        return "[キャラクター属性メモ]\n" + freeText.trim() + "\n\n";
    }

    private String buildDarknessSelections(DarknessSelection selection) {
        StringBuilder builder = new StringBuilder();
        if (selection != null && selection.selections() != null) {
            for (var entry : selection.selections().entrySet()) {
                List<AttributeOption> options = entry.getValue();
                if (options == null || options.isEmpty()) {
                    continue;
                }
                StringJoiner joiner = new StringJoiner("、");
                for (AttributeOption option : options) {
                    if (option != null) {
                        joiner.add(option.name());
                    }
                }
                builder.append(entry.getKey().getDisplayName())
                        .append(": ")
                        .append(joiner)
                        .append("\n");
            }
        }
        builder.append("\n");
        return builder.toString();
    }

    private String buildDarknessFreeTextSection(CharacterInput input) {
        String freeText = input.darknessFreeText();
        if (freeText == null || freeText.isBlank()) {
            return "";
        }
        return "[闇堕ちメモ]\n" + freeText.trim() + "\n\n";
    }

    private String loadTemplate(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(TEMPLATE_LOCATION);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt template from " + TEMPLATE_LOCATION, ex);
        }
    }
}
