package br.com.consisa.gov.kb.domain;

/**
 * Status de uma issue de governança.
 *
 * REGRA DE NEGÓCIO (Sprint 1):
 * - OPEN = issue detectada, aguardando análise
 * - ASSIGNED = issue atribuída a responsável
 * - IN_PROGRESS = analista assumiu a issue, em tratamento
 * - RESOLVED = issue resolvida/fechada
 * - IGNORED = issue ignorada/descartada
 *
 * IMPORTANTE:
 * - "Issue aberta" = OPEN, ASSIGNED ou IN_PROGRESS
 * - Quando um analista "pega" uma issue, ela continua sendo problema aberto
 * - Só deixa de ser problema quando status = RESOLVED
 */
public enum GovernanceIssueStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    IGNORED
}
