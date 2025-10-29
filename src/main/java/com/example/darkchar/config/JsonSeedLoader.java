package com.example.darkchar.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonSeedLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(JsonSeedLoader.class);
    private static final String WORLD_GENRE_RESOURCE_PATH = "data/world-genres.json";
    private static final List<String> ATTRIBUTE_OPTION_RESOURCE_PATHS = List.of(
            "data/attribute-options/character-trait.json",
            "data/attribute-options/motive.json",
            "data/attribute-options/transformation-process.json",
            "data/attribute-options/mindset.json",
            "data/attribute-options/appearance.json"
    );

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public JsonSeedLoader(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long worldGenreCount = countRows("world_genre");
        long attributeOptionCount = countRows("attribute_option");

        if (worldGenreCount > 0 && attributeOptionCount > 0) {
            logger.debug("Skipping JSON seed loading because tables already contain data.");
            return;
        }

        if (worldGenreCount == 0) {
            List<WorldGenreSeed> worldGenres = loadWorldGenres();
            if (!worldGenres.isEmpty()) {
                insertWorldGenres(worldGenres);
            } else {
                logger.warn("No world genre seed data available at {}", WORLD_GENRE_RESOURCE_PATH);
            }
        }

        if (attributeOptionCount == 0) {
            List<AttributeOptionSeed> attributeOptions = loadAttributeOptions();
            if (!attributeOptions.isEmpty()) {
                insertAttributeOptions(attributeOptions);
            } else {
                logger.warn("No attribute option seed data available under {}", ATTRIBUTE_OPTION_RESOURCE_PATHS);
            }
        }
    }

    private List<WorldGenreSeed> loadWorldGenres() {
        return readSeedList(WORLD_GENRE_RESOURCE_PATH, new TypeReference<>() {});
    }

    private List<AttributeOptionSeed> loadAttributeOptions() {
        List<AttributeOptionSeed> combined = new ArrayList<>();
        for (String resourcePath : ATTRIBUTE_OPTION_RESOURCE_PATHS) {
            List<AttributeOptionSeed> options = readSeedList(resourcePath, new TypeReference<>() {});
            if (!options.isEmpty()) {
                combined.addAll(options);
            }
        }
        return combined;
    }

    private <T> List<T> readSeedList(String resourcePath, TypeReference<List<T>> typeReference) {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            logger.warn("Seed resource {} not found on classpath.", resourcePath);
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<T> seedList = objectMapper.readValue(inputStream, typeReference);
            return seedList != null ? seedList : List.of();
        } catch (IOException ex) {
            logger.error("Failed to read seed data from {}", resourcePath, ex);
            return List.of();
        }
    }

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

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }
}
