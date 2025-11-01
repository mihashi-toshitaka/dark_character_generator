package com.example.darkchar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darkchar.domain.AttributeCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link AttributeQueryService} のマスタ取得を検証します。
 */
@SpringBootTest
class AttributeQueryServiceTest {

    @Autowired
    private AttributeQueryService attributeQueryService;

    /**
     * 世界観ジャンルのシードが読み込まれることを確認します。
     */
    @Test
    void loadWorldGenresShouldReturnSeedData() {
        assertThat(attributeQueryService.loadWorldGenres()).isNotEmpty();
    }

    /**
     * 闇堕ちカテゴリが期待通りに含まれることを確認します。
     */
    @Test
    void loadDarknessOptionsShouldIncludeAllCategories() {
        assertThat(attributeQueryService.loadDarknessOptions())
                .containsKeys(AttributeCategory.MOTIVE, AttributeCategory.TRANSFORMATION_PROCESS,
                        AttributeCategory.MINDSET, AttributeCategory.APPEARANCE);
    }
}
