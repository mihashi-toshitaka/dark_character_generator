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

/**
 * 画面で利用する属性マスタを提供するサービスです。
 */
@Service
public class AttributeQueryService {

    private final AttributeOptionRepository repository;

    /**
     * リポジトリを注入します。
     *
     * @param repository 属性リポジトリ
     */
    public AttributeQueryService(AttributeOptionRepository repository) {
        this.repository = repository;
    }

    /**
     * 世界観ジャンル一覧を読み込みます。
     *
     * @return 世界観ジャンル
     */
    public List<WorldGenre> loadWorldGenres() {
        return repository.findAllWorldGenres();
    }

    /**
     * キャラクター属性候補を読み込みます。
     *
     * @return キャラクター属性一覧
     */
    public List<AttributeOption> loadCharacterTraits() {
        return repository.findByCategory(AttributeCategory.CHARACTER_TRAIT);
    }

    /**
     * 闇堕ちカテゴリの選択肢を読み込みます。
     *
     * @return カテゴリ別の属性マップ
     */
    public Map<AttributeCategory, List<AttributeOption>> loadDarknessOptions() {
        Map<AttributeCategory, List<AttributeOption>> grouped = repository.findAllGroupedByCategory();
        return grouped.entrySet().stream()
                .filter(entry -> entry.getKey() != AttributeCategory.CHARACTER_TRAIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new EnumMap<>(AttributeCategory.class)));
    }
}
