package com.example.darkchar.service.openai;

import org.springframework.stereotype.Component;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * Factory responsible for building {@link OpenAI} clients with a dynamically
 * provided API key.
 */
@Component
public class OpenAiClientFactory {

    /**
     * Creates a new {@link OpenAI} client that is scoped to the provided API key.
     *
     * @param apiKey OpenAI API key
     * @return configured client instance
     */
    public OpenAIClient createClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiIntegrationException("OpenAI APIキーが設定されていません。");
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey.trim())
                .build();
    }

}