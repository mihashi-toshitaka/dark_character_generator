package com.example.darkchar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darkchar.domain.AttributeCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AttributeQueryServiceTest {

    @Autowired
    private AttributeQueryService attributeQueryService;

    @Test
    void loadWorldGenresShouldReturnSeedData() {
        assertThat(attributeQueryService.loadWorldGenres()).isNotEmpty();
    }

    @Test
    void loadDarknessOptionsShouldIncludeAllCategories() {
        assertThat(attributeQueryService.loadDarknessOptions())
                .containsKeys(AttributeCategory.MOTIVE, AttributeCategory.TRANSFORMATION_PROCESS,
                        AttributeCategory.MINDSET, AttributeCategory.APPEARANCE);
    }
}
