package com.example.darkchar.domain;

import java.util.List;
import java.util.Map;

public record DarknessSelection(
        Map<AttributeCategory, List<AttributeOption>> selections,
        int darknessLevel) {
}
