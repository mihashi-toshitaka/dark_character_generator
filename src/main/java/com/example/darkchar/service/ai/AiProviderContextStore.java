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

    public void setActiveProviderType(ProviderType providerType) {
        activeProviderType.set(providerType == null ? ProviderType.OPENAI : providerType);
    }

    public ProviderType getActiveProviderType() {
        ProviderType type = activeProviderType.get();
        return type == null ? ProviderType.OPENAI : type;
    }

    public Optional<String> getApiKey(ProviderType type) {
        return getHolder(type).apiKey();
    }

    public void setApiKey(ProviderType type, String apiKey) {
        getHolder(type).setApiKey(apiKey);
    }

    public boolean hasApiKey(ProviderType type) {
        return getHolder(type).hasApiKey();
    }

    public Optional<String> getSelectedModel(ProviderType type) {
        return getHolder(type).selectedModel();
    }

    public void setSelectedModel(ProviderType type, String modelId) {
        getHolder(type).setSelectedModel(modelId);
    }

    public List<String> getAvailableModels(ProviderType type) {
        return getHolder(type).availableModels();
    }

    public void setAvailableModels(ProviderType type, List<String> models) {
        getHolder(type).setAvailableModels(models);
    }

    public AiProviderContext getContext(ProviderType type) {
        ProviderType actualType = type == null ? ProviderType.OPENAI : type;
        ProviderContextHolder holder = getHolder(actualType);
        return holder.snapshot(actualType);
    }

    public void clear(ProviderType type) {
        getHolder(type).clear();
    }

    private ProviderContextHolder getHolder(ProviderType type) {
        ProviderType actualType = type == null ? ProviderType.OPENAI : type;
        return contexts.computeIfAbsent(actualType, ignored -> new ProviderContextHolder());
    }

    private static final class ProviderContextHolder {

        private final AtomicReference<String> apiKeyRef = new AtomicReference<>();
        private final AtomicReference<String> selectedModelRef = new AtomicReference<>();
        private final AtomicReference<List<String>> availableModelsRef = new AtomicReference<>(List.of());

        Optional<String> apiKey() {
            return Optional.ofNullable(apiKeyRef.get());
        }

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

        boolean hasApiKey() {
            return apiKeyRef.get() != null;
        }

        Optional<String> selectedModel() {
            return Optional.ofNullable(selectedModelRef.get());
        }

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

        List<String> availableModels() {
            List<String> models = availableModelsRef.get();
            return models == null ? List.of() : List.copyOf(models);
        }

        void setAvailableModels(List<String> models) {
            if (models == null || models.isEmpty()) {
                availableModelsRef.set(List.of());
            } else {
                availableModelsRef.set(List.copyOf(models));
            }
        }

        void clear() {
            apiKeyRef.set(null);
            selectedModelRef.set(null);
            availableModelsRef.set(List.of());
        }

        AiProviderContext snapshot(ProviderType type) {
            return new AiProviderContext(type, apiKey(), selectedModel(), availableModels());
        }
    }
}
