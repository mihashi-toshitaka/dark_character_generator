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

/**
 * 属性マスタを取得するリポジトリです。
 */
@Repository
public class AttributeOptionRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * JDBC テンプレートを注入します。
     *
     * @param jdbcTemplate JDBC テンプレート
     */
    public AttributeOptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 世界観ジャンルをすべて取得します。
     *
     * @return 世界観ジャンル一覧
     */
    public List<WorldGenre> findAllWorldGenres() {
        return jdbcTemplate.query("SELECT id, name FROM world_genre ORDER BY id", new WorldGenreRowMapper());
    }

    /**
     * 指定カテゴリの属性を取得します。
     *
     * @param category 対象カテゴリ
     * @return 属性リスト
     */
    public List<AttributeOption> findByCategory(AttributeCategory category) {
        return jdbcTemplate.query(
                "SELECT id, category, name, description FROM attribute_option WHERE category = ? ORDER BY id",
                new AttributeOptionRowMapper(),
                category.name());
    }

    /**
     * カテゴリ別に属性をまとめて取得します。
     *
     * @return カテゴリごとの属性マップ
     */
    public Map<AttributeCategory, List<AttributeOption>> findAllGroupedByCategory() {
        List<AttributeOption> options = jdbcTemplate.query(
                "SELECT id, category, name, description FROM attribute_option ORDER BY category, id",
                new AttributeOptionRowMapper());
        Map<AttributeCategory, List<AttributeOption>> grouped = new EnumMap<>(AttributeCategory.class);
        grouped.putAll(options.stream().collect(Collectors.groupingBy(AttributeOption::category, LinkedHashMap::new, Collectors.toList())));
        return grouped;
    }

    /**
     * world_genre テーブルをレコードに変換します。
     */
    private static class WorldGenreRowMapper implements RowMapper<WorldGenre> {
        /**
         * 1 行分を {@link WorldGenre} に変換します。
         *
         * @param rs     結果セット
         * @param rowNum 行番号
         * @return 変換結果
         * @throws SQLException SQL エラー
         */
        @Override
        public WorldGenre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new WorldGenre(rs.getLong("id"), rs.getString("name"));
        }
    }

    /**
     * attribute_option テーブルをレコードに変換します。
     */
    private static class AttributeOptionRowMapper implements RowMapper<AttributeOption> {
        /**
         * 1 行分を {@link AttributeOption} に変換します。
         *
         * @param rs     結果セット
         * @param rowNum 行番号
         * @return 変換結果
         * @throws SQLException SQL エラー
         */
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
