package br.com.consisa.gov.kb.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SyncMode {
    FULL,
    DELTA;

    @JsonCreator
    public static SyncMode fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FULL" -> FULL;
            case "DELTA", "DELTA_WINDOW", "INCREMENTAL" -> DELTA;
            default -> throw new IllegalArgumentException("Modo inválido: " + value);
        };
    }

    @JsonValue
    public String toJson() {
        return this == DELTA ? "DELTA" : "FULL";
    }
}
