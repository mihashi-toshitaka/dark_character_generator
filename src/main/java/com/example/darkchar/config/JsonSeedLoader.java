package com.example.darkchar.config;

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
import java.util.List;

@Component
public class JsonSeedLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(JsonSeedLoader.class);
    private static final String SEED_RESOURCE_PATH = "data/initial-seed.json";

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

        SeedData seedData = readSeedData();
        if (seedData == null) {
            logger.warn("Seed data could not be loaded from {}", SEED_RESOURCE_PATH);
            return;
        }

        if (worldGenreCount == 0 && seedData.worldGenres() != null) {
            insertWorldGenres(seedData.worldGenres());
        }

        if (attributeOptionCount == 0 && seedData.attributeOptions() != null) {
            insertAttributeOptions(seedData.attributeOptions());
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

    private SeedData readSeedData() {
        Resource resource = new ClassPathResource(SEED_RESOURCE_PATH);
        if (!resource.exists()) {
            logger.warn("Seed resource {} not found on classpath.", SEED_RESOURCE_PATH);
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, SeedData.class);
        } catch (IOException ex) {
            logger.error("Failed to read seed data from {}", SEED_RESOURCE_PATH, ex);
            return null;
        }
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }
}
