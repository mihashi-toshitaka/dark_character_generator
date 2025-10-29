package com.example.darkchar.config;

import java.util.List;

public record SeedData(
        List<WorldGenreSeed> worldGenres,
        List<AttributeOptionSeed> attributeOptions
) {
}
