package com.example.darkchar.service.ai;

/**
 * 利用可能なAIプロバイダの種類を表す列挙。
 */
public enum ProviderType {

    OPENAI("OpenAI"),
    LOCAL("ローカル");

    private final String displayName;

    /**
     * 表示名を指定して列挙を初期化します。
     *
     * @param displayName 表示名
     */
    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * UI で利用する表示名を返します。
     *
     * @return 表示名
     */
    public String getDisplayName() {
        return displayName;
    }
}
