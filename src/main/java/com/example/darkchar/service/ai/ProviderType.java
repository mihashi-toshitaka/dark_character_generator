package com.example.darkchar.service.ai;

/**
 * 利用可能なAIプロバイダの種類を表す列挙。
 */
public enum ProviderType {

    OPENAI("OpenAI"),
    LOCAL("ローカル");

    private final String displayName;

    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
