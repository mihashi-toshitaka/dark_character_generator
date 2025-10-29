package com.example.darkchar.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * OpenAIの利用可能なモデル一覧と選択済みモデルをメモリ上で保持するストア。
 */
@Component
public class OpenAiModelStore {

    private final AtomicReference<List<String>> availableModelsRef = new AtomicReference<>(List.of());
    private final AtomicReference<String> selectedModelRef = new AtomicReference<>();

    /**
     * 取得済みのモデル一覧を設定します。nullの場合は空に変換します。
     */
    public void setAvailableModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            availableModelsRef.set(List.of());
        } else {
            availableModelsRef.set(List.copyOf(models));
        }
    }

    /**
     * 取得済みのモデル一覧を返します。
     */
    public List<String> getAvailableModels() {
        List<String> models = availableModelsRef.get();
        return models == null ? List.of() : Collections.unmodifiableList(models);
    }

    /**
     * 選択済みのモデルIDを設定します。nullや空文字の場合はクリアされます。
     */
    public void setSelectedModel(String modelId) {
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
     * 選択済みモデルIDを返します。
     */
    public Optional<String> getSelectedModel() {
        return Optional.ofNullable(selectedModelRef.get());
    }

    /**
     * 保持している情報をすべてクリアします。
     */
    public void clear() {
        availableModelsRef.set(List.of());
        selectedModelRef.set(null);
    }
}
