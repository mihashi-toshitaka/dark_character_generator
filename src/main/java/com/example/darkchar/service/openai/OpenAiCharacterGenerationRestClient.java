package com.example.darkchar.service.openai;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link RestClient} を利用した OpenAI 連携クライアント実装。
 */
@Component
public class OpenAiCharacterGenerationRestClient implements OpenAiCharacterGenerationClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCharacterGenerationRestClient.class);
    private static final Set<String> TEMPERATURE_UNSUPPORTED_ERROR_CODES = Set.of(
            "temperature_not_supported",
            "model_capabilities.temperature_not_supported");
    private static final Set<String> TEMPERATURE_UNSUPPORTED_NORMALIZED_MESSAGES = Set
            .of("'temperature' is not supported for this model.",
                    "'temperature' is not supported by this model.",
                    "this model does not support the parameter `temperature`.",
                    "the parameter `temperature` is not supported by this model.")
            .stream()
            .map(OpenAiCharacterGenerationRestClient::normalizeMessageIndicator)
            .collect(Collectors.toUnmodifiableSet());
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCharacterGenerationRestClient(@Qualifier("openAiRestClientBuilder") RestClient.Builder builder,
            ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateNarrative(String apiKey, String modelId, CharacterInput input, DarknessSelection selection) {
        String normalizedModel = modelId == null ? null : modelId.trim();
        if (normalizedModel == null || normalizedModel.isEmpty()) {
            throw new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。");
        }
        // 最初は temperature を付けて試行し、APIが未対応の場合は temperature を外して1回だけ再試行する。
        String responseText = null;
        boolean triedWithoutTemperature = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean includeTemperature = (attempt == 0);
            if (!includeTemperature) {
                triedWithoutTemperature = true;
            }
            logger.info("Calling OpenAI responses API: model={}, temperature={}, maxOutputTokens={}",
                    normalizedModel, includeTemperature ? 0.8 : "(omitted)", 600);
            try {
                OpenAiRequest requestBody = buildRequest(normalizedModel, input, selection, includeTemperature);
                OpenAiResponse response = restClient.post()
                        .uri("/responses")
                        .headers(headers -> {
                            headers.setBearerAuth(apiKey);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .body(requestBody)
                        .retrieve()
                        .body(OpenAiResponse.class);

                if (response != null) {
                    logResponseMetadata(response);
                    String extracted = extractText(response);
                    if (extracted != null && !extracted.isBlank()) {
                        responseText = extracted;
                        // 正常終了すればループを抜ける
                        break;
                    }
                }
            } catch (RestClientResponseException ex) {
                String respBody = null;
                OpenAiErrorResponse errorResponse = null;
                try {
                    respBody = ex.getResponseBodyAsString();
                    errorResponse = parseErrorResponse(objectMapper, respBody);
                } catch (Exception e) {
                    // 無理に取得できなくても続行
                }

                OpenAiError errorDetails = errorResponse == null ? null : errorResponse.error();
                logger.warn(
                        "OpenAI responses API call failed: status={} {}, model={}, errorType={}, errorCode={}, errorMessage={}",
                        ex.getStatusCode().value(), ex.getStatusText(), normalizedModel,
                        safeValue(errorDetails == null ? null : errorDetails.type()),
                        safeValue(errorDetails == null ? null : errorDetails.code()),
                        safeValue(errorDetails == null ? null : errorDetails.message()));
                if (respBody != null && !respBody.isBlank() && logger.isDebugEnabled()) {
                    logger.debug("OpenAI responses API raw error body: {}", respBody);
                }

                // temperature が未サポートという明示的なエラーなら、次のループで temperature を外して再試行する
                if (!triedWithoutTemperature && isTemperatureUnsupported(errorResponse)) {
                    logger.info(
                            "Model {} does not support temperature; retrying without temperature. errorContext={}",
                            normalizedModel, formatErrorSummary(errorResponse));
                    continue; // 次のattemptで includeTemperature=false にする
                }

                String message = "OpenAI API呼び出しに失敗しました: HTTP %d %s".formatted(ex.getStatusCode().value(),
                        ex.getStatusText());
                if (errorDetails != null) {
                    message += " (errorType=%s, errorCode=%s, errorMessage=%s)".formatted(
                            safeValue(errorDetails.type()), safeValue(errorDetails.code()),
                            safeValue(errorDetails.message()));
                }
                throw new OpenAiIntegrationException(message, ex);
            } catch (RestClientException ex) {
                logger.warn("OpenAI responses API request encountered an error for model {}: {}", normalizedModel,
                        ex.getMessage());
                throw new OpenAiIntegrationException("OpenAI APIへのリクエスト中にエラーが発生しました。", ex);
            }
        }

        if (responseText != null && !responseText.isBlank()) {
            return responseText.trim();
        }

        throw new OpenAiIntegrationException("OpenAIレスポンスからテキストを取得できませんでした。");
    }

    /**
     * OpenAI Responses API に送信するリクエストをDTOとして構築する。
     */
    private OpenAiRequest buildRequest(String modelId, CharacterInput input, DarknessSelection selection,
            boolean includeTemperature) {
        String prompt = buildPrompt(input, selection);
        OpenAiContent content = new OpenAiContent("input_text", prompt);
        OpenAiMessage message = new OpenAiMessage("user", List.of(content));
        Double temperature = includeTemperature ? Double.valueOf(0.8) : null;
        return new OpenAiRequest(modelId, List.of(message), temperature, 600);
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

    private String formatUsage(OpenAiUsage usage) {
        if (usage == null) {
            return "usage=unknown";
        }
        return "prompt=%s, completion=%s, total=%s".formatted(
                usage.promptTokens() == null ? "?" : usage.promptTokens(),
                usage.completionTokens() == null ? "?" : usage.completionTokens(),
                usage.totalTokens() == null ? "?" : usage.totalTokens());
    }

    private void logResponseMetadata(OpenAiResponse response) {
        try {
            logger.info("Received OpenAI response: id={}, created={}, model={}, usage={}",
                    response.id(), response.created(), response.model(), formatUsage(response.usage()));
            if (logger.isDebugEnabled()) {
                logger.debug("OpenAI typed response payload: {}", objectMapper.writeValueAsString(response));
            }
        } catch (JsonProcessingException e) {
            logger.debug("Failed to serialize OpenAI response for logging: {}", e.getMessage());
        }
    }

    static OpenAiErrorResponse parseErrorResponse(ObjectMapper mapper, String responseBody) {
        if (mapper == null || responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(responseBody, OpenAiErrorResponse.class);
        } catch (JsonProcessingException e) {
            logger.debug("Failed to deserialize OpenAI error response: {}", e.getMessage());
            return null;
        }
    }

    static boolean isTemperatureUnsupported(OpenAiErrorResponse errorResponse) {
        if (errorResponse == null || errorResponse.error() == null) {
            return false;
        }
        OpenAiError error = errorResponse.error();
        String code = normalizeIdentifier(error.code());
        if (code != null && TEMPERATURE_UNSUPPORTED_ERROR_CODES.contains(code)) {
            return true;
        }
        String normalizedMessage = normalizeMessageIndicator(error.message());
        return normalizedMessage != null && TEMPERATURE_UNSUPPORTED_NORMALIZED_MESSAGES.contains(normalizedMessage);
    }

    private static String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeMessageIndicator(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ");
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }

    private static String formatErrorSummary(OpenAiErrorResponse errorResponse) {
        if (errorResponse == null || errorResponse.error() == null) {
            return "type=n/a, code=n/a, message=n/a, param=n/a";
        }
        OpenAiError error = errorResponse.error();
        return "type=%s, code=%s, message=%s, param=%s".formatted(
                safeValue(error.type()),
                safeValue(error.code()),
                safeValue(error.message()),
                safeValue(error.param()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponse(
            String id,
            String model,
            Long created,
            OpenAiUsage usage,
            List<OpenAiOutput> output) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiOutput(List<OpenAiResponseContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponseContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiUsage(
            @JsonProperty("input_tokens") Integer promptTokens,
            @JsonProperty("output_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static record OpenAiErrorResponse(OpenAiError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static record OpenAiError(
            String type,
            String code,
            String message,
            String param) {
    }
}
