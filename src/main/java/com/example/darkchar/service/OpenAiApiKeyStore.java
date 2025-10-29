package com.example.darkchar.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * アプリケーション起動中のみメモリ上でOpenAI APIキーを保持するための簡易ストア。
 */
@Component
public class OpenAiApiKeyStore {

    private final AtomicReference<String> apiKeyRef = new AtomicReference<>();

    /**
     * APIキーを保存します。空文字またはnullの場合はクリアします。
     *
     * @param apiKey APIキー
     */
    public void setApiKey(String apiKey) {
        if (apiKey == null) {
            clear();
            return;
        }
        String normalized = apiKey.trim();
        if (normalized.isEmpty()) {
            clear();
        } else {
            apiKeyRef.set(normalized);
        }
    }

    /**
     * APIキーを取得します。
     *
     * @return APIキー（存在しない場合は空）
     */
    public Optional<String> getApiKey() {
        return Optional.ofNullable(apiKeyRef.get());
    }

    /**
     * APIキーを保持しているかどうかを返します。
     */
    public boolean hasApiKey() {
        return apiKeyRef.get() != null;
    }

    /**
     * APIキーをクリアします。
     */
    public void clear() {
        apiKeyRef.set(null);
    }
}
