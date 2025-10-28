package com.example.darkchar.service;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.repository.AttributeOptionRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AttributeQueryService {

    private final AttributeOptionRepository repository;

    public AttributeQueryService(AttributeOptionRepository repository) {
        this.repository = repository;
    }

    public List<WorldGenre> loadWorldGenres() {
        return repository.findAllWorldGenres();
    }

    public List<AttributeOption> loadCharacterTraits() {
        return repository.findByCategory(AttributeCategory.CHARACTER_TRAIT);
    }

    public Map<AttributeCategory, List<AttributeOption>> loadDarknessOptions() {
        Map<AttributeCategory, List<AttributeOption>> grouped = repository.findAllGroupedByCategory();
        return grouped.entrySet().stream()
                .filter(entry -> entry.getKey() != AttributeCategory.CHARACTER_TRAIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new EnumMap<>(AttributeCategory.class)));
    }
}
