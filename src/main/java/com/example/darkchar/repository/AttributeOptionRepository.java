package com.example.darkchar.repository;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.WorldGenre;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AttributeOptionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttributeOptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorldGenre> findAllWorldGenres() {
        return jdbcTemplate.query("SELECT id, name FROM world_genre ORDER BY id", new WorldGenreRowMapper());
    }

    public List<AttributeOption> findByCategory(AttributeCategory category) {
        return jdbcTemplate.query(
                "SELECT id, category, name, description FROM attribute_option WHERE category = ? ORDER BY id",
                new AttributeOptionRowMapper(),
                category.name());
    }

    public Map<AttributeCategory, List<AttributeOption>> findAllGroupedByCategory() {
        List<AttributeOption> options = jdbcTemplate.query(
                "SELECT id, category, name, description FROM attribute_option ORDER BY category, id",
                new AttributeOptionRowMapper());
        Map<AttributeCategory, List<AttributeOption>> grouped = new EnumMap<>(AttributeCategory.class);
        grouped.putAll(options.stream().collect(Collectors.groupingBy(AttributeOption::category, LinkedHashMap::new, Collectors.toList())));
        return grouped;
    }

    private static class WorldGenreRowMapper implements RowMapper<WorldGenre> {
        @Override
        public WorldGenre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new WorldGenre(rs.getLong("id"), rs.getString("name"));
        }
    }

    private static class AttributeOptionRowMapper implements RowMapper<AttributeOption> {
        @Override
        public AttributeOption mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AttributeOption(
                    rs.getLong("id"),
                    AttributeCategory.fromCode(rs.getString("category")),
                    rs.getString("name"),
                    rs.getString("description"));
        }
    }
}
