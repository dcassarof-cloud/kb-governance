package br.com.consisa.gov.kb.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * 📅 Parser especializado de datas do Movidesk
 *
 * PROBLEMA RESOLVIDO:
 * -------------------
 * A API do Movidesk retorna datas em formato inconsistente:
 * - Às vezes com timezone (2024-01-19T10:30:00-03:00)
 * - Às vezes sem timezone (2024-01-19T10:30:00)
 * - Às vezes com Z (2024-01-19T10:30:00Z)
 * - Às vezes com frações de segundo
 *
 * ESTRATÉGIA:
 * -----------
 * - Tenta parsear com offset primeiro
 * - Se não tiver offset, assume UTC
 * - Suporta frações de segundo opcionais
 *
 * TESTES IMPORTANTES:
 * -------------------
 * - "2024-01-19T10:30:00"           -> OK (sem offset = UTC)
 * - "2024-01-19T10:30:00Z"          -> OK (Z = UTC)
 * - "2024-01-19T10:30:00-03:00"     -> OK (com offset)
 * - "2024-01-19T10:30:00.123"       -> OK (com millis)
 * - "2024-01-19T10:30:00.123456789" -> OK (com nanos)
 */
@Service
public class MovideskDateParser {

    /**
     * Formatter flexível que aceita:
     * - Datas com ou sem frações de segundo
     * - Datas com ou sem timezone
     */
    private static final DateTimeFormatter MOVIDESK_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            // frações de segundo (opcional)
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            // offset/timezone (opcional)
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter();

    /**
     * Converte string da API do Movidesk para OffsetDateTime.
     *
     * @param dateString string no formato ISO-8601 (com ou sem offset)
     * @return OffsetDateTime em UTC ou null se vazio
     *
     * @throws DateTimeParseException se formato inválido
     */
    public OffsetDateTime parse(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }

        // detecta se tem offset explícito
        boolean hasOffset = hasExplicitOffset(dateString);

        if (!hasOffset) {
            // sem offset → parseia como LocalDateTime e assume UTC
            LocalDateTime ldt = LocalDateTime.parse(dateString, MOVIDESK_FORMATTER);
            return ldt.atOffset(ZoneOffset.UTC);
        }

        // com offset → parseia direto
        return OffsetDateTime.parse(dateString, MOVIDESK_FORMATTER);
    }

    /**
     * Detecta se a string tem offset explícito.
     *
     * Padrões reconhecidos:
     * - termina com Z (ex: 2024-01-19T10:30:00Z)
     * - contém + seguido de dígitos (ex: 2024-01-19T10:30:00+03:00)
     * - termina com -HH:MM (ex: 2024-01-19T10:30:00-03:00)
     */
    private boolean hasExplicitOffset(String dateString) {
        return dateString.endsWith("Z")
                || dateString.contains("+")
                || dateString.matches(".*-\\d\\d:\\d\\d$");
    }
}
