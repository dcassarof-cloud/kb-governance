package br.com.consisa.gov.kb.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SyncModeConverter implements AttributeConverter<SyncMode, String> {

    @Override
    public String convertToDatabaseColumn(SyncMode attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public SyncMode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase();
        return switch (normalized) {
            case "FULL" -> SyncMode.FULL;
            case "DELTA", "DELTA_WINDOW", "INCREMENTAL" -> SyncMode.DELTA;
            default -> throw new IllegalArgumentException("Modo inválido: " + dbData);
        };
    }
}
