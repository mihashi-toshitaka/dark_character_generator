package com.example.darkchar.service.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.service.ai.ProviderGenerationResult;
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
    private static final int MAX_OUTPUT_TOKENS = 10000;
    private static final double TEMPERATURE = 0.8d;

    private final OpenAiClientFactory clientFactory;
    private final PromptTemplateRenderer promptTemplateRenderer;

    /**
     * 依存コンポーネントを注入します。
     *
     * @param clientFactory OpenAI クライアントファクトリ
     * @param promptTemplateRenderer プロンプト生成器
     */
    public OpenAiCharacterGenerationSdkClient(OpenAiClientFactory clientFactory,
            PromptTemplateRenderer promptTemplateRenderer) {
        this.clientFactory = clientFactory;
        this.promptTemplateRenderer = promptTemplateRenderer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderGenerationResult generate(String apiKey, String modelId, CharacterInput input, DarknessSelection selection) {
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
                    return new ProviderGenerationResult(text.trim(), prompt);
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

    /**
     * Chat Completions API へのリクエストを構築します。
     *
     * @param modelId            使用するモデルID
     * @param prompt             送信するプロンプト
     * @param includeTemperature 温度パラメータを含めるか
     * @return 生成したパラメータ
     */
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

    /**
     * レスポンスから最初のテキストを取り出します。
     *
     * @param chatCompletion OpenAI レスポンス
     * @return 取得したテキスト
     */
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

    /**
     * レスポンスのメタ情報をログへ出力します。
     *
     * @param chatCompletion OpenAI レスポンス
     */
    private void logResponseMetadata(ChatCompletion chatCompletion) {
        logger.info("Received OpenAI response: " + chatCompletion.toString());
    }

    /**
     * 温度パラメータ未対応のエラーか判定します。
     *
     * @param ex OpenAI 例外
     * @return 温度未対応なら true
     */
    private boolean isTemperatureUnsupported(OpenAIException ex) {
        if (ex instanceof BadRequestException) {
            String message = ex.getMessage();
            return message != null && message.toLowerCase().contains("temperature")
                    && message.toLowerCase().contains("not support");
        }
        return false;
    }

}
