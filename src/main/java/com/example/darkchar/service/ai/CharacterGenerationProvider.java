package com.example.darkchar.service.ai;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;

/**
 * 各種AIプロバイダが実装するキャラクター生成用インターフェース。
 */
public interface CharacterGenerationProvider {

    /**
     * プロバイダ種別を返します。
     *
     * @return プロバイダ種別
     */
    ProviderType getProviderType();

    /**
     * UI に表示する名称を返します。
     *
     * @return 表示名
     */
    String getDisplayName();

    /**
     * 設定が利用可能かを評価します。
     *
     * @param context プロバイダ設定
     * @return 評価結果
     */
    ProviderConfigurationStatus assessConfiguration(AiProviderContext context);

    /**
     * キャラクター生成を実行します。
     *
     * @param context   プロバイダ設定
     * @param input     入力情報
     * @param selection 闇堕ち選択
     * @return 生成結果
     */
    ProviderGenerationResult generate(AiProviderContext context, CharacterInput input, DarknessSelection selection);

    /**
     * 連携失敗時に表示する警告文を構築します。
     *
     * @param throwable 発生した例外
     * @return 警告メッセージ
     */
    default String buildFailureWarning(Throwable throwable) {
        String detail = throwable == null ? null : throwable.getMessage();
        String base = getDisplayName() + "連携に失敗したため、サンプル結果を表示しています。";
        if (detail == null || detail.isBlank()) {
            return base;
        }
        return base + "詳細: " + detail;
    }
}
