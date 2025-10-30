package com.example.darkchar.service.ai;

import java.util.Optional;

/**
 * プロバイダが利用可能かどうかの評価結果を表すレコード。
 */
public record ProviderConfigurationStatus(boolean ready, Optional<String> warningMessage) {

    public ProviderConfigurationStatus {
        warningMessage = warningMessage == null ? Optional.empty() : warningMessage;
    }

    public static ProviderConfigurationStatus ready() {
        return new ProviderConfigurationStatus(true, Optional.empty());
    }

    public static ProviderConfigurationStatus notReady() {
        return new ProviderConfigurationStatus(false, Optional.empty());
    }

    public static ProviderConfigurationStatus notReadyWithWarning(String warning) {
        return new ProviderConfigurationStatus(false, Optional.ofNullable(warning));
    }
}
