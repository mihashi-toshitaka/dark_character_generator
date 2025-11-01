package com.example.darkchar.service.ai;

import java.util.Optional;

/**
 * プロバイダが利用可能かどうかの評価結果を表すレコード。
 */
public record ProviderConfigurationStatus(boolean ready, Optional<String> warningMessage) {

    /**
     * 警告メッセージを正規化します。
     */
    public ProviderConfigurationStatus {
        warningMessage = warningMessage == null ? Optional.empty() : warningMessage;
    }

    /**
     * 利用可能な状態を表します。
     *
     * @return 準備完了ステータス
     */
    public static ProviderConfigurationStatus onReady() {
        return new ProviderConfigurationStatus(true, Optional.empty());
    }

    /**
     * 利用不可状態を表します。
     *
     * @return 未準備ステータス
     */
    public static ProviderConfigurationStatus notReady() {
        return new ProviderConfigurationStatus(false, Optional.empty());
    }

    /**
     * 警告付きの利用不可状態を表します。
     *
     * @param warning 警告メッセージ
     * @return 未準備ステータス
     */
    public static ProviderConfigurationStatus notReadyWithWarning(String warning) {
        return new ProviderConfigurationStatus(false, Optional.ofNullable(warning));
    }
}
