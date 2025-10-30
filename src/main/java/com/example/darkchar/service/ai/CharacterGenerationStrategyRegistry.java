package com.example.darkchar.service.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 登録されたAIプロバイダを解決するレジストリ。
 */
@Component
public class CharacterGenerationStrategyRegistry {

    private final Map<ProviderType, CharacterGenerationProvider> providers;

    public CharacterGenerationStrategyRegistry(List<CharacterGenerationProvider> providers) {
        Map<ProviderType, CharacterGenerationProvider> map = new EnumMap<>(ProviderType.class);
        for (CharacterGenerationProvider provider : providers) {
            map.put(provider.getProviderType(), provider);
        }
        this.providers = Collections.unmodifiableMap(map);
    }

    public Optional<CharacterGenerationProvider> findProvider(ProviderType type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(type));
    }

    public List<ProviderType> getRegisteredProviderTypes() {
        return List.copyOf(providers.keySet());
    }
}
