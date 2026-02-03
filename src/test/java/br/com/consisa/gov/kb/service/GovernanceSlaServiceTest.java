package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para GovernanceSlaService.
 *
 * <p>Verifica:
 * <ul>
 *   <li>Prazos de SLA corretos por severidade</li>
 *   <li>Cálculo de overdue respeita status</li>
 *   <li>Tratamento de valores nulos</li>
 *   <li>Timezone/Instant não quebra cálculos</li>
 * </ul>
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
class GovernanceSlaServiceTest {

    private GovernanceSlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new GovernanceSlaService();
    }

    @Nested
    @DisplayName("getSlaDays()")
    class GetSlaDaysTests {

        @Test
        @DisplayName("ERROR deve retornar 3 dias")
        void errorSeverityReturns3Days() {
            assertEquals(3, slaService.getSlaDays(GovernanceSeverity.ERROR));
        }

        @Test
        @DisplayName("WARN deve retornar 15 dias")
        void warnSeverityReturns15Days() {
            assertEquals(15, slaService.getSlaDays(GovernanceSeverity.WARN));
        }

        @Test
        @DisplayName("INFO deve retornar 30 dias")
        void infoSeverityReturns30Days() {
            assertEquals(30, slaService.getSlaDays(GovernanceSeverity.INFO));
        }

        @Test
        @DisplayName("null deve retornar default (15 dias)")
        void nullSeverityReturnsDefault() {
            assertEquals(15, slaService.getSlaDays(null));
        }
    }

    @Nested
    @DisplayName("calculateDueAt(OffsetDateTime, Severity)")
    class CalculateDueAtOffsetDateTimeTests {

        @Test
        @DisplayName("Calcula prazo correto para ERROR (3 dias)")
        void calculatesCorrectDueForError() {
            OffsetDateTime base = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expected = base.plusDays(3);

            OffsetDateTime result = slaService.calculateDueAt(base, GovernanceSeverity.ERROR);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Calcula prazo correto para WARN (15 dias)")
        void calculatesCorrectDueForWarn() {
            OffsetDateTime base = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expected = base.plusDays(15);

            OffsetDateTime result = slaService.calculateDueAt(base, GovernanceSeverity.WARN);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Calcula prazo correto para INFO (30 dias)")
        void calculatesCorrectDueForInfo() {
            OffsetDateTime base = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expected = base.plusDays(30);

            OffsetDateTime result = slaService.calculateDueAt(base, GovernanceSeverity.INFO);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Retorna null para base null")
        void returnsNullForNullBase() {
            assertNull(slaService.calculateDueAt((OffsetDateTime) null, GovernanceSeverity.ERROR));
        }

        @Test
        @DisplayName("Retorna null para severity null")
        void returnsNullForNullSeverity() {
            OffsetDateTime base = OffsetDateTime.now();
            assertNull(slaService.calculateDueAt(base, null));
        }
    }

    @Nested
    @DisplayName("calculateDueAt(Instant, Severity)")
    class CalculateDueAtInstantTests {

        @Test
        @DisplayName("Calcula prazo correto usando Instant")
        void calculatesCorrectDueUsingInstant() {
            Instant base = Instant.parse("2026-02-01T10:00:00Z");
            Instant expected = base.plus(3, ChronoUnit.DAYS);

            Instant result = slaService.calculateDueAt(base, GovernanceSeverity.ERROR);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Preserva precisão de milissegundos")
        void preservesMillisecondPrecision() {
            Instant base = Instant.parse("2026-02-01T10:30:45.123Z");
            Instant result = slaService.calculateDueAt(base, GovernanceSeverity.WARN);

            // Deve adicionar exatamente 15 dias
            assertEquals(base.plus(15, ChronoUnit.DAYS), result);
        }
    }

    @Nested
    @DisplayName("isOverdue()")
    class IsOverdueTests {

        @Test
        @DisplayName("Retorna true quando SLA vencido e status OPEN")
        void returnsTrueWhenOverdueAndOpen() {
            Instant now = Instant.parse("2026-02-10T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-05T10:00:00Z");

            assertTrue(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.OPEN));
        }

        @Test
        @DisplayName("Retorna true quando SLA vencido e status ASSIGNED")
        void returnsTrueWhenOverdueAndAssigned() {
            Instant now = Instant.parse("2026-02-10T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-05T10:00:00Z");

            assertTrue(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.ASSIGNED));
        }

        @Test
        @DisplayName("Retorna true quando SLA vencido e status IN_PROGRESS")
        void returnsTrueWhenOverdueAndInProgress() {
            Instant now = Instant.parse("2026-02-10T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-05T10:00:00Z");

            assertTrue(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.IN_PROGRESS));
        }

        @Test
        @DisplayName("Retorna false quando SLA vencido mas status RESOLVED")
        void returnsFalseWhenOverdueButResolved() {
            Instant now = Instant.parse("2026-02-10T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-05T10:00:00Z");

            assertFalse(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.RESOLVED));
        }

        @Test
        @DisplayName("Retorna false quando SLA vencido mas status IGNORED")
        void returnsFalseWhenOverdueButIgnored() {
            Instant now = Instant.parse("2026-02-10T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-05T10:00:00Z");

            assertFalse(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.IGNORED));
        }

        @Test
        @DisplayName("Retorna false quando SLA não vencido")
        void returnsFalseWhenNotOverdue() {
            Instant now = Instant.parse("2026-02-01T10:00:00Z");
            OffsetDateTime slaDueAt = OffsetDateTime.parse("2026-02-10T10:00:00Z");

            assertFalse(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.OPEN));
        }

        @Test
        @DisplayName("Retorna false para slaDueAt null")
        void returnsFalseForNullSlaDueAt() {
            Instant now = Instant.now();
            assertFalse(slaService.isOverdue(now, null, GovernanceIssueStatus.OPEN));
        }

        @Test
        @DisplayName("Retorna false para now null")
        void returnsFalseForNullNow() {
            OffsetDateTime slaDueAt = OffsetDateTime.now().minusDays(1);
            assertFalse(slaService.isOverdue((Instant) null, slaDueAt, GovernanceIssueStatus.OPEN));
        }

        @Test
        @DisplayName("Retorna false para status null")
        void returnsFalseForNullStatus() {
            Instant now = Instant.now();
            OffsetDateTime slaDueAt = OffsetDateTime.now().minusDays(1);
            assertFalse(slaService.isOverdue(now, slaDueAt, null));
        }
    }

    @Nested
    @DisplayName("calculateReopenedSlaDueAt()")
    class CalculateReopenedSlaDueAtTests {

        @Test
        @DisplayName("Recalcula SLA usando now como base")
        void recalculatesUsingNowAsBase() {
            OffsetDateTime before = OffsetDateTime.now();
            OffsetDateTime result = slaService.calculateReopenedSlaDueAt(GovernanceSeverity.ERROR);
            OffsetDateTime after = OffsetDateTime.now();

            // Deve estar entre before+3 e after+3
            assertTrue(result.isAfter(before.plusDays(3).minusSeconds(1)));
            assertTrue(result.isBefore(after.plusDays(3).plusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("getDaysUntilDue()")
    class GetDaysUntilDueTests {

        @Test
        @DisplayName("Retorna dias positivos para SLA futuro")
        void returnsPositiveForFutureDue() {
            OffsetDateTime slaDueAt = OffsetDateTime.now().plusDays(5);
            long days = slaService.getDaysUntilDue(slaDueAt);

            assertTrue(days >= 4 && days <= 5);
        }

        @Test
        @DisplayName("Retorna dias negativos para SLA vencido")
        void returnsNegativeForPastDue() {
            OffsetDateTime slaDueAt = OffsetDateTime.now().minusDays(5);
            long days = slaService.getDaysUntilDue(slaDueAt);

            assertTrue(days >= -6 && days <= -4);
        }

        @Test
        @DisplayName("Retorna MAX_VALUE para slaDueAt null")
        void returnsMaxValueForNull() {
            assertEquals(Long.MAX_VALUE, slaService.getDaysUntilDue(null));
        }
    }

    @Nested
    @DisplayName("Timezone handling")
    class TimezoneTests {

        @Test
        @DisplayName("Cálculo funciona com diferentes offsets")
        void worksWithDifferentOffsets() {
            // São Paulo (UTC-3)
            OffsetDateTime saoPaulo = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(-3));
            // UTC
            OffsetDateTime utc = OffsetDateTime.of(2026, 2, 1, 13, 0, 0, 0, ZoneOffset.UTC);

            // Ambos representam o mesmo instante
            assertEquals(saoPaulo.toInstant(), utc.toInstant());

            // SLA calculado deve ser equivalente
            OffsetDateTime dueSaoPaulo = slaService.calculateDueAt(saoPaulo, GovernanceSeverity.ERROR);
            OffsetDateTime dueUtc = slaService.calculateDueAt(utc, GovernanceSeverity.ERROR);

            assertEquals(dueSaoPaulo.toInstant(), dueUtc.toInstant());
        }

        @Test
        @DisplayName("isOverdue funciona corretamente com OffsetDateTime")
        void isOverdueWorksWithOffsetDateTime() {
            OffsetDateTime now = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.ofHours(-3));
            OffsetDateTime slaDueAt = OffsetDateTime.of(2026, 2, 5, 10, 0, 0, 0, ZoneOffset.UTC);

            assertTrue(slaService.isOverdue(now, slaDueAt, GovernanceIssueStatus.OPEN));
        }
    }
}
