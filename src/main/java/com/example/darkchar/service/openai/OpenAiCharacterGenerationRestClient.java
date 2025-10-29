package com.example.darkchar.service.openai;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link RestClient} を利用した OpenAI 連携クライアント実装。
 */
@Component
public class OpenAiCharacterGenerationRestClient implements OpenAiCharacterGenerationClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCharacterGenerationRestClient.class);
    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String MODEL = "gpt-4o-mini";

    private final RestClient restClient;

    public OpenAiCharacterGenerationRestClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String generateNarrative(String apiKey, CharacterInput input, DarknessSelection selection) {
        try {
            logger.debug("Calling OpenAI responses API with model {}", MODEL);
            OpenAiResponse response = restClient.post()
                    .uri("/responses")
                    .headers(headers -> {
                        headers.setBearerAuth(apiKey);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .body(buildRequest(input, selection))
                    .retrieve()
                    .body(OpenAiResponse.class);

            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new OpenAiIntegrationException("OpenAIレスポンスからテキストを取得できませんでした。");
            }
            return text.trim();
        } catch (RestClientResponseException ex) {
            String message = "OpenAI API呼び出しに失敗しました: HTTP %d %s".formatted(ex.getRawStatusCode(), ex.getStatusText());
            throw new OpenAiIntegrationException(message, ex);
        } catch (RestClientException ex) {
            throw new OpenAiIntegrationException("OpenAI APIへのリクエスト中にエラーが発生しました。", ex);
        }
    }

    private OpenAiRequest buildRequest(CharacterInput input, DarknessSelection selection) {
        String prompt = buildPrompt(input, selection);
        OpenAiContent content = new OpenAiContent("input_text", prompt);
        OpenAiMessage message = new OpenAiMessage("user", List.of(content));
        return new OpenAiRequest(MODEL, List.of(message), 0.8, 600);
    }

    private String buildPrompt(CharacterInput input, DarknessSelection selection) {
        StringBuilder builder = new StringBuilder();
        builder.append("あなたは闇堕ちキャラクターの設定と短いストーリーを作成する日本語のライターです。\n");
        builder.append("以下の入力情報に基づき、世界観の説明、キャラクターの転落のきっかけ、闇堕ち後の姿を含む物語を400〜600文字程度で作成してください。\n");
        builder.append("最後に箇条書きは使わず、段落構成でまとめてください。\n\n");

        builder.append("[世界観ジャンル]\n");
        builder.append(input.worldGenre().name()).append("\n\n");

        builder.append("[モード]\n");
        builder.append(input.mode() == InputMode.AUTO ? "オート" : "セミオート").append("\n\n");

        if (input.mode() == InputMode.SEMI_AUTO && input.characterTraits() != null && !input.characterTraits().isEmpty()) {
            builder.append("[キャラクター属性]\n");
            for (AttributeOption option : input.characterTraits()) {
                builder.append("・").append(option.name()).append(": ").append(option.description()).append("\n");
            }
            builder.append("\n");
        }

        if (input.traitFreeText() != null && !input.traitFreeText().isBlank()) {
            builder.append("[キャラクター属性メモ]\n");
            builder.append(input.traitFreeText().trim()).append("\n\n");
        }

        builder.append("[主人公度]\n");
        builder.append(input.protagonistScore()).append("/5\n\n");

        builder.append("[闇堕ちカテゴリと選択肢]\n");
        Map<AttributeCategory, List<AttributeOption>> darknessSelections = selection.selections();
        for (Map.Entry<AttributeCategory, List<AttributeOption>> entry : darknessSelections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            builder.append(entry.getKey().getDisplayName()).append(": ");
            StringJoiner joiner = new StringJoiner("、");
            for (AttributeOption option : entry.getValue()) {
                joiner.add(option.name());
            }
            builder.append(joiner).append("\n");
        }
        builder.append("\n");

        builder.append("[闇堕ち度]\n");
        builder.append(selection.darknessLevel()).append("/5\n\n");

        if (input.darknessFreeText() != null && !input.darknessFreeText().isBlank()) {
            builder.append("[闇堕ちメモ]\n");
            builder.append(input.darknessFreeText().trim()).append("\n\n");
        }

        builder.append("条件:\n");
        builder.append("1. キャラクターが闇堕ちに至った心理的な揺らぎや事件を描写する。\n");
        builder.append("2. 闇堕ち後のビジュアルや能力、価値観の変化を盛り込む。\n");
        builder.append("3. 最後は読者が次の展開を想像できる余韻を残す。\n");
        builder.append("4. 出力は日本語で行う。\n");

        return builder.toString();
    }

    private String extractText(OpenAiResponse response) {
        if (response == null || response.output() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (OpenAiOutput output : response.output()) {
            if (output == null || output.content() == null) {
                continue;
            }
            for (OpenAiResponseContent content : output.content()) {
                if (content == null) {
                    continue;
                }
                if (Objects.equals(content.type(), "output_text") || Objects.equals(content.type(), "text")) {
                    if (content.text() != null) {
                        builder.append(content.text());
                    }
                }
            }
        }
        return builder.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiRequest(
            String model,
            List<OpenAiMessage> input,
            double temperature,
            @JsonProperty("max_output_tokens") int maxOutputTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiMessage(String role, List<OpenAiContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponse(List<OpenAiOutput> output) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiOutput(List<OpenAiResponseContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponseContent(String type, String text) {
    }
}
