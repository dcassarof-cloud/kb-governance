package br.com.consisa.gov.kb.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 📅 Utilitário centralizado para conversão de datas.
 *
 * PADRÃO DO PROJETO (Sprint 2):
 * - Banco de dados: TIMESTAMPTZ (PostgreSQL)
 * - Entity/Repository: Instant ou OffsetDateTime
 * - API DTO: OffsetDateTime (UTC) → formato ISO-8601
 *
 * REGRAS:
 * - Toda data é tratada em UTC
 * - Conversão sempre null-safe
 * - Fallback para now() quando necessário
 */
public final class DateTimeUtils {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private DateTimeUtils() {
        // Utility class
    }

    /**
     * Converte Instant para OffsetDateTime (UTC).
     * Null-safe: retorna now() se input for null.
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null
                ? instant.atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Converte Instant para OffsetDateTime (UTC).
     * Null-safe: retorna null se input for null.
     */
    public static OffsetDateTime toOffsetDateTimeOrNull(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    /**
     * Converte OffsetDateTime para Instant.
     * Null-safe: retorna now() se input for null.
     */
    public static Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null
                ? offsetDateTime.toInstant()
                : Instant.now();
    }

    /**
     * Converte OffsetDateTime para Instant.
     * Null-safe: retorna null se input for null.
     */
    public static Instant toInstantOrNull(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    /**
     * Retorna OffsetDateTime atual em UTC.
     */
    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Retorna OffsetDateTime atual em America/Sao_Paulo.
     */
    public static OffsetDateTime nowSaoPaulo() {
        return OffsetDateTime.now(SAO_PAULO);
    }

    /**
     * Retorna Instant atual.
     */
    public static Instant nowInstant() {
        return Instant.now();
    }
}
