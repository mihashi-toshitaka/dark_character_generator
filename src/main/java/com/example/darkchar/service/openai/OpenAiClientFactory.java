package com.example.darkchar.service.openai;

import org.springframework.stereotype.Component;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * APIキーから OpenAI クライアントを生成するファクトリです。
 */
@Component
public class OpenAiClientFactory {

    /**
     * 指定された API キーで利用できるクライアントを生成します。
     *
     * @param apiKey OpenAI の API キー
     * @return 設定済みクライアント
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
