package com.example.darkchar.service.openai;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.openai.client.OpenAI;
import com.openai.exceptions.OpenAIException;
import com.openai.models.Response;
import com.openai.models.ResponseCreateParams;
import com.openai.models.ResponseOutput;
import com.openai.models.ResponseOutputContent;
import com.openai.models.ResponseOutputContentText;
import com.openai.models.Usage;

/**
 * {@link OpenAI} SDK を使用したキャラクター生成クライアント実装。
 */
@Component
public class OpenAiCharacterGenerationSdkClient implements OpenAiCharacterGenerationClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCharacterGenerationSdkClient.class);
    private static final int MAX_OUTPUT_TOKENS = 600;
    private static final double TEMPERATURE = 0.8d;
    private static final Set<String> TEMPERATURE_UNSUPPORTED_ERROR_CODES = Set.of(
            "temperature_not_supported",
            "model_capabilities.temperature_not_supported");

    private final OpenAiClientFactory clientFactory;

    public OpenAiCharacterGenerationSdkClient(OpenAiClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public String generateNarrative(String apiKey, String modelId, CharacterInput input, DarknessSelection selection) {
        String normalizedModel = modelId == null ? null : modelId.trim();
        if (normalizedModel == null || normalizedModel.isEmpty()) {
            throw new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。");
        }

        OpenAI client = clientFactory.createClient(apiKey);

        boolean triedWithoutTemperature = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean includeTemperature = attempt == 0;
            if (!includeTemperature) {
                triedWithoutTemperature = true;
            }

            logger.info("Calling OpenAI responses API via SDK: model={}, temperature={}, maxOutputTokens={}",
                    normalizedModel, includeTemperature ? TEMPERATURE : "(omitted)", MAX_OUTPUT_TOKENS);
            try {
                ResponseCreateParams request = buildRequest(normalizedModel, input, selection, includeTemperature);
                Response response = client.responses().create(request);
                logResponseMetadata(response);
                String text = extractText(response);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            } catch (OpenAIException ex) {
                logger.warn("OpenAI responses API call failed: status={}, code={}, message={}",
                        safeStatus(ex), safeCode(ex), ex.getMessage());
                if (!triedWithoutTemperature && isTemperatureUnsupported(ex)) {
                    logger.info("Model {} does not support temperature; retrying without temperature.", normalizedModel);
                    continue;
                }
                throw new OpenAiIntegrationException("OpenAI API呼び出しに失敗しました。", ex);
            } catch (RuntimeException ex) {
                logger.warn("Unexpected error while calling OpenAI responses API: {}", ex.getMessage());
                throw new OpenAiIntegrationException("OpenAI APIへのリクエスト中にエラーが発生しました。", ex);
            }
        }

        throw new OpenAiIntegrationException("OpenAIレスポンスからテキストを取得できませんでした。");
    }

    private ResponseCreateParams buildRequest(String modelId, CharacterInput input, DarknessSelection selection,
            boolean includeTemperature) {
        ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
                .model(modelId)
                .input(List.of(ResponseCreateParams.Input.builder()
                        .role("user")
                        .content(List.of(ResponseCreateParams.InputContent.ofText(
                                ResponseCreateParams.InputContentText.builder()
                                        .text(buildPrompt(input, selection))
                                        .build())))
                        .build()))
                .maxOutputTokens(Long.valueOf(MAX_OUTPUT_TOKENS));
        if (includeTemperature) {
            builder.temperature(Double.valueOf(TEMPERATURE));
        }
        return builder.build();
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
        for (var entry : selection.selections().entrySet()) {
            List<AttributeOption> options = entry.getValue();
            if (options == null || options.isEmpty()) {
                continue;
            }
            builder.append(entry.getKey().getDisplayName()).append(": ");
            StringJoiner joiner = new StringJoiner("、");
            for (AttributeOption option : options) {
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

    private String extractText(Response response) {
        if (response == null || response.output() == null) {
            return null;
        }
        return response.output().stream()
                .filter(Objects::nonNull)
                .map(ResponseOutput::content)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(ResponseOutputContent::text)
                .filter(Objects::nonNull)
                .map(ResponseOutputContentText::value)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    private void logResponseMetadata(Response response) {
        Usage usage = response == null ? null : response.usage();
        logger.info("Received OpenAI response: id={}, model={}, usage={}",
                response == null ? "n/a" : response.id(),
                response == null ? "n/a" : response.model(),
                usage == null ? "prompt=?, completion=?, total=?"
                        : "prompt=%s, completion=%s, total=%s".formatted(
                                usage.promptTokens(), usage.completionTokens(), usage.totalTokens()));
    }

    private boolean isTemperatureUnsupported(OpenAIException ex) {
        String code = normalizeIdentifier(safeCode(ex));
        if (code != null && TEMPERATURE_UNSUPPORTED_ERROR_CODES.contains(code)) {
            return true;
        }
        String message = normalizeMessageIndicator(ex == null ? null : ex.getMessage());
        return message != null && message.contains("temperature") && message.contains("not supported");
    }

    private static String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMessageIndicator(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String safeStatus(OpenAIException ex) {
        if (ex == null) {
            return "n/a";
        }
        try {
            Method method = ex.getClass().getMethod("getStatusCode");
            Object value = method.invoke(ex);
            return value == null ? "n/a" : value.toString();
        } catch (Exception ignored) {
            return "n/a";
        }
    }

    private static String safeCode(OpenAIException ex) {
        if (ex == null) {
            return null;
        }
        try {
            Method errorMethod = ex.getClass().getMethod("getError");
            Object error = errorMethod.invoke(ex);
            if (error == null) {
                return null;
            }
            Method codeMethod = error.getClass().getMethod("getCode");
            Object code = codeMethod.invoke(error);
            return code == null ? null : code.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
