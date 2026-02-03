package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Serviço de cálculo de SLA para issues de governança.
 *
 * <p>Regras de SLA por severidade (Sprint 5):
 * <ul>
 *   <li>ERROR (crítico): 3 dias</li>
 *   <li>WARN (médio): 15 dias</li>
 *   <li>INFO (baixo): 30 dias</li>
 * </ul>
 *
 * <p>Decisões de design:
 * <ul>
 *   <li>Ao criar issue: slaDueAt = createdAt + dias_por_severidade</li>
 *   <li>Ao alterar severidade: recalcula slaDueAt mantendo createdAt como base</li>
 *   <li>Ao resolver: mantém slaDueAt para análise histórica</li>
 *   <li>Ao reabrir (RESOLVED → OPEN): recalcula SLA usando now() como base,
 *       pois representa novo prazo para resolução (abordagem mais realista)</li>
 * </ul>
 *
 * <p>Timezone: Todos os cálculos usam America/Sao_Paulo internamente,
 * mas armazenam como UTC (OffsetDateTime/Instant).
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
@Service
public class GovernanceSlaService {

    /**
     * Timezone padrão para cálculos de SLA.
     */
    public static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    /**
     * Dias de SLA por severidade.
     */
    private static final int SLA_DAYS_ERROR = 3;
    private static final int SLA_DAYS_WARN = 15;
    private static final int SLA_DAYS_INFO = 30;

    /**
     * Calcula a data de vencimento do SLA baseada na severidade.
     *
     * @param baseDate  data base para cálculo (criação ou reabertura)
     * @param severity  severidade da issue
     * @return data de vencimento do SLA
     */
    public OffsetDateTime calculateDueAt(OffsetDateTime baseDate, GovernanceSeverity severity) {
        if (baseDate == null || severity == null) {
            return null;
        }

        int slaDays = getSlaDays(severity);
        return baseDate.plusDays(slaDays);
    }

    /**
     * Calcula a data de vencimento do SLA usando Instant.
     *
     * @param baseInstant  instante base para cálculo
     * @param severity     severidade da issue
     * @return instante de vencimento do SLA
     */
    public Instant calculateDueAt(Instant baseInstant, GovernanceSeverity severity) {
        if (baseInstant == null || severity == null) {
            return null;
        }

        int slaDays = getSlaDays(severity);
        return baseInstant.plus(slaDays, ChronoUnit.DAYS);
    }

    /**
     * Retorna os dias de SLA para uma severidade.
     *
     * @param severity severidade
     * @return quantidade de dias de SLA
     */
    public int getSlaDays(GovernanceSeverity severity) {
        if (severity == null) {
            return SLA_DAYS_WARN; // default
        }

        return switch (severity) {
            case ERROR -> SLA_DAYS_ERROR;
            case WARN -> SLA_DAYS_WARN;
            case INFO -> SLA_DAYS_INFO;
        };
    }

    /**
     * Verifica se uma issue está vencida (overdue).
     *
     * <p>Uma issue é considerada vencida quando:
     * <ul>
     *   <li>slaDueAt não é null</li>
     *   <li>slaDueAt < now</li>
     *   <li>status não é RESOLVED nem IGNORED</li>
     * </ul>
     *
     * @param now       instante atual
     * @param slaDueAt  data de vencimento do SLA
     * @param status    status atual da issue
     * @return true se vencida
     */
    public boolean isOverdue(Instant now, OffsetDateTime slaDueAt, GovernanceIssueStatus status) {
        if (slaDueAt == null || now == null || status == null) {
            return false;
        }

        // Issues resolvidas ou ignoradas não são consideradas vencidas
        if (status == GovernanceIssueStatus.RESOLVED || status == GovernanceIssueStatus.IGNORED) {
            return false;
        }

        return now.isAfter(slaDueAt.toInstant());
    }

    /**
     * Verifica se uma issue está vencida usando OffsetDateTime.
     *
     * @param now       data/hora atual
     * @param slaDueAt  data de vencimento do SLA
     * @param status    status atual da issue
     * @return true se vencida
     */
    public boolean isOverdue(OffsetDateTime now, OffsetDateTime slaDueAt, GovernanceIssueStatus status) {
        if (now == null) {
            return false;
        }
        return isOverdue(now.toInstant(), slaDueAt, status);
    }

    /**
     * Calcula o SLA para reabertura de issue.
     *
     * <p>Quando uma issue é reaberta (RESOLVED → OPEN), recalculamos o SLA
     * usando a data atual como base. Isso representa um novo prazo realista
     * para resolução, ao invés de manter o prazo original que já passou.
     *
     * @param severity severidade da issue
     * @return nova data de vencimento do SLA
     */
    public OffsetDateTime calculateReopenedSlaDueAt(GovernanceSeverity severity) {
        return calculateDueAt(OffsetDateTime.now(), severity);
    }

    /**
     * Calcula dias restantes até o vencimento do SLA.
     *
     * @param slaDueAt data de vencimento
     * @return dias restantes (negativo se vencido)
     */
    public long getDaysUntilDue(OffsetDateTime slaDueAt) {
        if (slaDueAt == null) {
            return Long.MAX_VALUE;
        }

        Instant now = Instant.now();
        return ChronoUnit.DAYS.between(now, slaDueAt.toInstant());
    }
}
