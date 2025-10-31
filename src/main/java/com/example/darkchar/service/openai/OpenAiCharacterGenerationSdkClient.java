package com.example.darkchar.service.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
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
    private final PromptTemplateRenderer promptTemplateRenderer;

    public OpenAiCharacterGenerationSdkClient(OpenAiClientFactory clientFactory,
            PromptTemplateRenderer promptTemplateRenderer) {
        this.clientFactory = clientFactory;
        this.promptTemplateRenderer = promptTemplateRenderer;
    }

    @Override
    public String generateNarrative(String apiKey, String modelId, CharacterInput input, DarknessSelection selection) {
        String normalizedModel = modelId == null ? null : modelId.trim();
        if (normalizedModel == null || normalizedModel.isEmpty()) {
            throw new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。");
        }

        OpenAIClient client = clientFactory.createClient(apiKey);

        String prompt = promptTemplateRenderer.render(input, selection);
        boolean triedWithoutTemperature = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean includeTemperature = attempt == 0;
            if (!includeTemperature) {
                triedWithoutTemperature = true;
            }
            logger.info("Calling OpenAI responses API via SDK: model={}, temperature={}, maxOutputTokens={}",
                    normalizedModel, includeTemperature ? TEMPERATURE : "(omitted)", MAX_OUTPUT_TOKENS);
            try {
                ChatCompletionCreateParams params = buildRequest(normalizedModel, prompt, includeTemperature);
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

    private ChatCompletionCreateParams buildRequest(String modelId, String prompt, boolean includeTemperature) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(modelId)
                .addUserMessage(prompt)
                .maxCompletionTokens(MAX_OUTPUT_TOKENS);
        if (includeTemperature) {
            builder.temperature(TEMPERATURE);
        }
        return builder.build();
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
