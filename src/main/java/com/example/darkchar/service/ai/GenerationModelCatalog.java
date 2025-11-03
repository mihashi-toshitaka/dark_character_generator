package com.example.darkchar.service.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.example.darkchar.service.openai.OpenAiGenerationModel;

/**
 * プロバイダごとの利用可能な生成モデルを管理するカタログ。
 */
@Component
public class GenerationModelCatalog {

    private final Map<ProviderType, Supplier<List<String>>> modelResolvers;
    private final Set<ProviderType> providersRequiringSelection;

    public GenerationModelCatalog() {
        EnumMap<ProviderType, Supplier<List<String>>> resolverMap = new EnumMap<>(ProviderType.class);
        resolverMap.put(ProviderType.OPENAI, OpenAiGenerationModel::ids);
        this.modelResolvers = Collections.unmodifiableMap(resolverMap);
        this.providersRequiringSelection = Set.of(ProviderType.OPENAI);
    }

    /**
     * 指定されたプロバイダで使用可能なモデル ID の一覧を返します。
     *
     * @param providerType プロバイダ種別
     * @return モデル ID の一覧（該当しない場合は空リスト）
     */
    public List<String> listModels(ProviderType providerType) {
        Objects.requireNonNull(providerType, "providerType");
        Supplier<List<String>> resolver = modelResolvers.get(providerType);
        if (resolver == null) {
            return List.of();
        }
        return List.copyOf(resolver.get());
    }

    /**
     * モデル選択が必須かどうかを判定します。
     *
     * @param providerType プロバイダ種別
     * @return モデル選択が必須な場合は {@code true}
     */
    public boolean requiresModelSelection(ProviderType providerType) {
        Objects.requireNonNull(providerType, "providerType");
        return providersRequiringSelection.contains(providerType);
    }
}
