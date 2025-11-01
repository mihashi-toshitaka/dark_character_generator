package com.example.darkchar.domain;

/**
 * 世界観ジャンルを表すレコードです。
 */
public record WorldGenre(Long id, String name) {
    /**
     * コンボボックス表示用に名称を返します。
     *
     * @return ジャンル名
     */
    @Override
    public String toString() {
        return name;
    }
}
