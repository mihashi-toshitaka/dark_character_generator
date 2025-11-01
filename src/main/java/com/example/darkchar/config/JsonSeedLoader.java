package com.example.darkchar.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JSON ファイルから初期データを読み込みます。
 */
@Component
public class JsonSeedLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(JsonSeedLoader.class);
    private static final String WORLD_GENRES_RESOURCE_PATH = "data/world-genres.json";
    private static final String ATTRIBUTE_OPTIONS_RESOURCE_PATTERN = "classpath:data/attribute-options/*.json";

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ResourcePatternResolver resourcePatternResolver;

    /**
     * 依存コンポーネントを注入します。
     *
     * @param objectMapper JSON マッパー
     * @param jdbcTemplate JDBC テンプレート
     */
    public JsonSeedLoader(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * アプリ起動時にデータを投入します。
     *
     * @param args 実行引数
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        long worldGenreCount = countRows("world_genre");
        long attributeOptionCount = countRows("attribute_option");

        if (worldGenreCount > 0 && attributeOptionCount > 0) {
            logger.debug("Skipping JSON seed loading because tables already contain data.");
            return;
        }

        if (worldGenreCount == 0) {
            List<WorldGenreSeed> worldGenres = readWorldGenres();
            if (!worldGenres.isEmpty()) {
                insertWorldGenres(worldGenres);
            }
        }

        if (attributeOptionCount == 0) {
            List<AttributeOptionSeed> attributeOptions = readAttributeOptions();
            if (!attributeOptions.isEmpty()) {
                insertAttributeOptions(attributeOptions);
            }
        }
    }

    /**
     * 世界観ジャンルを登録します。
     *
     * @param worldGenres シードデータ
     */
    private void insertWorldGenres(List<WorldGenreSeed> worldGenres) {
        if (worldGenres.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO world_genre (name) VALUES (?)",
                worldGenres.stream()
                        .map(genre -> new Object[]{genre.name()})
                        .toList()
        );
        logger.info("Inserted {} world genres from JSON seed.", worldGenres.size());
    }

    /**
     * 属性データを登録します。
     *
     * @param attributeOptions シードデータ
     */
    private void insertAttributeOptions(List<AttributeOptionSeed> attributeOptions) {
        if (attributeOptions.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO attribute_option (category, name, description) VALUES (?, ?, ?)",
                attributeOptions.stream()
                        .map(option -> new Object[]{option.category(), option.name(), option.description()})
                        .toList()
        );
        logger.info("Inserted {} attribute options from JSON seed.", attributeOptions.size());
    }

    /**
     * 世界観ジャンルのシードを読み込みます。
     *
     * @return シードデータ
     */
    private List<WorldGenreSeed> readWorldGenres() {
        Resource resource = new ClassPathResource(WORLD_GENRES_RESOURCE_PATH);
        if (!resource.exists()) {
            logger.warn("World genre seed resource {} not found on classpath.", WORLD_GENRES_RESOURCE_PATH);
            return Collections.emptyList();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<WorldGenreSeed>>() {});
        } catch (IOException ex) {
            logger.error("Failed to read world genre seed data from {}", WORLD_GENRES_RESOURCE_PATH, ex);
            return Collections.emptyList();
        }
    }

    /**
     * 属性シードをすべて読み込みます。
     *
     * @return シードデータ
     */
    private List<AttributeOptionSeed> readAttributeOptions() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(ATTRIBUTE_OPTIONS_RESOURCE_PATTERN);
            if (resources.length == 0) {
                logger.warn("No attribute option seed resources found using pattern {}", ATTRIBUTE_OPTIONS_RESOURCE_PATTERN);
                return Collections.emptyList();
            }

            List<AttributeOptionSeed> allOptions = new ArrayList<>();
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    List<AttributeOptionSeed> options = objectMapper.readValue(inputStream, new TypeReference<List<AttributeOptionSeed>>() {});
                    allOptions.addAll(options);
                } catch (IOException ex) {
                    logger.error("Failed to read attribute option seed data from {}", resource.getFilename(), ex);
                }
            }
            return allOptions;
        } catch (IOException ex) {
            logger.error("Failed to resolve attribute option seed resources using pattern {}", ATTRIBUTE_OPTIONS_RESOURCE_PATTERN, ex);
            return Collections.emptyList();
        }
    }

    /**
     * テーブル内の件数を取得します。
     *
     * @param tableName テーブル名
     * @return 件数
     */
    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }
}
