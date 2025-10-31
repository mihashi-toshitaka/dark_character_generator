package com.example.darkchar.service.openai;

import org.springframework.stereotype.Component;

import com.openai.client.OpenAI;

/**
 * Factory responsible for building {@link OpenAI} clients with a dynamically provided API key.
 */
@Component
public class OpenAiClientFactory {

    /**
     * Creates a new {@link OpenAI} client that is scoped to the provided API key.
     *
     * @param apiKey OpenAI API key
     * @return configured client instance
     */
    public OpenAI createClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiIntegrationException("OpenAI APIキーが設定されていません。");
        }
        return OpenAI.builder()
                .apiKey(apiKey.trim())
                .build();
    }
}
