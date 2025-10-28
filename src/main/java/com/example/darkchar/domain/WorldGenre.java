package com.example.darkchar.domain;

public record WorldGenre(Long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
