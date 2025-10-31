package com.example.darkchar.service.openai;

import java.util.List;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.openai.client.OpenAIClient;
import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * SDK を使用したキャラクター生成クライアント実装。
 */
@Component
public class OpenAiCharacterGenerationSdkClient implements OpenAiCharacterGenerationClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCharacterGenerationSdkClient.class);
    private static final int MAX_OUTPUT_TOKENS = 1800;
    private static final double TEMPERATURE = 0.8d;

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

        OpenAIClient client = clientFactory.createClient(apiKey);

        boolean triedWithoutTemperature = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean includeTemperature = attempt == 0;
            if (!includeTemperature) {
                triedWithoutTemperature = true;
            }
            logger.info("Calling OpenAI responses API via SDK: model={}, temperature={}, maxOutputTokens={}",
                    normalizedModel, includeTemperature ? TEMPERATURE : "(omitted)", MAX_OUTPUT_TOKENS);
            try {
                ChatCompletionCreateParams params = buildRequest(normalizedModel, input, selection, includeTemperature);
                ChatCompletion chatCompletion = client.chat().completions().create(params);
                logResponseMetadata(chatCompletion);
                String text = extractText(chatCompletion);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            } catch (OpenAIException ex) {
                logger.warn("OpenAI responses API call failed: message={}", ex.getMessage());
                if (!triedWithoutTemperature && isTemperatureUnsupported(ex)) {
                    logger.info("Model {} does not support temperature; retrying without temperature.",
                            normalizedModel);
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

    private ChatCompletionCreateParams buildRequest(String modelId, CharacterInput input, DarknessSelection selection,
            boolean includeTemperature) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(modelId)
                .addUserMessage(buildPrompt(input, selection))
                .maxCompletionTokens(MAX_OUTPUT_TOKENS);
        if (includeTemperature) {
            builder.temperature(TEMPERATURE);
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

        if (input.mode() == InputMode.SEMI_AUTO && input.characterTraits() != null
                && !input.characterTraits().isEmpty()) {
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

    private String extractText(ChatCompletion chatCompletion) {
        if (chatCompletion == null) {
            return null;
        }
        return chatCompletion.choices()
                .get(0)
                .message()
                .content()
                .get();
    }

    private void logResponseMetadata(ChatCompletion chatCompletion) {
        logger.info("Received OpenAI response: " + chatCompletion.toString());
    }

    private boolean isTemperatureUnsupported(OpenAIException ex) {
        if (ex instanceof BadRequestException) {
            String message = ex.getMessage();
            return message != null && message.toLowerCase().contains("temperature")
                    && message.toLowerCase().contains("not support");
        }
        return false;
    }

}
