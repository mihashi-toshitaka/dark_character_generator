package com.example.darkchar.service.ai;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * プロバイダごとのAPIキーやモデル選択を保持するメモリ内ストア。
 */
@Component
public class AiProviderContextStore {

    private final ConcurrentMap<ProviderType, ProviderContextHolder> contexts = new ConcurrentHashMap<>();
    private final AtomicReference<ProviderType> activeProviderType = new AtomicReference<>(ProviderType.OPENAI);

    /**
     * 現在のアクティブプロバイダを更新します。
     *
     * @param providerType 新しいプロバイダ
     */
    public void setActiveProviderType(ProviderType providerType) {
        activeProviderType.set(providerType == null ? ProviderType.OPENAI : providerType);
    }

    /**
     * アクティブなプロバイダ種別を取得します。
     *
     * @return プロバイダ種別
     */
    public ProviderType getActiveProviderType() {
        ProviderType type = activeProviderType.get();
        return type == null ? ProviderType.OPENAI : type;
    }

    /**
     * APIキーを取得します。
     *
     * @param type プロバイダ種別
     * @return APIキー
     */
    public Optional<String> getApiKey(ProviderType type) {
        return getHolder(type).apiKey();
    }

    /**
     * APIキーを保存します。
     *
     * @param type   プロバイダ種別
     * @param apiKey APIキー
     */
    public void setApiKey(ProviderType type, String apiKey) {
        getHolder(type).setApiKey(apiKey);
    }

    /**
     * APIキーが設定されているか確認します。
     *
     * @param type プロバイダ種別
     * @return 設定済みなら true
     */
    public boolean hasApiKey(ProviderType type) {
        return getHolder(type).hasApiKey();
    }

    /**
     * 選択済みモデルを取得します。
     *
     * @param type プロバイダ種別
     * @return モデルID
     */
    public Optional<String> getSelectedModel(ProviderType type) {
        return getHolder(type).selectedModel();
    }

    /**
     * 選択モデルを保存します。
     *
     * @param type    プロバイダ種別
     * @param modelId モデルID
     */
    public void setSelectedModel(ProviderType type, String modelId) {
        getHolder(type).setSelectedModel(modelId);
    }

    /**
     * 利用可能モデル一覧を取得します。
     *
     * @param type プロバイダ種別
     * @return モデルID一覧
     */
    public List<String> getAvailableModels(ProviderType type) {
        return getHolder(type).availableModels();
    }

    /**
     * 利用可能モデル一覧を保存します。
     *
     * @param type   プロバイダ種別
     * @param models モデルID一覧
     */
    public void setAvailableModels(ProviderType type, List<String> models) {
        getHolder(type).setAvailableModels(models);
    }

    /**
     * 現在の設定をまとめて取得します。
     *
     * @param type プロバイダ種別
     * @return コンテキスト
     */
    public AiProviderContext getContext(ProviderType type) {
        ProviderType actualType = type == null ? ProviderType.OPENAI : type;
        ProviderContextHolder holder = getHolder(actualType);
        return holder.snapshot(actualType);
    }

    /**
     * 保存済み設定をクリアします。
     *
     * @param type プロバイダ種別
     */
    public void clear(ProviderType type) {
        getHolder(type).clear();
    }

    /**
     * 指定プロバイダのホルダーを取得します。
     *
     * @param type プロバイダ種別
     * @return ホルダー
     */
    private ProviderContextHolder getHolder(ProviderType type) {
        ProviderType actualType = type == null ? ProviderType.OPENAI : type;
        return contexts.computeIfAbsent(actualType, ignored -> new ProviderContextHolder());
    }

    /**
     * プロバイダ別の設定値を保持するヘルパーです。
     */
    private static final class ProviderContextHolder {

        private final AtomicReference<String> apiKeyRef = new AtomicReference<>();
        private final AtomicReference<String> selectedModelRef = new AtomicReference<>();
        private final AtomicReference<List<String>> availableModelsRef = new AtomicReference<>(List.of());

        /**
         * APIキーを返します。
         *
         * @return APIキー
         */
        Optional<String> apiKey() {
            return Optional.ofNullable(apiKeyRef.get());
        }

        /**
         * APIキーを設定します。
         *
         * @param apiKey APIキー
         */
        void setApiKey(String apiKey) {
            if (apiKey == null) {
                apiKeyRef.set(null);
                selectedModelRef.set(null);
                availableModelsRef.set(List.of());
                return;
            }
            String normalized = apiKey.trim();
            if (normalized.isEmpty()) {
                apiKeyRef.set(null);
                selectedModelRef.set(null);
                availableModelsRef.set(List.of());
            } else {
                apiKeyRef.set(normalized);
            }
        }

        /**
         * APIキーが登録済みか確認します。
         *
         * @return 設定済みなら true
         */
        boolean hasApiKey() {
            return apiKeyRef.get() != null;
        }

        /**
         * 選択済みモデルを返します。
         *
         * @return モデルID
         */
        Optional<String> selectedModel() {
            return Optional.ofNullable(selectedModelRef.get());
        }

        /**
         * 選択モデルを設定します。
         *
         * @param modelId モデルID
         */
        void setSelectedModel(String modelId) {
            if (modelId == null) {
                selectedModelRef.set(null);
                return;
            }
            String normalized = modelId.trim();
            if (normalized.isEmpty()) {
                selectedModelRef.set(null);
            } else {
                selectedModelRef.set(normalized);
            }
        }

        /**
         * 利用可能モデル一覧を返します。
         *
         * @return モデルID一覧
         */
        List<String> availableModels() {
            List<String> models = availableModelsRef.get();
            return models == null ? List.of() : List.copyOf(models);
        }

        /**
         * 利用可能モデル一覧を設定します。
         *
         * @param models モデルID一覧
         */
        void setAvailableModels(List<String> models) {
            if (models == null || models.isEmpty()) {
                availableModelsRef.set(List.of());
            } else {
                availableModelsRef.set(List.copyOf(models));
            }
        }

        /**
         * すべての情報を初期化します。
         */
        void clear() {
            apiKeyRef.set(null);
            selectedModelRef.set(null);
            availableModelsRef.set(List.of());
        }

        /**
         * 現在の情報を {@link AiProviderContext} に変換します。
         *
         * @param type プロバイダ種別
         * @return コンテキスト
         */
        AiProviderContext snapshot(ProviderType type) {
            return new AiProviderContext(type, apiKey(), selectedModel(), availableModels());
        }
    }
}
